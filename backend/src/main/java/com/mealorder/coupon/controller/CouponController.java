package com.mealorder.coupon.controller;

import com.mealorder.common.result.R;
import com.mealorder.common.util.UserContext;
import com.mealorder.coupon.dto.CouponDtos.CenterCouponVO;
import com.mealorder.coupon.dto.CouponDtos.UserCouponVO;
import com.mealorder.coupon.service.CouponService;
import com.mealorder.coupon.service.UserCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户券中心：可领列表、领券、我的券（登录即可，userId 取自 JWT）
 */
@Tag(name = "优惠券")
@RestController
@RequestMapping("/coupons")
public class CouponController {

    private final CouponService couponService;
    private final UserCouponService userCouponService;

    public CouponController(CouponService couponService, UserCouponService userCouponService) {
        this.couponService = couponService;
        this.userCouponService = userCouponService;
    }

    @Operation(summary = "券中心：有效期内且有余量的券，标记当前用户是否已领")
    @GetMapping
    public R<List<CenterCouponVO>> receivable() {
        return R.ok(couponService.receivable(UserContext.get().userId()));
    }

    @Operation(summary = "领券（三步防超发：资格校验 → 余量条件扣减 → 唯一索引兜底）")
    @PostMapping("/{id}/receive")
    public R<UserCouponVO> receive(@PathVariable Long id) {
        return R.ok(userCouponService.receive(UserContext.get().userId(), id));
    }

    @Operation(summary = "我的券（过期为展示层推导）")
    @GetMapping("/mine")
    public R<List<UserCouponVO>> mine() {
        return R.ok(userCouponService.mine(UserContext.get().userId()));
    }
}