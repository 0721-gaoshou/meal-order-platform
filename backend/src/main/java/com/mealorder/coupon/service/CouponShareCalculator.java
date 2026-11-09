package com.mealorder.coupon.service;

import com.mealorder.coupon.entity.Coupon;

/**
 * 优惠券金额分摊（计划书 §4.8 逐项累计法）
 * 前 n-1 项按"含当前项的累计金额占比"取整后减去已分摊，尾差全归最后一项，保证 Σ分摊 == 总优惠
 */
public final class CouponShareCalculator {

    private CouponShareCalculator() {
    }

    /** 券面优惠额·分：满减券直接取 discountAmount；折扣券按 rate 折算实付后取差额 */
    public static long discountOf(Coupon coupon, long totalAmount) {
        if (CouponService.TYPE_DISCOUNT.equals(coupon.getType())) {
            long pay = Math.round(totalAmount * coupon.getDiscountRate() / 100.0);
            return totalAmount - pay;
        }
        return coupon.getDiscountAmount();
    }

    /**
     * @param discount 总优惠·分
     * @param amounts  各商品行金额·分（顺序即分摊输出顺序）
     * @return 每行分摊到的优惠·分，最后一项吸收尾差
     */
    public static long[] allocate(long discount, long[] amounts) {
        long[] shares = new long[amounts.length];
        long total = 0;
        for (long amount : amounts) {
            total += amount;
        }
        if (discount <= 0 || total <= 0 || amounts.length == 0) {
            return shares;
        }
        long allocated = 0;
        long cumulative = 0;
        for (int i = 0; i < amounts.length - 1; i++) {
            cumulative += amounts[i];
            shares[i] = Math.round((double) discount * cumulative / total) - allocated;
            allocated += shares[i];
        }
        shares[amounts.length - 1] = discount - allocated;
        return shares;
    }
}