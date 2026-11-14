package com.mealorder.pay.controller;

import com.mealorder.auth.annotation.RequireRole;
import com.mealorder.common.result.R;
import com.mealorder.pay.dto.PayDtos.ReconcileResultVO;
import com.mealorder.pay.job.PayReconcileJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端支付运维（M5）：对账手动触发入口
 */
@Tag(name = "支付运维")
@RestController
@RequestMapping("/admin/pay")
public class AdminPayController {

    private final PayReconcileJob payReconcileJob;

    public AdminPayController(PayReconcileJob payReconcileJob) {
        this.payReconcileJob = payReconcileJob;
    }

    @Operation(summary = "手动触发 pay_log ↔ orders 对账，返回不一致明细")
    @RequireRole({"ADMIN"})
    @PostMapping("/reconcile-scan")
    public R<ReconcileResultVO> reconcileScan() {
        var mismatches = payReconcileJob.scanOnce();
        return R.ok(new ReconcileResultVO(mismatches.size(), mismatches, payReconcileJob.intervalMs()));
    }
}