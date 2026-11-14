package com.mealorder.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealorder.pay.dto.PayDtos.ReconcileMismatch;
import com.mealorder.pay.entity.PayLog;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PayLogMapper extends BaseMapper<PayLog> {

    /** 对账方向1：支付单已 PAID 但订单不在已支付族状态（回调丢失/被误取消） */
    @Select("SELECT o.id AS orderId, o.order_no AS orderNo, 'PAY_PAID_ORDER_NOT_PAID' AS type, o.status AS orderStatus "
            + "FROM pay_log p JOIN orders o ON o.id = p.order_id "
            + "WHERE p.status = 'PAID' AND o.status NOT IN ('PAID','MAKING','READY','COMPLETED','REFUNDING','REFUNDED')")
    List<ReconcileMismatch> selectPaidLogOrderNotPaid();
}