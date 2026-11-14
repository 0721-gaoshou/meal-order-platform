package com.mealorder.behavior.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为埋点，对应表 user_behavior（AI 推荐预留）
 */
@Data
@TableName("user_behavior")
public class UserBehavior {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** 商品 ID -> spu.id；订单/支付/领券等无商品场景为 NULL */
    private Long itemId;

    /** ADD_CART/ORDER/PAY/COUPON_RECEIVE（EXPOSED/CLICK 留待前端埋点） */
    private String action;

    /** 场景: cart/checkout/pay/coupon */
    private String scene;

    private LocalDateTime createdAt;
}