package com.mealorder.order.service;

import com.mealorder.cart.service.CartService;
import com.mealorder.cart.service.CartService.CheckedItem;
import com.mealorder.order.dto.OrderDtos.PreviewItemVO;
import com.mealorder.order.dto.OrderDtos.PreviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 结算预览：试算金额 + 发下单令牌（计划书 §4.10 第二层）
 * 不做库存硬校验——加购后库存可能被别人买走，最终由下单时 DB 条件更新兜底（§4.6）
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;
    private final CartService cartService;

    public PreviewVO preview(Long userId, List<Long> skuIds) {
        List<CheckedItem> items = cartService.requireCartItems(userId, skuIds);
        List<PreviewItemVO> itemVOs = items.stream()
                .map(i -> new PreviewItemVO(i.sku().getId(), i.spu().getName(), i.sku().getSpec(),
                        i.spu().getMainImage(), i.sku().getPrice(), i.quantity(),
                        i.sku().getPrice() * i.quantity()))
                .toList();
        long totalAmount = itemVOs.stream().mapToLong(PreviewItemVO::subtotal).sum();

        // SETNX 令牌：key 含 userId 防盗用，下单时 DEL 成功才放行；TTL 兜底防垃圾堆积
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().setIfAbsent(tokenKey(userId, token), "1", TOKEN_TTL);
        return new PreviewVO(itemVOs, totalAmount, token);
    }

    public static String tokenKey(Long userId, String token) {
        return "order:token:" + userId + ":" + token;
    }
}