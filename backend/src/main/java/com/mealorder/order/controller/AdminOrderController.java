package com.mealorder.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealorder.auth.annotation.RequireRole;
import com.mealorder.common.result.R;
import com.mealorder.order.dto.OrderDtos.OrderQuery;
import com.mealorder.order.dto.OrderDtos.OrderStatusRequest;
import com.mealorder.order.dto.OrderDtos.OrderVO;
import com.mealorder.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端订单（M4）：列表 + 出餐状态流转
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "订单分页（status 可选筛选，全量）")
    @GetMapping("/page")
    public R<Page<OrderVO>> page(@ModelAttribute OrderQuery query) {
        return R.ok(orderService.adminPage(query));
    }

    @Operation(summary = "推进一步状态（PAID→MAKING→READY→COMPLETED，跳态拒绝）")
    @RequireRole({"ADMIN", "MERCHANT"})
    @PutMapping("/{id}/status")
    public R<Void> transition(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        orderService.transitionStatus(id, request.status());
        return R.ok();
    }
}