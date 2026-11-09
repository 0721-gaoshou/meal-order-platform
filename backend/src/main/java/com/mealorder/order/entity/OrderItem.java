package com.mealorder.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单明细，对应表 order_item；名称/规格/单价均为下单时快照，防商品改价污染历史单
 */
@Data
@TableName("order_item")
public class OrderItem {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private Long spuId;

    private Long skuId;

    private String spuName;

    private String skuSpec;

    private String skuImage;

    private Integer quantity;

    /** 成交单价·分（快照） */
    private Long unitPrice;

    /** 本行分摊优惠·分（M3 §4.8 分摊算法写入） */
    private Long couponShareAmount;

    /** NONE/PARTIAL/REFUNDING/REFUNDED */
    private String refundStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}