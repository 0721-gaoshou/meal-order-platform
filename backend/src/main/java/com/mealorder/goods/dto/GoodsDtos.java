package com.mealorder.goods.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品模块 DTO 集中定义（无行为数据体不拆文件）
 */
public final class GoodsDtos {

    private GoodsDtos() {
    }

    // ---------------- 分类 ----------------

    public record CategoryUpsertRequest(
            @NotBlank(message = "分类名不能为空") @Size(max = 64, message = "分类名最长 64 字") String name,
            String icon,
            Integer sort,
            Integer status) {
    }

    /** 拖拽排序结果：ids 按新顺序排列，后端按下标重写 sort */
    public record CategorySortRequest(@NotEmpty List<Long> ids) {
    }

    public record CategoryVO(
            @JsonSerialize(using = ToStringSerializer.class) Long id,
            String name,
            String icon,
            Integer sort,
            Integer status,
            LocalDateTime createdAt) {
    }

    // ---------------- SPU / SKU ----------------

    public record SkuItem(
            Long id,
            @NotBlank(message = "规格名不能为空") @Size(max = 128) String spec,
            @NotNull(message = "价格不能为空") @Min(value = 1, message = "价格必须大于 0 分") Long price,
            @NotNull(message = "库存不能为空") @Min(value = 0, message = "库存不能为负") Integer stock) {
    }

    public record SpuUpsertRequest(
            @NotBlank(message = "商品名不能为空") @Size(max = 128, message = "商品名最长 128 字") String name,
            @NotNull(message = "分类不能为空") Long categoryId,
            String mainImage,
            String tags,
            String description,
            @NotEmpty(message = "至少需要一个 SKU 规格") @Valid List<SkuItem> skus) {
    }

    /** 商品列表查询参数（GET 分页） */
    public record SpuQuery(
            String keyword,
            Long categoryId,
            String status,
            Integer pageNum,
            Integer pageSize) {

        public int page() {
            return pageNum == null || pageNum < 1 ? 1 : pageNum;
        }

        public int size() {
            return pageSize == null || pageSize < 1 || pageSize > 100 ? 10 : pageSize;
        }
    }

    public record SkuVO(@JsonSerialize(using = ToStringSerializer.class) Long id, String spec, Long price, Integer stock) {
    }

    public record SpuVO(
            @JsonSerialize(using = ToStringSerializer.class) Long id,
            String name,
            @JsonSerialize(using = ToStringSerializer.class) Long categoryId,
            String categoryName,
            String mainImage,
            String tags,
            String description,
            String status,
            LocalDateTime createdAt,
            List<SkuVO> skus) {
    }

    /** 批量上下架请求 */
    public record BatchStatusRequest(
            @NotEmpty(message = "请选择商品") List<Long> ids,
            @NotNull(message = "缺少目标状态") Boolean onSale) {
    }
}
