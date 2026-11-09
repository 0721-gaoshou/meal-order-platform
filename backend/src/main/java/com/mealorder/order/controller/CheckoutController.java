package com.mealorder.order.controller;

import com.mealorder.common.result.R;
import com.mealorder.common.util.UserContext;
import com.mealorder.order.dto.OrderDtos.CheckoutRequest;
import com.mealorder.order.dto.OrderDtos.PreviewVO;
import com.mealorder.order.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 结算台：进入结算页时调预览，试算金额并领取下单令牌
 */
@Tag(name = "结算")
@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @Operation(summary = "结算预览（试算金额与券分摊 + 发下单令牌，30 分钟有效）")
    @PostMapping("/preview")
    public R<PreviewVO> preview(@Valid @RequestBody CheckoutRequest request) {
        return R.ok(checkoutService.preview(UserContext.get().userId(),
                request.skuIds(), request.userCouponId()));
    }
}