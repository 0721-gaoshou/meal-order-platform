package com.mealorder.cart.controller;

import com.mealorder.cart.dto.CartDtos.AddCartRequest;
import com.mealorder.cart.dto.CartDtos.CartVO;
import com.mealorder.cart.dto.CartDtos.UpdateQuantityRequest;
import com.mealorder.cart.service.CartService;
import com.mealorder.common.result.R;
import com.mealorder.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户购物车：登录即可，无角色限制（userId 取自 JWT，不传参防越权）
 */
@Tag(name = "购物车")
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "购物车列表（含失效条目标记与 valid 合计）")
    @GetMapping
    public R<CartVO> list() {
        return R.ok(cartService.list(UserContext.get().userId()));
    }

    @Operation(summary = "加购（同 SKU 数量累加，超库存回滚报错）")
    @PostMapping("/items")
    public R<Void> add(@Valid @RequestBody AddCartRequest request) {
        cartService.add(UserContext.get().userId(), request.skuId(), request.quantity());
        return R.ok();
    }

    @Operation(summary = "改数量（覆盖式）")
    @PutMapping("/items/{skuId}")
    public R<Void> updateQuantity(@PathVariable Long skuId, @Valid @RequestBody UpdateQuantityRequest request) {
        cartService.updateQuantity(UserContext.get().userId(), skuId, request.quantity());
        return R.ok();
    }

    @Operation(summary = "删除条目（幂等，不存在也返回成功）")
    @DeleteMapping("/items/{skuId}")
    public R<Void> remove(@PathVariable Long skuId) {
        cartService.remove(UserContext.get().userId(), skuId);
        return R.ok();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public R<Void> clear() {
        cartService.clear(UserContext.get().userId());
        return R.ok();
    }
}