package com.mealorder.goods.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealorder.auth.annotation.RequireRole;
import com.mealorder.common.result.R;
import com.mealorder.goods.dto.GoodsDtos.BatchStatusRequest;
import com.mealorder.goods.dto.GoodsDtos.SpuQuery;
import com.mealorder.goods.dto.GoodsDtos.SpuUpsertRequest;
import com.mealorder.goods.dto.GoodsDtos.SpuVO;
import com.mealorder.goods.service.SpuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品 SPU 管理：管理员与商家均可操作（多商家数据隔离 M1 不启用）
 */
@Tag(name = "商品管理")
@RestController
@RequestMapping("/admin/spus")
public class SpuController {

    private final SpuService spuService;

    public SpuController(SpuService spuService) {
        this.spuService = spuService;
    }

    @Operation(summary = "商品分页列表（keyword 模糊/categoryId/status 筛选）")
    @GetMapping("/page")
    public R<Page<SpuVO>> page(@ModelAttribute SpuQuery query) {
        return R.ok(spuService.page(query));
    }

    @Operation(summary = "新建商品（默认下架，SKU 随表单提交）")
    @RequireRole({"ADMIN", "MERCHANT"})
    @PostMapping
    public R<String> create(@Valid @RequestBody SpuUpsertRequest request) {
        // 雪花 ID 以字符串返回，前端 JS Number 会丢精度
        return R.ok(String.valueOf(spuService.create(request)));
    }

    @Operation(summary = "编辑商品（SKU 全量替换）")
    @RequireRole({"ADMIN", "MERCHANT"})
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SpuUpsertRequest request) {
        spuService.update(id, request);
        return R.ok();
    }

    @Operation(summary = "批量上下架")
    @RequireRole({"ADMIN", "MERCHANT"})
    @PutMapping("/status")
    public R<Void> batchChangeStatus(@Valid @RequestBody BatchStatusRequest request) {
        spuService.batchChangeStatus(request);
        return R.ok();
    }

    @Operation(summary = "删除商品（逻辑删除，级联 SKU）")
    @RequireRole({"ADMIN", "MERCHANT"})
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        spuService.delete(id);
        return R.ok();
    }
}
