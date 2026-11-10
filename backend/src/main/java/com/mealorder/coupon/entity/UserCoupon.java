package com.mealorder.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户券，对应表 user_coupon；(user_id, coupon_id) 唯一索引是领券幂等锚点（计划书 §4.11）
 */
@Data
@TableName("user_coupon")
public class UserCoupon {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long couponId;

    /** UNUSED/USED/EXPIRED；展示层的"已过期"由券模板 endTime 推导，不依赖定时刷状态 */
    private String status;

    private Long usedOrderId;

    private LocalDateTime usedTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}