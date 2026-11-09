package com.mealorder.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单主表，对应表 orders；状态机见计划书 §4.2（M2 只写 PENDING_PAY）
 */
@Data
@TableName("orders")
public class Order {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long userId;

    /** PENDING_PAY/PAID/MAKING/READY/COMPLETED/CLOSED/REFUNDING/REFUNDED */
    private String status;

    /** 商品总额·分 */
    private Long totalAmount;

    /** 优惠总额·分（M3 优惠券启用） */
    private Long discountAmount;

    /** 实付·分 = total - discount */
    private Long payAmount;

    private Long couponId;

    private String remark;

    /** 下单幂等键（前端 UUID），uk_request_id 兜底 */
    private String requestId;

    private LocalDateTime expireTime;

    private LocalDateTime payTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}