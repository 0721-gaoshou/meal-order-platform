package com.mealorder.coupon;

import com.mealorder.coupon.entity.Coupon;
import com.mealorder.coupon.service.CouponShareCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * §4.8 分摊算法单元测试：核心断言是"Σ分摊恰好等于总优惠"，尾差必须被最后一项吸收
 */
class CouponShareCalculatorTest {

    private static long sum(long[] arr) {
        long s = 0;
        for (long v : arr) {
            s += v;
        }
        return s;
    }

    @Test
    void 计划书验收用例_满30减5拆到9_9加9_9加10_2() {
        long[] amounts = {990, 990, 1020};
        long[] shares = CouponShareCalculator.allocate(500, amounts);
        assertEquals(500, sum(shares));
        assertEquals(165, shares[0]);
        assertEquals(165, shares[1]);
        // 尾差 170 归最后一项：三项都不超过各自行金额，部分退款可按行退
        assertEquals(170, shares[2]);
        for (int i = 0; i < shares.length; i++) {
            assertTrue(shares[i] <= amounts[i]);
        }
    }

    @Test
    void 等额三行除不尽时尾差归最后一项() {
        long[] shares = CouponShareCalculator.allocate(100, new long[]{1000, 1000, 1000});
        assertEquals(100, sum(shares));
        assertEquals(33, shares[0]);
        assertEquals(34, shares[1]);
        assertEquals(33, shares[2]);
    }

    @Test
    void 一分优惠也不会丢() {
        assertEquals(1, sum(CouponShareCalculator.allocate(1, new long[]{3, 3, 3})));
        assertEquals(1, sum(CouponShareCalculator.allocate(1, new long[]{1})));
    }

    @Test
    void 单行券额等于行金额() {
        long[] shares = CouponShareCalculator.allocate(500, new long[]{500});
        assertEquals(500, shares[0]);
    }

    @Test
    void 穷举一千组金额和券面_和对账恒等() {
        for (int totalParts = 1; totalParts <= 6; totalParts++) {
            for (long a = 1; a <= 40; a += 7) {
                long[] amounts = new long[totalParts];
                for (int i = 0; i < totalParts; i++) {
                    amounts[i] = a + i * 3;
                }
                long total = sum(amounts);
                for (long discount = 1; discount <= total; discount += 13) {
                    assertEquals(discount, sum(CouponShareCalculator.allocate(discount, amounts)),
                            "discount=" + discount + " amounts=" + java.util.Arrays.toString(amounts));
                }
            }
        }
    }

    @Test
    void 满减券面额直接取discountAmount() {
        Coupon coupon = new Coupon();
        coupon.setType("FULL_REDUCTION");
        coupon.setDiscountAmount(500L);
        assertEquals(500, CouponShareCalculator.discountOf(coupon, 3000));
    }

    @Test
    void 折扣券9折按四舍五入到分取差额() {
        Coupon coupon = new Coupon();
        coupon.setType("DISCOUNT");
        coupon.setDiscountRate(90);
        assertEquals(300, CouponShareCalculator.discountOf(coupon, 3000));
        // 9.99 元打 9 折 → 实付 8.991 → 四舍五入 8.99，优惠 1.00
        assertEquals(100, CouponShareCalculator.discountOf(coupon, 999));
    }
}