package com.mealorder.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealorder.cart.service.CartService;
import com.mealorder.cart.service.CartService.CheckedItem;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.ResultCode;
import com.mealorder.coupon.entity.Coupon;
import com.mealorder.coupon.mapper.UserCouponMapper;
import com.mealorder.coupon.service.CouponShareCalculator;
import com.mealorder.coupon.service.UserCouponService;
import com.mealorder.goods.mapper.SkuMapper;
import com.mealorder.order.dto.OrderDtos.CreateOrderRequest;
import com.mealorder.order.dto.OrderDtos.OrderItemVO;
import com.mealorder.order.dto.OrderDtos.OrderQuery;
import com.mealorder.order.dto.OrderDtos.OrderVO;
import com.mealorder.order.entity.Order;
import com.mealorder.order.entity.OrderItem;
import com.mealorder.order.mapper.OrderItemMapper;
import com.mealorder.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 下单链路（计划书 §4.6/§4.10）：三层防重——前端防抖(M3页面) / Redis 令牌 DEL / uk_request_id 兜底
 * 防超卖：事务内逐行 DB 条件更新，任一不足整体回滚
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final CartService cartService;
    private final StringRedisTemplate redis;
    private final UserCouponService userCouponService;
    private final UserCouponMapper userCouponMapper;
    private final OrderCancelService orderCancelService;
    private final com.mealorder.mq.OrderDelayProducer orderDelayProducer;
    private final com.mealorder.behavior.service.BehaviorTracker behaviorTracker;

    @Value("${order.expire-minutes}")
    private long expireMinutes;

    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(Long userId, CreateOrderRequest request) {
        // 幂等回放放在令牌检查之前：同 requestId 重试直接返回原单，不再消耗令牌
        Order existing = findByRequestId(request.requestId());
        if (existing != null) {
            if (!existing.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "requestId 冲突");
            }
            return toVO(existing, itemsOf(existing.getId()));
        }

        Boolean firstUse = redis.delete(CheckoutService.tokenKey(userId, request.token()));
        if (!Boolean.TRUE.equals(firstUse)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "下单令牌已失效，请刷新结算页重试");
        }

        List<CheckedItem> items = cartService.requireCartItems(userId, request.skuIds());
        long[] amounts = items.stream().mapToLong(i -> i.sku().getPrice() * i.quantity()).toArray();
        for (int i = 0; i < items.size(); i++) {
            if (skuMapper.deductStock(items.get(i).sku().getId(), items.get(i).quantity()) == 0) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR,
                        "「" + items.get(i).spu().getName() + "」库存不足，请调整数量");
            }
        }

        long totalAmount = 0;
        for (long amount : amounts) {
            totalAmount += amount;
        }
        long discountAmount = 0;
        long[] shares = null;
        if (request.userCouponId() != null) {
            Coupon coupon = userCouponService.requireUsable(userId, request.userCouponId(), totalAmount);
            discountAmount = CouponShareCalculator.discountOf(coupon, totalAmount);
            shares = CouponShareCalculator.allocate(discountAmount, amounts);
        }

        // 时间戳显式赋值：MP insert 不回读 DB 默认值，创建响应要带 createdAt
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setOrderNo("MO" + IdWorker.getIdStr());
        order.setUserId(userId);
        order.setStatus("PENDING_PAY");
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(totalAmount - discountAmount);
        order.setCouponId(request.userCouponId());
        order.setRemark(request.remark());
        order.setRequestId(request.requestId());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setExpireTime(now.plusMinutes(expireMinutes));
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发同 requestId 穿透到唯一索引：回滚本次扣减，回放胜出的原单
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            Order winner = findByRequestId(request.requestId());
            if (winner == null) {
                throw e;
            }
            return toVO(winner, itemsOf(winner.getId()));
        }

        // 券核销（§4.7 CAS）：订单已有 ID 才能写 used_order_id；影响行数 0 说明券被别的订单抢用，整体回滚
        if (request.userCouponId() != null
                && userCouponMapper.markUsed(request.userCouponId(), userId, order.getId(), now) == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "优惠券已被使用，请重新选择");
        }

        for (int i = 0; i < items.size(); i++) {
            CheckedItem item = items.get(i);
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setSpuId(item.spu().getId());
            oi.setSkuId(item.sku().getId());
            oi.setSpuName(item.spu().getName());
            oi.setSkuSpec(item.sku().getSpec());
            oi.setSkuImage(item.spu().getMainImage());
            oi.setQuantity(item.quantity());
            oi.setUnitPrice(item.sku().getPrice());
            oi.setCouponShareAmount(shares == null ? 0L : shares[i]);
            oi.setRefundStatus("NONE");
            orderItemMapper.insert(oi);
        }

        // Redis 不与 DB 同事务：提交后再清购物车；清理失败只影响体验，订单以 DB 为准
        String cartKey = CartService.cartKey(userId);
        Object[] fields = request.skuIds().stream().map(Object::toString).toArray();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    redis.opsForHash().delete(cartKey, fields);
                } catch (Exception ex) {
                    log.warn("订单已提交但购物车清理失败: orderNo={}", order.getOrderNo(), ex);
                }
                // M5 延迟取消主通道：提交后才投递，避免消息先于订单行可见；失败由扫表兜底接管
                try {
                    orderDelayProducer.sendCancelDelay(order.getId(), order.getExpireTime());
                } catch (Exception ex) {
                    log.warn("订单已提交但延迟取消消息投递失败，兜底扫表接管: orderNo={}", order.getOrderNo(), ex);
                }
                behaviorTracker.record(order.getUserId(), null, "ORDER", "checkout");
            }
        });
        return toVO(order, itemsOf(order.getId()));
    }

    /**
     * 取消待支付订单（用户主动）：校验归属与状态后走统一取消出口
     * （超时自动取消复用同一出口 OrderCancelService）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!"PENDING_PAY".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "只能取消待支付订单");
        }
        if (!orderCancelService.cancel(order)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单状态已变化，请刷新后重试");
        }
    }

    public Page<OrderVO> page(Long userId, OrderQuery query) {
        Page<Order> page = orderMapper.selectPage(new Page<>(query.page(), query.size()),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(query.status() != null && !query.status().isBlank(), Order::getStatus, query.status())
                        .orderByDesc(Order::getCreatedAt));
        return toVOPage(page);
    }

    /** 管理端订单分页：全量（M4 状态流转演示；多商家隔离后续按 merchant_id 加条件） */
    public Page<OrderVO> adminPage(OrderQuery query) {
        Page<Order> page = orderMapper.selectPage(new Page<>(query.page(), query.size()),
                new LambdaQueryWrapper<Order>()
                        .eq(query.status() != null && !query.status().isBlank(), Order::getStatus, query.status())
                        .orderByDesc(Order::getCreatedAt));
        return toVOPage(page);
    }

    private static final Map<String, String> NEXT_STATUS = Map.of(
            "PAID", "MAKING", "MAKING", "READY", "READY", "COMPLETED");

    /** 商家/管理员推进订单状态：只允许 PAID→MAKING→READY→COMPLETED，跳态拒绝 */
    public void transitionStatus(Long id, String to) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        String expected = NEXT_STATUS.get(order.getStatus());
        if (expected == null || !expected.equals(to)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "不允许的状态流转：" + order.getStatus() + " → " + to);
        }
        if (orderMapper.transit(id, order.getStatus(), to) == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单状态已变化，请刷新后重试");
        }
    }

    private Page<OrderVO> toVOPage(Page<Order> page) {
        List<Long> orderIds = page.getRecords().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemMap = orderIds.isEmpty() ? Map.of()
                : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))
                        .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        Page<OrderVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(o -> toVO(o, itemMap.getOrDefault(o.getId(), List.of()))).toList());
        return result;
    }

    public OrderVO detail(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return toVO(order, itemsOf(id));
    }

    private Order findByRequestId(String requestId) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getRequestId, requestId));
    }

    private List<OrderItem> itemsOf(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId).orderByAsc(OrderItem::getId));
    }

    private OrderVO toVO(Order order, List<OrderItem> items) {
        return new OrderVO(order.getId(), order.getOrderNo(), order.getStatus(),
                order.getTotalAmount(), order.getDiscountAmount(), order.getPayAmount(), order.getCouponId(),
                order.getRemark(),
                order.getExpireTime(), order.getPayTime(), order.getCreatedAt(),
                items.stream().map(i -> new OrderItemVO(i.getSkuId(), i.getSpuId(), i.getSpuName(),
                        i.getSkuSpec(), i.getSkuImage(), i.getQuantity(), i.getUnitPrice(),
                        i.getCouponShareAmount(), i.getRefundStatus())).toList());
    }
}