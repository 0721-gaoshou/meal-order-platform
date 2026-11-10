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
}