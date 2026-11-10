package com.mealorder.coupon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mealorder.common.exception.BusinessException;
import com.mealorder.common.result.ResultCode;
import com.mealorder.coupon.dto.CouponDtos.UserCouponVO;
import com.mealorder.coupon.dto.CouponDtos.UsableCouponVO;
import com.mealorder.coupon.entity.Coupon;
import com.mealorder.coupon.entity.UserCoupon;
import com.mealorder.coupon.mapper.CouponMapper;
import com.mealorder.coupon.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 领券与我的券（计划书 §4.7）
 * 领取三步：① 资格校验（友好提示）② remain 条件扣减（原子防超发）③ 插 user_coupon（uk 兜底并发重复领）
 * ②③ 同一事务：任何一步失败整体回滚，不会出现"扣了余量没发券"或"发了券没扣余量"
 */
@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Transactional(rollbackFor = Exception.class)
    public UserCouponVO receive(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "券尚未开领");
        }
        if (now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "券已过领取期");
        }
        // ① 资格校验：先查一次给友好提示，真正的并发正确性由 ② 条件扣减 + ③ 唯一索引保证
        if (userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId).eq(UserCoupon::getCouponId, couponId)) > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "已领取过该券");
        }
        // ② 原子扣余量
        if (couponMapper.decrementRemain(couponId) == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "券已被领完");
        }
        // ③ 落券；并发重复领在这里撞唯一索引，抛异常回滚 ② 的扣减
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus("UNUSED");
        userCoupon.setCreatedAt(now);
        userCoupon.setUpdatedAt(now);
        try {
            userCouponMapper.insert(userCoupon);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "已领取过该券");
        }
        return toVO(userCoupon, coupon, now);
    }

    /** 我的券：过期为展示层推导（不依赖定时刷状态），USED 保留核销信息 */
    public List<UserCouponVO> mine(Long userId) {
        List<UserCoupon> mine = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId).orderByDesc(UserCoupon::getCreatedAt));
        if (mine.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(
                        mine.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Coupon::getId, Function.identity()));
        return mine.stream()
                .map(uc -> toVO(uc, couponMap.get(uc.getCouponId()), now))
                .toList();
    }

    /**
     * 券可用性校验（计划书 §3.1 下单"券可用性校验"）：归属 → 状态 → 有效期 → 门槛
     * 预览与下单都调它；预览只试算，真正的占用由下单事务内 markUsed CAS 决定
     */
    public Coupon requireUsable(Long userId, Long userCouponId, long totalAmount) {
        UserCoupon mine = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getId, userCouponId).eq(UserCoupon::getUserId, userId));
        if (mine == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        if (!"UNUSED".equals(mine.getStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "USED".equals(mine.getStatus()) ? "该券已使用" : "该券不可用");
        }
        Coupon coupon = couponMapper.selectById(mine.getCouponId());
        if (coupon == null) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "券模板已失效");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "券未到使用时间");
        }
        if (now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "券已过期");
        }
        if (totalAmount < coupon.getThresholdAmount()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "满 " + CouponService.fenToYuanText(coupon.getThresholdAmount()) + " 元可用");
        }
        return coupon;
    }

    /** 结算页选券列表：我的未使用券逐张判定可用性，并给出按当前金额的预计优惠 */
    public List<UsableCouponVO> usableList(Long userId, long totalAmount) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> unused = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId).eq(UserCoupon::getStatus, "UNUSED")
                .orderByDesc(UserCoupon::getCreatedAt));
        if (unused.isEmpty()) {
            return List.of();
        }
        Map<Long, Coupon> couponMap = couponMapper.selectBatchIds(
                        unused.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Coupon::getId, Function.identity()));
        return unused.stream().map(uc -> {
            Coupon c = couponMap.get(uc.getCouponId());
            String reason = null;
            Long estimated = null;
            if (c == null) {
                reason = "券模板已失效";
            } else if (now.isBefore(c.getStartTime())) {
                reason = "未到使用时间";
            } else if (now.isAfter(c.getEndTime())) {
                reason = "已过期";
            } else if (totalAmount < c.getThresholdAmount()) {
                reason = "满 " + CouponService.fenToYuanText(c.getThresholdAmount()) + " 元可用";
            } else {
                estimated = CouponShareCalculator.discountOf(c, totalAmount);
            }
            return new UsableCouponVO(uc.getId(), uc.getCouponId(), c == null ? "券已失效" : c.getName(),
                    c == null ? null : c.getType(), c == null ? null : c.getThresholdAmount(),
                    c == null ? null : c.getDiscountAmount(), c == null ? null : c.getDiscountRate(),
                    c == null ? null : c.getEndTime(), reason == null, estimated, reason);
        }).toList();
    }

    private UserCouponVO toVO(UserCoupon uc, Coupon coupon, LocalDateTime now) {
        String status = uc.getStatus();
        if ("UNUSED".equals(status) && coupon != null && now.isAfter(coupon.getEndTime())) {
            status = "EXPIRED";
        }
        return new UserCouponVO(uc.getId(), uc.getCouponId(), uc.getUsedOrderId(),
                coupon == null ? "券已失效" : coupon.getName(),
                coupon == null ? null : coupon.getType(),
                coupon == null ? null : coupon.getThresholdAmount(),
                coupon == null ? null : coupon.getDiscountAmount(),
                coupon == null ? null : coupon.getDiscountRate(),
                status,
                coupon == null ? null : coupon.getStartTime(),
                coupon == null ? null : coupon.getEndTime(),
                uc.getUsedTime(), uc.getCreatedAt());
    }
}