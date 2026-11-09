package com.mealorder.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealorder.coupon.entity.Coupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 防超发核心（计划书 §4.7 第②步）：条件扣减余量，影响行数 0 = 已被领完
     * remain 为 UNSIGNED，WHERE remain>0 保证不会减成负数
     */
    @Update("UPDATE coupon SET remain = remain - 1 WHERE id = #{couponId} AND remain > 0")
    int decrementRemain(@Param("couponId") Long couponId);
}