package com.mealorder.pay.job;

import com.mealorder.order.mapper.OrderMapper;
import com.mealorder.pay.dto.PayDtos.ReconcileMismatch;
import com.mealorder.pay.mapper.PayLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * M5 对账任务：pay_log ↔ orders 双向状态对账，只报告不自动改数（修复走人工运维）
 * 方向1 支付单 PAID 订单未支付族；方向2 订单已支付族无 PAID 支付单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayReconcileJob {

    private final PayLogMapper payLogMapper;
    private final OrderMapper orderMapper;

    @Value("${pay.reconcile-ms:60000}")
    private long reconcileMs;

    @Scheduled(fixedDelayString = "${pay.reconcile-ms:60000}")
    public void scheduledScan() {
        List<ReconcileMismatch> mismatches = scanOnce();
        if (!mismatches.isEmpty()) {
            log.warn("支付对账发现不一致: {} 笔", mismatches.size());
        }
    }

    /** 可手动触发（管理端运维入口） */
    public List<ReconcileMismatch> scanOnce() {
        List<ReconcileMismatch> mismatches = new ArrayList<>(payLogMapper.selectPaidLogOrderNotPaid());
        mismatches.addAll(orderMapper.selectPaidOrderNoPayLog());
        for (ReconcileMismatch mismatch : mismatches) {
            log.warn("支付对账不一致: orderId={}, orderNo={}, type={}, orderStatus={}",
                    mismatch.orderId(), mismatch.orderNo(), mismatch.type(), mismatch.orderStatus());
        }
        return mismatches;
    }

    public long intervalMs() {
        return reconcileMs;
    }
}