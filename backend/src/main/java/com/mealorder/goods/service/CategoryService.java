package com.mealorder.goods.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.ResultCode;
import com.mealorder.goods.dto.GoodsDtos.CategorySortRequest;
import com.mealorder.goods.dto.GoodsDtos.CategoryUpsertRequest;
import com.mealorder.goods.dto.GoodsDtos.CategoryVO;
import com.mealorder.goods.entity.Category;
import com.mealorder.goods.mapper.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类管理：增删改查 + 拖拽排序
 */
@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryVO> list() {
        return categoryMapper.selectList(
                        Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSort))
                .stream()
                .map(c -> new CategoryVO(c.getId(), c.getName(), c.getIcon(), c.getSort(),
                        c.getStatus(), c.getCreatedAt()))
                .toList();
    }

    public void create(CategoryUpsertRequest request) {
        checkNameUnique(request.name(), null);
        Category category = new Category();
        apply(category, request);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        categoryMapper.insert(category);
    }

    public void update(Long id, CategoryUpsertRequest request) {
        Category category = requireById(id);
        checkNameUnique(request.name(), id);
        apply(category, request);
        categoryMapper.updateById(category);
    }

    public void delete(Long id) {
        requireById(id);
        categoryMapper.deleteById(id);
    }

    /**
     * 拖拽排序：前端把新顺序的 id 列表整体提交，按下标重写 sort
     */
    @Transactional
    public void sort(CategorySortRequest request) {
        List<Long> ids = request.ids();
        for (int i = 0; i < ids.size(); i++) {
            Category category = new Category();
            category.setId(ids.get(i));
            category.setSort(i);
            categoryMapper.updateById(category);
        }
    }

    private void apply(Category category, CategoryUpsertRequest request) {
        category.setName(request.name());
        category.setIcon(request.icon());
        if (request.sort() != null) {
            category.setSort(request.sort());
        }
        if (request.status() != null) {
            category.setStatus(request.status());
        }
    }

    private void checkNameUnique(String name, Long excludeId) {
        Long count = categoryMapper.selectCount(
                Wrappers.<Category>lambdaQuery().eq(Category::getName, name).ne(excludeId != null, Category::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "分类名已存在: " + name);
        }
    }

    private Category requireById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }
}
