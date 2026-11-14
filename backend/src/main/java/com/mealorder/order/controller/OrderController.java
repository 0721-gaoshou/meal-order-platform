package com.mealorder.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealorder.common.result.R;
import com.mealorder.common.util.UserContext;
import com.mealorder.order.dto.OrderDtos.CreateOrderRequest;
import com.mealorder.order.dto.OrderDtos.OrderQuery;
import com.mealorder.order.dto.OrderDtos.OrderVO;
import com.mealorder.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户订单：登录即可，数据按 userId 隔离（userId 取自 JWT，不传参防越权）
 */
@Tag(name = "订单")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "下单（令牌防重 + requestId 幂等 + DB 条件扣库存）")
    @PostMapping
    public R<OrderVO> create(@Valid @RequestBody CreateOrderRequest request) {
        return R.ok(orderService.create(UserContext.get().userId(), request));
    }

    @Operation(summary = "取消待支付订单（回补库存 + 未过期券返还）")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancel(UserContext.get().userId(), id);
        return R.ok();
    }

    @Operation(summary = "我的订单分页（status 可选筛选）")
    @GetMapping("/page")
    public R<Page<OrderVO>> page(@ModelAttribute OrderQuery query) {
        return R.ok(orderService.page(UserContext.get().userId(), query));
    }

    @Operation(summary = "订单详情（仅本人可见）")
    @GetMapping("/{id}")
    public R<OrderVO> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(UserContext.get().userId(), id));
    }
}