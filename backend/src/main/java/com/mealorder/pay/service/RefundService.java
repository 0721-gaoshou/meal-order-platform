package com.mealorder.pay.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.ResultCode;
import com.mealorder.order.entity.Order;
import com.mealorder.order.entity.OrderItem;
import com.mealorder.order.mapper.OrderItemMapper;
import com.mealorder.order.mapper.OrderMapper;
import com.mealorder.pay.dto.PayDtos.RefundVO;
import com.mealorder.pay.entity.RefundOrder;
import com.mealorder.pay.mapper.RefundOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款（M4）：整单全额退。申请走状态 CAS（已支付/制作中/待取餐 → 退款中），
 * Mock 退款回调幂等（refund_no 唯一 + 状态 CAS），成功后订单与明细都置退款完成
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final List<String> REFUNDABLE = List.of("PAID", "MAKING", "READY");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RefundOrderMapper refundOrderMapper;

    @Transactional(rollbackFor = Exception.class)
    public RefundVO apply(Long userId, Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        // 先判"是否有退款在处理"，再判订单状态，保证重复申请得到准确提示
        Long processing = refundOrderMapper.selectCount(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getOrderId, orderId).eq(RefundOrder::getStatus, "REFUNDING"));
        if (processing != null && processing > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该订单已有退款处理中");
        }
        if (!REFUNDABLE.contains(order.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "当前订单状态不可申请退款");
        }
        if (orderMapper.markRefunding(orderId) == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单状态已变化，请刷新后重试");
        }
        LocalDateTime now = LocalDateTime.now();
        RefundOrder refund = new RefundOrder();
        refund.setRefundNo("RF" + IdWorker.getIdStr());
        refund.setOrderId(orderId);
        refund.setRefundAmount(order.getPayAmount());
        refund.setStatus("REFUNDING");
        refund.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        refund.setCreatedAt(now);
        refund.setUpdatedAt(now);
        refundOrderMapper.insert(refund);
        return new RefundVO(orderId, refund.getRefundNo(), refund.getRefundAmount(), refund.getStatus(), "REFUNDING");
    }

    @Transactional(rollbackFor = Exception.class)
    public RefundVO mockCallback(String refundNo) {
        RefundOrder refund = refundOrderMapper.selectOne(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getRefundNo, refundNo));
        if (refund == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款单不存在");
        }
        if ("REFUNDED".equals(refund.getStatus())) {
            return new RefundVO(refund.getOrderId(), refund.getRefundNo(), refund.getRefundAmount(),
                    refund.getStatus(), currentOrderStatus(refund.getOrderId()));
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = refundOrderMapper.update(null, Wrappers.<RefundOrder>lambdaUpdate()
                .eq(RefundOrder::getId, refund.getId())
                .eq(RefundOrder::getStatus, "REFUNDING")
                .set(RefundOrder::getStatus, "REFUNDED")
                .set(RefundOrder::getRefundTime, now)
                .set(RefundOrder::getUpdatedAt, now));
        if (rows == 0) {
            RefundOrder latest = refundOrderMapper.selectById(refund.getId());
            if (latest != null && "REFUNDED".equals(latest.getStatus())) {
                return new RefundVO(latest.getOrderId(), latest.getRefundNo(), latest.getRefundAmount(),
                        latest.getStatus(), currentOrderStatus(latest.getOrderId()));
            }
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "退款单状态异常");
        }
        // 订单 REFUNDING → REFUNDED（并发回调时另一笔可能已完成，允许幂等通过）
        if (orderMapper.transit(refund.getOrderId(), "REFUNDING", "REFUNDED") == 0
                && !"REFUNDED".equals(currentOrderStatus(refund.getOrderId()))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单状态不是退款中，无法完成退款");
        }
        orderItemMapper.update(null, Wrappers.<OrderItem>lambdaUpdate()
                .eq(OrderItem::getOrderId, refund.getOrderId())
                .set(OrderItem::getRefundStatus, "REFUNDED"));
        return new RefundVO(refund.getOrderId(), refund.getRefundNo(), refund.getRefundAmount(),
                "REFUNDED", "REFUNDED");
    }

    private String currentOrderStatus(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        return order == null ? null : order.getStatus();
    }
}