package com.mealorder.pay.controller;

import com.mealorder.common.result.R;
import com.mealorder.common.util.UserContext;
import com.mealorder.pay.dto.PayDtos.MockCallbackRequest;
import com.mealorder.pay.dto.PayDtos.PayCreateVO;
import com.mealorder.pay.dto.PayDtos.PayResultVO;
import com.mealorder.pay.service.PayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付：发起（需登录）+ Mock 渠道回调（白名单，模拟网关）
 * 正式微信支付时把 mock-callback 换成微信回调验签，其余链路不变（M9）
 */
@Tag(name = "支付")
@RestController
@RequestMapping("/pay")
public class PayController {

    private final PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
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
}