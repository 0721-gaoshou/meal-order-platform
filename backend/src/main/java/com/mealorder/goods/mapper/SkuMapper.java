package com.mealorder.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealorder.goods.entity.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface SkuMapper extends BaseMapper<Sku> {

    /**
     * 防超卖核心（计划书 §4.6 MVP 定案）：条件更新原子扣减，影响行数 0 = 库存不足
     * 自定义 SQL 不走 MP 逻辑删除拦截，deleted=0 手动带上；updated_at 由 DDL ON UPDATE 自动维护
     */
    @Update("UPDATE sku SET stock = stock - #{quantity} WHERE id = #{skuId} AND stock >= #{quantity} AND deleted = 0")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /** 取消订单回补库存 */
    @Update("UPDATE sku SET stock = stock + #{quantity} WHERE id = #{skuId}")
    int restoreStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
