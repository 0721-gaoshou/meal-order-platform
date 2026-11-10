package com.mealorder.order.service;

import com.mealorder.coupon.entity.Coupon;
import com.mealorder.coupon.entity.UserCoupon;
import com.mealorder.coupon.mapper.CouponMapper;
import com.mealorder.coupon.mapper.UserCouponMapper;
import com.mealorder.goods.mapper.SkuMapper;
import com.mealorder.order.entity.Order;
import com.mealorder.order.entity.OrderItem;
import com.mealorder.order.mapper.OrderItemMapper;
import com.mealorder.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 取消订单的单一出口：用户主动取消与超时自动取消共用
 * 动作 = 状态 CAS(PENDING_PAY→CLOSED) + 回补库存 + 未过期券返还（§4.7）
 */
@Service
@RequiredArgsConstructor
public class OrderCancelService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponMapper couponMapper;

    /** @return false 表示状态已被并发改动（非待支付），本次不处理 */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Order order) {
        if (orderMapper.markClosed(order.getId()) == 0) {
            return false;
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            skuMapper.restoreStock(item.getSkuId(), item.getQuantity());
        }
        if (order.getCouponId() != null) {
            UserCoupon userCoupon = userCouponMapper.selectById(order.getCouponId());
            Coupon coupon = userCoupon == null ? null : couponMapper.selectById(userCoupon.getCouponId());
            if (userCoupon != null && coupon != null
                    && !LocalDateTime.now().isAfter(coupon.getEndTime())) {
                userCouponMapper.restoreToUnused(userCoupon.getId(), order.getUserId(), order.getId());
            }
        }
        return true;
    }
}