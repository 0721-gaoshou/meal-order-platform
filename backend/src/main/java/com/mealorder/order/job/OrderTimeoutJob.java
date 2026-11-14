package com.mealorder.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mealorder.order.entity.Order;
import com.mealorder.order.mapper.OrderMapper;
import com.mealorder.order.service.OrderCancelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时未支付订单自动取消（M5 兜底方案）：
 * 每 order.timeout-scan-ms 扫一次 PENDING_PAY 且 expire_time 已过的单，逐单走统一取消出口
 * 说明：延迟队列（RabbitMQ 延迟插件）作为时效主通道待 M5 定制镜像后接入，本扫表始终保留兜底
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private static final int BATCH_SIZE = 100;

    private final OrderMapper orderMapper;
    private final OrderCancelService orderCancelService;

    @Scheduled(fixedDelayString = "${order.timeout-scan-ms:30000}")
    public void scheduledScan() {
        int cancelled = scanOnce();
        if (cancelled > 0) {
            log.info("超时订单自动取消: {} 笔", cancelled);
        }
    }

    /** 可手动触发（管理端接口/验收脚本），返回本次取消笔数 */
    public int scanOnce() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                        .select(Order::getId)
                        .eq(Order::getStatus, "PENDING_PAY")
                        .lt(Order::getExpireTime, now)
                        .last("LIMIT " + BATCH_SIZE))
                .stream().map(Order::getId).toList();
        int cancelled = 0;
        for (Long id : ids) {
            Order order = orderMapper.selectById(id);
            // 二次确认：扫描后可能已被支付/取消
            if (order == null || !"PENDING_PAY".equals(order.getStatus())
                    || !LocalDateTime.now().isAfter(order.getExpireTime())) {
                continue;
            }
            try {
                if (orderCancelService.cancel(order)) {
                    cancelled++;
                }
            } catch (Exception e) {
                log.warn("超时取消失败，等待下轮重试: orderId={}", id, e);
            }
        }
        return cancelled;
    }
}