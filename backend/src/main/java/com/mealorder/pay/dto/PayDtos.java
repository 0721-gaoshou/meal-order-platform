package com.mealorder.pay.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotBlank;

/**
 * 支付模块 DTO。M4 先用 Mock 网关：发起支付拿商户单号，模拟回调驱动状态机
 */
public final class PayDtos {

    private PayDtos() {
    }

    public record PayCreateVO(
            @JsonSerialize(using = ToStringSerializer.class) Long orderId,
            String orderNo,
            String outTradeNo,
            Long amount,
            String payChannel) {
    }

    /** 模拟渠道回调：tradeNo 为渠道单号（Mock 下由前端生成），重复回调必须幂等 */
    public record MockCallbackRequest(
            @NotBlank(message = "缺少商户单号") String outTradeNo,
            @NotBlank(message = "缺少渠道单号") String tradeNo) {
    }

    public record PayResultVO(
            @JsonSerialize(using = ToStringSerializer.class) Long orderId,
            String orderStatus,
            String outTradeNo,
            String tradeNo,
            Long amount) {
    }

    public record RefundApplyRequest(
            @jakarta.validation.constraints.Size(max = 255, message = "退款原因最长 255 字") String reason) {
    }

    public record RefundVO(
            @JsonSerialize(using = ToStringSerializer.class) Long orderId,
            String refundNo,
            Long refundAmount,
            String status,
            String orderStatus) {
    }

    /** Mock 退款回调：refundNo 幂等锚点 */
    public record MockRefundCallbackRequest(
            @NotBlank(message = "缺少退款单号") String refundNo) {
    }

    /** 对账不一致明细：type = PAY_PAID_ORDER_NOT_PAID / ORDER_PAID_NO_PAY_LOG */
    public record ReconcileMismatch(
            @JsonSerialize(using = ToStringSerializer.class) Long orderId,
            String orderNo,
            String type,
            String orderStatus) {
    }

    /** 对账结果：只报告不自动改数；intervalMs 回显配置值便于证明被读到 */
    public record ReconcileResultVO(int count, java.util.List<ReconcileMismatch> mismatches, long intervalMs) {
    }
}