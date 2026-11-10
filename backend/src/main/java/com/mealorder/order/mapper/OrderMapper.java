package com.mealorder.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealorder.order.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface OrderMapper extends BaseMapper<Order> {

    /** 状态机 CAS：待支付 → 已支付（并发重复回调/重复支付只会有一次生效） */
    @Update("UPDATE orders SET status = 'PAID', pay_time = #{payTime} WHERE id = #{id} AND status = 'PENDING_PAY'")
    int markPaid(@Param("id") Long id, @Param("payTime") LocalDateTime payTime);

    /** 状态机 CAS：待支付 → 已取消 */
    @Update("UPDATE orders SET status = 'CLOSED' WHERE id = #{id} AND status = 'PENDING_PAY'")
    int markClosed(@Param("id") Long id);

    /** 通用状态流转（前后态都校验，防跳态） */
    @Update("UPDATE orders SET status = #{to} WHERE id = #{id} AND status = #{from}")
    int transit(@Param("id") Long id, @Param("from") String from, @Param("to") String to);

    /** 申请退款：已支付/制作中/待取餐 → 退款中 */
    @Update("UPDATE orders SET status = 'REFUNDING' WHERE id = #{id} AND status IN ('PAID','MAKING','READY')")
    int markRefunding(@Param("id") Long id);
}