package com.mealorder.order.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 下单链路 DTO 集中定义（与 GoodsDtos 同风格）
 */
public final class OrderDtos {

    private OrderDtos() {
    }

    /** 结算预览/下单共用的选品：只传 skuId，数量以服务端购物车为准（防篡改）；userCouponId 可选 */
    public record CheckoutRequest(
            @NotEmpty(message = "请选择商品") @Size(max = 50, message = "单次最多结算 50 种商品") List<Long> skuIds,
            Long userCouponId) {
    }

    public record PreviewItemVO(
            @JsonSerialize(using = ToStringSerializer.class) Long skuId,
            String spuName,
            String skuSpec,
            String skuImage,
            Long unitPrice,
            Integer quantity,
            Long subtotal,
            /** 本行分摊到的优惠·分（§4.8，未选券为 0） */
            Long couponShareAmount) {
    }

    /** token：下单令牌，提交时 DEL 成功才放行（§4.10 第二层）；payAmount = totalAmount - discountAmount */
    public record PreviewVO(List<PreviewItemVO> items, Long totalAmount, Long discountAmount,
                            Long payAmount, String couponName, String token) {
    }

    public record CreateOrderRequest(
            @NotBlank(message = "缺少 requestId") @Size(max = 64) String requestId,
            @NotBlank(message = "缺少下单令牌") String token,
            @NotEmpty(message = "请选择商品") @Size(max = 50, message = "单次最多结算 50 种商品") List<Long> skuIds,
            @Size(max = 255, message = "备注最长 255 字") String remark,
            /** 使用的用户券ID（user_coupon.id），可为空 */
            Long userCouponId) {
    }

    public record OrderItemVO(
            @JsonSerialize(using = ToStringSerializer.class) Long skuId,
            @JsonSerialize(using = ToStringSerializer.class) Long spuId,
            String spuName,
            String skuSpec,
            String skuImage,
            Integer quantity,
            Long unitPrice,
            Long couponShareAmount,
            String refundStatus) {
    }

    public record OrderVO(
            @JsonSerialize(using = ToStringSerializer.class) Long id,
            String orderNo,
            String status,
            Long totalAmount,
            Long discountAmount,
            Long payAmount,
            @JsonSerialize(using = ToStringSerializer.class) Long couponId,
            String remark,
            LocalDateTime expireTime,
            LocalDateTime payTime,
            LocalDateTime createdAt,
            List<OrderItemVO> items) {
    }

    /** 我的订单分页查询 */
    public record OrderQuery(String status, Integer pageNum, Integer pageSize) {

        public int page() {
            return pageNum == null || pageNum < 1 ? 1 : pageNum;
        }

        public int size() {
            return pageSize == null || pageSize < 1 || pageSize > 100 ? 10 : pageSize;
        }
    }

    /** 管理端订单状态流转：只允许 PAID→MAKING→READY→COMPLETED */
    public record OrderStatusRequest(@NotBlank(message = "缺少目标状态") String status) {
    }
}