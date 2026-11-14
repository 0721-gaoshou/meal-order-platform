package com.mealorder.cart.service;

import com.mealorder.cart.dto.CartDtos.CartItemVO;
import com.mealorder.cart.dto.CartDtos.CartVO;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.ResultCode;
import com.mealorder.goods.entity.Sku;
import com.mealorder.goods.entity.Spu;
import com.mealorder.goods.mapper.SkuMapper;
import com.mealorder.goods.mapper.SpuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 服务端购物车：Redis Hash，key=cart:{userId}，field=skuId，value=quantity
 * 只存数量不存价格——列表时回源 DB 取现价，下单时再快照（计划书 §5.2）
 * 库存校验是体验层，防超卖最终靠下单时的 DB 条件更新（§4.6）
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private static final String KEY_PREFIX = "cart:";

    private final StringRedisTemplate redis;
    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;
    private final com.mealorder.behavior.service.BehaviorTracker behaviorTracker;

    public void add(Long userId, Long skuId, Integer quantity) {
        Sku sku = requireOnSaleSku(skuId);
        String key = cartKey(userId);
        String field = skuId.toString();
        // HINCRBY 原子累加；越过库存则回补，避免并发加购把总量推过库存
        Long newQuantity = redis.opsForHash().increment(key, field, quantity);
        if (newQuantity > sku.getStock()) {
            redis.opsForHash().increment(key, field, -quantity);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "库存不足，当前仅剩 " + sku.getStock() + " 件");
        }
        behaviorTracker.record(userId, sku.getSpuId(), "ADD_CART", "cart");
    }

    public void updateQuantity(Long userId, Long skuId, Integer quantity) {
        Sku sku = requireOnSaleSku(skuId);
        if (quantity > sku.getStock()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "库存不足，当前仅剩 " + sku.getStock() + " 件");
        }
        redis.opsForHash().put(cartKey(userId), skuId.toString(), quantity.toString());
    }

    public void remove(Long userId, Long skuId) {
        redis.opsForHash().delete(cartKey(userId), skuId.toString());
    }

    public void clear(Long userId) {
        redis.delete(cartKey(userId));
    }

    /** 结算/下单共用：按 skuIds 从购物车取数量并校验在售，数量以服务端为准（防客户端篡改） */
    public List<CheckedItem> requireCartItems(Long userId, List<Long> skuIds) {
        List<Object> fields = skuIds.stream().map(id -> (Object) id.toString()).toList();
        List<Object> values = redis.opsForHash().multiGet(cartKey(userId), fields);
        Map<Long, Sku> skuMap = skuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        Set<Long> spuIds = skuMap.values().stream().map(Sku::getSpuId).collect(Collectors.toSet());
        Map<Long, Spu> spuMap = spuIds.isEmpty() ? Map.of()
                : spuMapper.selectBatchIds(spuIds).stream()
                        .collect(Collectors.toMap(Spu::getId, Function.identity()));

        List<CheckedItem> items = new ArrayList<>(skuIds.size());
        for (int i = 0; i < skuIds.size(); i++) {
            Long skuId = skuIds.get(i);
            Object quantity = values.get(i);
            if (quantity == null) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "购物车条目已变化，请刷新购物车");
            }
            Sku sku = skuMap.get(skuId);
            if (sku == null) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "商品已失效，请刷新购物车");
            }
            Spu spu = spuMap.get(sku.getSpuId());
            if (spu == null || !"ON_SALE".equals(spu.getStatus())) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "「" + (spu == null ? skuId : spu.getName()) + "」已下架");
            }
            items.add(new CheckedItem(sku, spu, Integer.valueOf(quantity.toString())));
        }
        return items;
    }

    /** 结算级校验通过的购物车条目：数量来自 Redis，价格/名称来自 DB 现值 */
    public record CheckedItem(Sku sku, Spu spu, Integer quantity) {
    }

    public static String cartKey(Long userId) {
        return KEY_PREFIX + userId;
    }

    public CartVO list(Long userId) {
        Map<Object, Object> entries = redis.opsForHash().entries(cartKey(userId));
        if (entries.isEmpty()) {
            return new CartVO(List.of(), 0, 0L);
        }
        Map<Long, Integer> quantities = entries.entrySet().stream()
                .collect(Collectors.toMap(e -> Long.valueOf(e.getKey().toString()),
                        e -> Integer.valueOf(e.getValue().toString())));

        Map<Long, Sku> skuMap = skuMapper.selectBatchIds(quantities.keySet()).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        Set<Long> spuIds = skuMap.values().stream().map(Sku::getSpuId).collect(Collectors.toSet());
        Map<Long, Spu> spuMap = spuIds.isEmpty() ? Map.of()
                : spuMapper.selectBatchIds(spuIds).stream()
                        .collect(Collectors.toMap(Spu::getId, Function.identity()));

        List<CartItemVO> items = quantities.entrySet().stream()
                // 雪花 ID 按时间有序，倒序 ≈ 最近加购在前
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .map(e -> toItem(e.getKey(), e.getValue(), skuMap.get(e.getKey()), spuMap))
                .toList();

        int totalQuantity = items.stream().filter(CartItemVO::valid).mapToInt(CartItemVO::quantity).sum();
        long totalAmount = items.stream().filter(CartItemVO::valid)
                .mapToLong(i -> i.price() * i.quantity()).sum();
        return new CartVO(items, totalQuantity, totalAmount);
    }

    private CartItemVO toItem(Long skuId, Integer quantity, Sku sku, Map<Long, Spu> spuMap) {
        if (sku == null) {
            return new CartItemVO(skuId, null, "商品已失效", null, null, 0L, quantity, 0, false);
        }
        Spu spu = spuMap.get(sku.getSpuId());
        boolean valid = spu != null && "ON_SALE".equals(spu.getStatus());
        return new CartItemVO(skuId, sku.getSpuId(),
                spu == null ? "商品已失效" : spu.getName(),
                sku.getSpec(), spu == null ? null : spu.getMainImage(),
                sku.getPrice(), quantity, sku.getStock(), valid);
    }

    private Sku requireOnSaleSku(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品规格不存在");
        }
        Spu spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null || !"ON_SALE".equals(spu.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "商品已下架");
        }
        return sku;
    }
}