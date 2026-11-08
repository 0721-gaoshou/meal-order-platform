package com.mealorder.goods.controller;

import com.mealorder.auth.annotation.RequireRole;
import com.mealorder.common.result.R;
import com.mealorder.goods.dto.GoodsDtos.CategorySortRequest;
import com.mealorder.goods.dto.GoodsDtos.CategoryUpsertRequest;
import com.mealorder.goods.dto.GoodsDtos.CategoryVO;
import com.mealorder.goods.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类管理：查询所有登录用户可用（商品表单要选分类）；写操作仅 ADMIN
 */
@Tag(name = "分类管理")
@RestController
@RequestMapping("/admin/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表（按 sort 升序）")
    @GetMapping
    public R<List<CategoryVO>> list() {
        return R.ok(categoryService.list());
    }

    @Operation(summary = "新建分类")
    @RequireRole("ADMIN")
    @PostMapping
    public R<Void> create(@Valid @RequestBody CategoryUpsertRequest request) {
        categoryService.create(request);
        return R.ok();
    }

    @Operation(summary = "编辑分类")
    @RequireRole("ADMIN")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryUpsertRequest request) {
        categoryService.update(id, request);
        return R.ok();
    }

    @Operation(summary = "删除分类（逻辑删除）")
    @RequireRole("ADMIN")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }

    @Operation(summary = "拖拽排序（ids 按新顺序整体提交）")
    @RequireRole("ADMIN")
    @PutMapping("/sort")
    public R<Void> sort(@Valid @RequestBody CategorySortRequest request) {
        categoryService.sort(request);
        return R.ok();
    }
}
