package com.mealorder.goods.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealorder.common.result.R;
import com.mealorder.goods.dto.GoodsDtos.CategoryVO;
import com.mealorder.goods.dto.GoodsDtos.SpuQuery;
import com.mealorder.goods.dto.GoodsDtos.SpuVO;
import com.mealorder.goods.service.CategoryService;
import com.mealorder.goods.service.SpuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端公开商品接口（白名单 /public/**，免登录浏览；下单/购物车仍需登录）
 * 与 /admin/** 的区别：只暴露在售商品与启用分类，不含管理字段
 */
@Tag(name = "C端商品")
@RestController
@RequestMapping("/public/goods")
public class PublicGoodsController {

    private final SpuService spuService;
    private final CategoryService categoryService;

    public PublicGoodsController(SpuService spuService, CategoryService categoryService) {
        this.spuService = spuService;
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表（启用中，按排序值）")
    @GetMapping("/categories")
    public R<List<CategoryVO>> categories() {
        return R.ok(categoryService.list());
    }

    @Operation(summary = "在售商品分页（keyword 模糊 / categoryId 筛选）")
    @GetMapping("/spus")
    public R<Page<SpuVO>> spus(@ModelAttribute SpuQuery query) {
        return R.ok(spuService.page(new SpuQuery(query.keyword(), query.categoryId(),
                "ON_SALE", query.pageNum(), query.pageSize())));
    }

    @Operation(summary = "商品详情（含全部规格）")
    @GetMapping("/spus/{id}")
    public R<SpuVO> detail(@PathVariable Long id) {
        return R.ok(spuService.detail(id));
    }
}