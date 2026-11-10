package com.mealorder.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealorder.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 核销（计划书 §4.7）：状态 CAS，只有 UNUSED 才能被本次下单占用
     * 影响行数 0 = 券已被其他订单核销/状态不符，调用方抛异常整体回滚
     */
    @Update("UPDATE user_coupon SET status = 'USED', used_order_id = #{orderId}, used_time = #{usedTime} "
            + "WHERE id = #{id} AND user_id = #{userId} AND status = 'UNUSED'")
    int markUsed(@Param("id") Long id, @Param("userId") Long userId,
                 @Param("orderId") Long orderId, @Param("usedTime") LocalDateTime usedTime);
}