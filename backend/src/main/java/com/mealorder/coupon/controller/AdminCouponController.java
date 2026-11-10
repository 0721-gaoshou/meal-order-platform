package com.mealorder.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealorder.auth.annotation.RequireRole;
import com.mealorder.common.result.R;
import com.mealorder.coupon.dto.CouponDtos.CouponQuery;
import com.mealorder.coupon.dto.CouponDtos.CouponUpsertRequest;
import com.mealorder.coupon.dto.CouponDtos.CouponVO;
import com.mealorder.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 券模板管理（计划书 §3.1 管理端：创建满减/折扣券、限量、生效时间）
 */
@Tag(name = "优惠券管理")
@RestController
@RequestMapping("/admin/coupons")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Operation(summary = "券分页列表（keyword/type/status 筛选，status 按时间区间推导）")
    @GetMapping("/page")
    public R<Page<CouponVO>> page(@ModelAttribute CouponQuery query) {
        return R.ok(couponService.page(query));
    }

    @Operation(summary = "创建券（余量=发行总量；满减券填金额，折扣券填折扣率）")
    @RequireRole({"ADMIN", "MERCHANT"})
    @PostMapping
    public R<String> create(@Valid @RequestBody CouponUpsertRequest request) {
        return R.ok(String.valueOf(couponService.create(request)));
    }

    @Operation(summary = "编辑券（发行总量不得小于已领取数，余量按已发数重算）")
    @RequireRole({"ADMIN", "MERCHANT"})
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CouponUpsertRequest request) {
        couponService.update(id, request);
        return R.ok();
    }
}