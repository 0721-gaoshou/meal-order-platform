package com.mealorder.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类，对应表 category（deleted 走全局逻辑删除配置）
 */
@Data
@TableName("category")
public class Category {

    /** 雪花 ID 序列化为字符串，避免 JS 端精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String icon;

    /** 越小越靠前，拖拽排序改这里 */
    private Integer sort;

    /** 1 启用 0 停用 */
    private Integer status;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
