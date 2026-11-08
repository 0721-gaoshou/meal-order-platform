package com.mealorder.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品 SPU，对应表 spu
 */
@Data
@TableName("spu")
public class Spu {

    /** 雪花 ID 序列化为字符串，避免 JS 端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private Long categoryId;

    /** 商家ID，多商家隔离预留，M1 不启用 */
    private Long merchantId;

    private String mainImage;

    /** 结构化标签，AI 推荐预留（逗号分隔） */
    private String tags;

    private String description;

    /** ON_SALE / OFF_SALE */
    private String status;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
