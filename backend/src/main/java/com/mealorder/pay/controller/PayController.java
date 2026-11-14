package com.mealorder.pay.controller;

import com.mealorder.common.result.R;
import com.mealorder.common.util.UserContext;
import com.mealorder.pay.dto.PayDtos.MockCallbackRequest;
import com.mealorder.pay.dto.PayDtos.MockRefundCallbackRequest;
import com.mealorder.pay.dto.PayDtos.PayCreateVO;
import com.mealorder.pay.dto.PayDtos.PayResultVO;
import com.mealorder.pay.dto.PayDtos.RefundApplyRequest;
import com.mealorder.pay.dto.PayDtos.RefundVO;
import com.mealorder.pay.service.PayService;
import com.mealorder.pay.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付与退款：发起/申请需登录，Mock 渠道回调走白名单（正式微信支付换验签，其余链路不变）
 */
@Tag(name = "支付与退款")
@RestController
@RequestMapping("/pay")
public class PayController {

    private final PayService payService;
    private final RefundService refundService;

    public PayController(PayService payService, RefundService refundService) {
        this.payService = payService;
        this.refundService = refundService;
    }

    @Operation(summary = "发起支付（Mock）：返回商户支付单号，前端模拟渠道回调")
    @PostMapping("/orders/{orderId}")
    public R<PayCreateVO> create(@PathVariable Long orderId) {
        return R.ok(payService.create(UserContext.get().userId(), orderId));
    }

    @Operation(summary = "Mock 渠道回调（幂等）：重复回调返回同一结果")
    @PostMapping("/mock-callback")
    public R<PayResultVO> mockCallback(@Valid @RequestBody MockCallbackRequest request) {
        return R.ok(payService.mockCallback(request));
    }

    @Operation(summary = "申请退款（整单全额退，已支付/制作中/待取餐可申请）")
    @PostMapping("/orders/{orderId}/refund")
    public R<RefundVO> applyRefund(@PathVariable Long orderId,
                                   @RequestBody(required = false) RefundApplyRequest request) {
        return R.ok(refundService.apply(UserContext.get().userId(), orderId,
                request == null ? null : request.reason()));
    }

    @Operation(summary = "Mock 退款回调（幂等）：成功后订单/明细置退款完成")
    @PostMapping("/mock-refund-callback")
    public R<RefundVO> mockRefundCallback(@Valid @RequestBody MockRefundCallbackRequest request) {
        return R.ok(refundService.mockCallback(request.refundNo()));
    }
}