package com.mealorder.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券模板，对应表 coupon
 * 无 status/deleted 字段：券的可用状态一律由 start_time/end_time 推导（M3 决策 1）
 */
@Data
@TableName("coupon")
public class Coupon {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    /** FULL_REDUCTION满减 / DISCOUNT折扣 */
    private String type;

    /** 使用门槛·分，0=无门槛 */
    private Long thresholdAmount;

    /** 优惠金额·分（满减券） */
    private Long discountAmount;

    /** 折扣率，90=9折（折扣券） */
    private Integer discountRate;

    private Integer totalCount;

    private Integer remain;

    /** 每人限领；当前由 uk_user_coupon 硬锁 1 张，字段预留放宽（M3 决策 2） */
    private Integer perUserLimit;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}