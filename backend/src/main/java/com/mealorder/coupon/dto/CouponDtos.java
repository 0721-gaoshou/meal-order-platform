package com.mealorder.coupon.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 优惠券模块 DTO 集中定义（与 GoodsDtos 同风格）
 */
public final class CouponDtos {

    private CouponDtos() {
    }

    /**
     * 券模板创建/编辑：满减券填 discountAmount，折扣券填 discountRate（90=9折）
     * 编辑时 totalCount 不得小于已领取数，余量由服务端按已发数重算
     */
    public record CouponUpsertRequest(
            @NotBlank(message = "券名不能为空") @Size(max = 128, message = "券名最长 128 字") String name,
            @NotBlank(message = "缺少券类型") String type,
            @NotNull(message = "缺少使用门槛") @Min(value = 0, message = "门槛不能为负") Long thresholdAmount,
            @Min(value = 0, message = "优惠金额不能为负") Long discountAmount,
            @Min(value = 1, message = "折扣率需大于 0") @Max(value = 99, message = "折扣率需小于 100") Integer discountRate,
            @NotNull(message = "缺少发行总量") @Min(value = 1, message = "发行总量至少 1 张") Integer totalCount,
            @NotNull(message = "缺少生效时间") LocalDateTime startTime,
            @NotNull(message = "缺少失效时间") LocalDateTime endTime) {
    }

    public record CouponQuery(
            String keyword,
            String type,
            /** NOT_STARTED / IN_PROGRESS / ENDED */
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

    public record CouponVO(
            @JsonSerialize(using = ToStringSerializer.class) Long id,
            String name,
            String type,
            Long thresholdAmount,
            Long discountAmount,
            Integer discountRate,
            Integer totalCount,
            Integer remain,
            Integer issued,
            /** 按时间区间推导：NOT_STARTED / IN_PROGRESS / ENDED */
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime createdAt) {
    }

    /** 券中心条目：模板信息 + 当前用户是否已领 */
    public record CenterCouponVO(
            @JsonSerialize(using = ToStringSerializer.class) Long id,
            String name,
            String type,
            Long thresholdAmount,
            Long discountAmount,
            Integer discountRate,
            Integer totalCount,
            Integer remain,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Boolean received) {
    }

    /** 结算页选券列表：可用性与预计优惠额（真实核销在下单事务内 CAS） */
    public record UsableCouponVO(
            @JsonSerialize(using = ToStringSerializer.class) Long userCouponId,
            @JsonSerialize(using = ToStringSerializer.class) Long couponId,
            String name,
            String type,
            Long thresholdAmount,
            Long discountAmount,
            Integer discountRate,
            LocalDateTime couponEndTime,
            Boolean usable,
            /** usable=true 时按传入金额算出的预计优惠·分 */
            Long estimatedDiscount,
            /** usable=false 时的原因 */
            String reason) {
    }

    public record UserCouponVO(
            @JsonSerialize(using = ToStringSerializer.class) Long id,
            @JsonSerialize(using = ToStringSerializer.class) Long couponId,
            @JsonSerialize(using = ToStringSerializer.class) Long usedOrderId,
            String name,
            String type,
            Long thresholdAmount,
            Long discountAmount,
            Integer discountRate,
            /** UNUSED / USED / EXPIRED（过期为展示层推导，不依赖定时刷库） */
            String status,
            LocalDateTime couponStartTime,
            LocalDateTime couponEndTime,
            LocalDateTime usedTime,
            LocalDateTime createdAt) {
    }
}