package com.mealorder.cart.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 购物车模块 DTO 集中定义（与 GoodsDtos 同风格）
 */
public final class CartDtos {

    private CartDtos() {
    }

    /** 加购：数量累加到已有条目上 */
    public record AddCartRequest(
            @NotNull(message = "缺少 skuId") Long skuId,
            @NotNull(message = "缺少数量") @Min(value = 1, message = "数量至少 1 件")
            @Max(value = 999, message = "单次最多加购 999 件") Integer quantity) {
    }

    /** 改数量：覆盖式设置 */
    public record UpdateQuantityRequest(
            @NotNull(message = "缺少数量") @Min(value = 1, message = "数量至少 1 件")
            @Max(value = 999, message = "单条目最多 999 件") Integer quantity) {
    }

    /** valid=false：SKU 已删除或 SPU 已下架，仅可删除，不参与合计与下单 */
    public record CartItemVO(
            @JsonSerialize(using = ToStringSerializer.class) Long skuId,
            @JsonSerialize(using = ToStringSerializer.class) Long spuId,
            String spuName,
            String spec,
            String mainImage,
            Long price,
            Integer quantity,
            Integer stock,
            Boolean valid) {
    }

    /** totalQuantity/totalAmount 只统计 valid 条目；金额单位·分 */
    public record CartVO(List<CartItemVO> items, Integer totalQuantity, Long totalAmount) {
    }
}