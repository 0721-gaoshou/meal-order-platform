package com.mealorder.goods.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.ResultCode;
import com.mealorder.goods.dto.GoodsDtos.BatchStatusRequest;
import com.mealorder.goods.dto.GoodsDtos.SkuItem;
import com.mealorder.goods.dto.GoodsDtos.SkuVO;
import com.mealorder.goods.dto.GoodsDtos.SpuQuery;
import com.mealorder.goods.dto.GoodsDtos.SpuUpsertRequest;
import com.mealorder.goods.dto.GoodsDtos.SpuVO;
import com.mealorder.goods.entity.Category;
import com.mealorder.goods.entity.Sku;
import com.mealorder.goods.entity.Spu;
import com.mealorder.goods.mapper.CategoryMapper;
import com.mealorder.goods.mapper.SkuMapper;
import com.mealorder.goods.mapper.SpuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品 SPU 管理：分页查询 + 新增/编辑（含 SKU 全量替换）+ 上下架 + 逻辑删除
 */
@Service
public class SpuService {

    public static final String STATUS_ON_SALE = "ON_SALE";
    public static final String STATUS_OFF_SALE = "OFF_SALE";

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;

    public SpuService(SpuMapper spuMapper, SkuMapper skuMapper, CategoryMapper categoryMapper) {
        this.spuMapper = spuMapper;
        this.skuMapper = skuMapper;
        this.categoryMapper = categoryMapper;
    }

    public Page<SpuVO> page(SpuQuery query) {
        Page<Spu> page = spuMapper.selectPage(
                new Page<>(query.page(), query.size()),
                Wrappers.<Spu>lambdaQuery()
                        .like(query.keyword() != null && !query.keyword().isBlank(), Spu::getName, query.keyword())
                        .eq(query.categoryId() != null, Spu::getCategoryId, query.categoryId())
                        .eq(query.status() != null && !query.status().isBlank(), Spu::getStatus, query.status())
                        .orderByDesc(Spu::getCreatedAt));
        return toVOPage(page);
    }

    /** 商品详情（C 端只允许看在售商品） */
    public SpuVO detail(Long id) {
        Spu spu = requireById(id);
        if (!STATUS_ON_SALE.equals(spu.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品已下架");
        }
        List<Sku> skus = skuMapper.selectList(Wrappers.<Sku>lambdaQuery().eq(Sku::getSpuId, id));
        Category category = categoryMapper.selectById(spu.getCategoryId());
        return toVO(spu, skus, category == null ? null : category.getName());
    }

    @Transactional
    public Long create(SpuUpsertRequest request) {
        Spu spu = new Spu();
        apply(spu, request);
        spu.setStatus(STATUS_OFF_SALE); // 新品默认下架，上架走专门操作
        spuMapper.insert(spu);
        insertSkus(spu.getId(), request.skus());
        return spu.getId();
    }

    @Transactional
    public void update(Long id, SpuUpsertRequest request) {
        Spu spu = requireById(id);
        apply(spu, request);
        spuMapper.updateById(spu);
        // SKU 全量替换：老 SKU 逻辑删除 + 新列表插入（M1 简化，库存对账在 M2 下单链路再考虑）
        skuMapper.delete(Wrappers.<Sku>lambdaQuery().eq(Sku::getSpuId, id));
        insertSkus(id, request.skus());
    }

    /** 单个/批量上下架共用 */
    @Transactional
    public void batchChangeStatus(BatchStatusRequest request) {
        String target = Boolean.TRUE.equals(request.onSale()) ? STATUS_ON_SALE : STATUS_OFF_SALE;
        List<Spu> list = spuMapper.selectByIds(request.ids());
        list.forEach(spu -> {
            spu.setStatus(target);
            spuMapper.updateById(spu);
        });
    }

    @Transactional
    public void delete(Long id) {
        requireById(id);
        spuMapper.deleteById(id);
        skuMapper.delete(Wrappers.<Sku>lambdaQuery().eq(Sku::getSpuId, id));
    }

    // ---------------- 内部方法 ----------------

    private void apply(Spu spu, SpuUpsertRequest request) {
        spu.setName(request.name());
        spu.setCategoryId(request.categoryId());
        spu.setMainImage(request.mainImage());
        spu.setTags(request.tags());
        spu.setDescription(request.description());
    }

    private void insertSkus(Long spuId, List<SkuItem> skus) {
        for (SkuItem item : skus) {
            Sku sku = new Sku();
            sku.setSpuId(spuId);
            sku.setSpec(item.spec());
            sku.setPrice(item.price());
            sku.setStock(item.stock());
            sku.setVersion(0);
            skuMapper.insert(sku);
        }
    }

    private Page<SpuVO> toVOPage(Page<Spu> page) {
        List<Spu> records = page.getRecords();
        Page<SpuVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (records.isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }
        Map<Long, String> categoryNames = categoryMapper.selectByIds(
                        records.stream().map(Spu::getCategoryId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<Long, List<Sku>> skuMap = skuMapper.selectList(
                        Wrappers.<Sku>lambdaQuery().in(Sku::getSpuId,
                                records.stream().map(Spu::getId).toList()))
                .stream()
                .collect(Collectors.groupingBy(Sku::getSpuId));
        voPage.setRecords(records.stream()
                .map(spu -> toVO(spu, skuMap.get(spu.getId()), categoryNames.get(spu.getCategoryId())))
                .toList());
        return voPage;
    }

    private SpuVO toVO(Spu spu, List<Sku> skus, String categoryName) {
        List<SkuVO> skuVOs = skus == null ? Collections.emptyList() : skus.stream()
                .map(sku -> new SkuVO(sku.getId(), sku.getSpec(), sku.getPrice(), sku.getStock()))
                .toList();
        return new SpuVO(spu.getId(), spu.getName(), spu.getCategoryId(), categoryName,
                spu.getMainImage(), spu.getTags(), spu.getDescription(), spu.getStatus(),
                spu.getCreatedAt(), skuVOs);
    }

    private Spu requireById(Long id) {
        Spu spu = spuMapper.selectById(id);
        if (spu == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        return spu;
    }
}
