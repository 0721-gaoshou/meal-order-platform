package com.mealorder.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mealorder.coupon.dto.CouponDtos.CenterCouponVO;
import com.mealorder.coupon.dto.CouponDtos.CouponQuery;
import com.mealorder.coupon.dto.CouponDtos.CouponUpsertRequest;
import com.mealorder.coupon.dto.CouponDtos.CouponVO;
import com.mealorder.coupon.entity.Coupon;
import com.mealorder.coupon.entity.UserCoupon;
import com.mealorder.coupon.mapper.CouponMapper;
import com.mealorder.coupon.mapper.UserCouponMapper;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 券模板：管理端 CRUD + 券中心可领列表
 * 状态不入库，按 start/end 时间区间推导（M3 决策 1）
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    public static final String TYPE_FULL_REDUCTION = "FULL_REDUCTION";
    public static final String TYPE_DISCOUNT = "DISCOUNT";

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    public Long create(CouponUpsertRequest request) {
        validate(request);
        Coupon coupon = new Coupon();
        apply(coupon, request);
        coupon.setRemain(request.totalCount());
        coupon.setPerUserLimit(1);
        // MP insert 不回读 DB 默认时间戳，显式赋值（M2 踩坑沿用）
        LocalDateTime now = LocalDateTime.now();
        coupon.setCreatedAt(now);
        coupon.setUpdatedAt(now);
        couponMapper.insert(coupon);
        return coupon.getId();
    }

    public void update(Long id, CouponUpsertRequest request) {
        validate(request);
        Coupon old = couponMapper.selectById(id);
        if (old == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        int issued = old.getTotalCount() - old.getRemain();
        if (request.totalCount() < issued) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "发行总量不能小于已领取数（已发 " + issued + " 张）");
        }
        apply(old, request);
        old.setTotalCount(request.totalCount());
        old.setRemain(request.totalCount() - issued);
        old.setUpdatedAt(LocalDateTime.now());
        couponMapper.updateById(old);
    }

    public Page<CouponVO> page(CouponQuery query) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<Coupon>()
                .like(hasText(query.keyword()), Coupon::getName, query.keyword())
                .eq(hasText(query.type()), Coupon::getType, query.type())
                .orderByDesc(Coupon::getCreatedAt);
        if ("NOT_STARTED".equals(query.status())) {
            wrapper.gt(Coupon::getStartTime, now);
        } else if ("IN_PROGRESS".equals(query.status())) {
            wrapper.le(Coupon::getStartTime, now).ge(Coupon::getEndTime, now);
        } else if ("ENDED".equals(query.status())) {
            wrapper.lt(Coupon::getEndTime, now);
        }
        Page<Coupon> page = couponMapper.selectPage(new Page<>(query.page(), query.size()), wrapper);
        Page<CouponVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(c -> toVO(c, now)).toList());
        return result;
    }

    /** 券中心：在有效期内且有余量，标记当前用户是否已领 */
    public List<CenterCouponVO> receivable(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .le(Coupon::getStartTime, now).ge(Coupon::getEndTime, now)
                .gt(Coupon::getRemain, 0)
                .orderByDesc(Coupon::getStartTime));
        if (coupons.isEmpty()) {
            return List.of();
        }
        Set<Long> receivedIds = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, userId)
                        .in(UserCoupon::getCouponId, coupons.stream().map(Coupon::getId).toList()))
                .stream().map(UserCoupon::getCouponId).collect(Collectors.toSet());
        return coupons.stream().map(c -> new CenterCouponVO(c.getId(), c.getName(), c.getType(),
                c.getThresholdAmount(), c.getDiscountAmount(), c.getDiscountRate(),
                c.getTotalCount(), c.getRemain(), c.getStartTime(), c.getEndTime(),
                receivedIds.contains(c.getId()))).toList();
    }

    public static String fenToYuanText(long fen) {
        return new BigDecimal(fen).movePointLeft(2).toPlainString();
    }

    public static String derivedStatus(Coupon coupon, LocalDateTime now) {
        if (now.isBefore(coupon.getStartTime())) {
            return "NOT_STARTED";
        }
        if (now.isAfter(coupon.getEndTime())) {
            return "ENDED";
        }
        return "IN_PROGRESS";
    }

    private CouponVO toVO(Coupon c, LocalDateTime now) {
        return new CouponVO(c.getId(), c.getName(), c.getType(), c.getThresholdAmount(),
                c.getDiscountAmount(), c.getDiscountRate(), c.getTotalCount(), c.getRemain(),
                c.getTotalCount() - c.getRemain(), derivedStatus(c, now),
                c.getStartTime(), c.getEndTime(), c.getCreatedAt());
    }

    private void apply(Coupon coupon, CouponUpsertRequest r) {
        coupon.setName(r.name());
        coupon.setType(r.type());
        coupon.setThresholdAmount(r.thresholdAmount());
        coupon.setDiscountAmount(r.discountAmount() == null ? 0L : r.discountAmount());
        coupon.setDiscountRate(r.discountRate());
        coupon.setTotalCount(r.totalCount());
        coupon.setStartTime(r.startTime());
        coupon.setEndTime(r.endTime());
    }

    private void validate(CouponUpsertRequest r) {
        if (!TYPE_FULL_REDUCTION.equals(r.type()) && !TYPE_DISCOUNT.equals(r.type())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "券类型只能是 FULL_REDUCTION 或 DISCOUNT");
        }
        if (!r.endTime().isAfter(r.startTime())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "失效时间必须晚于生效时间");
        }
        if (TYPE_FULL_REDUCTION.equals(r.type()) && (r.discountAmount() == null || r.discountAmount() <= 0)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "满减券必须填优惠金额");
        }
        if (TYPE_DISCOUNT.equals(r.type()) && r.discountRate() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "折扣券必须填折扣率（90=9折）");
        }
        if (r.discountAmount() != null && r.discountAmount() > 0
                && r.thresholdAmount() > 0 && r.discountAmount() >= r.thresholdAmount()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "满减金额不能大于等于门槛金额");
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}