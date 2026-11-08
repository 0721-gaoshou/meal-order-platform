package com.mealorder.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品 SKU，对应表 sku
 * version 字段为乐观锁预留（M2 下单扣库存时再启用 OptimisticLockerInnerInterceptor）
 */
@Data
@TableName("sku")
public class Sku {

    /** 雪花 ID 序列化为字符串，避免 JS 端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long spuId;

    private String spec;

    /** 售价·分 */
    private Long price;

    private Integer stock;

    /** 乐观锁版本号（预留） */
    private Integer version;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
