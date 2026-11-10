package com.mealorder.order.service;

import com.mealorder.cart.service.CartService;
import com.mealorder.cart.service.CartService.CheckedItem;
import com.mealorder.coupon.entity.Coupon;
import com.mealorder.coupon.service.CouponShareCalculator;
import com.mealorder.coupon.service.UserCouponService;
import com.mealorder.order.dto.OrderDtos.PreviewItemVO;
import com.mealorder.order.dto.OrderDtos.PreviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 结算预览：试算金额（含券分摊）+ 发下单令牌（计划书 §4.10 第二层）
 * 不做库存硬校验——加购后库存可能被别人买走，最终由下单时 DB 条件更新兜底（§4.6）
 * 选券同理：预览只校验可用性并试算优惠，不锁券，占用由下单事务内 CAS 决定
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;
    private final CartService cartService;
    private final UserCouponService userCouponService;

    public PreviewVO preview(Long userId, List<Long> skuIds, Long userCouponId) {
        List<CheckedItem> items = cartService.requireCartItems(userId, skuIds);
        long[] amounts = items.stream().mapToLong(i -> i.sku().getPrice() * i.quantity()).toArray();
        long totalAmount = 0;
        for (long amount : amounts) {
            totalAmount += amount;
        }

        long discountAmount = 0;
        String couponName = null;
        long[] shares = null;
        if (userCouponId != null) {
            Coupon coupon = userCouponService.requireUsable(userId, userCouponId, totalAmount);
            discountAmount = CouponShareCalculator.discountOf(coupon, totalAmount);
            shares = CouponShareCalculator.allocate(discountAmount, amounts);
            couponName = coupon.getName();
        }

        List<PreviewItemVO> itemVOs = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            CheckedItem item = items.get(i);
            itemVOs.add(new PreviewItemVO(item.sku().getId(), item.spu().getName(), item.sku().getSpec(),
                    item.spu().getMainImage(), item.sku().getPrice(), item.quantity(),
                    amounts[i], shares == null ? 0L : shares[i]));
        }

        // SETNX 令牌：key 含 userId 防盗用，下单时 DEL 成功才放行；TTL 兜底防垃圾堆积
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().setIfAbsent(tokenKey(userId, token), "1", TOKEN_TTL);
        return new PreviewVO(itemVOs, totalAmount, discountAmount, totalAmount - discountAmount, couponName, token);
    }

    public static String tokenKey(Long userId, String token) {
        return "order:token:" + userId + ":" + token;
    }
}