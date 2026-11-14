package com.mealorder.mq;

import com.mealorder.order.entity.Order;
import com.mealorder.order.mapper.OrderMapper;
import com.mealorder.order.service.OrderCancelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 延迟到期消费：走统一取消出口（CAS PENDING_PAY→CLOSED），与扫表兜底竞态时只有一方生效
 * 消费异常不 requeue：避免毒消息无限重投，失败由兜底扫表接管重试
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayConsumer {

    private final OrderMapper orderMapper;
    private final OrderCancelService orderCancelService;

    @RabbitListener(queues = "${order.mq.delay-queue}")
    public void onDelayExpire(String orderId) {
        try {
            Order order = orderMapper.selectById(Long.valueOf(orderId));
            if (order == null || !"PENDING_PAY".equals(order.getStatus())) {
                return;
            }
            if (LocalDateTime.now().isBefore(order.getExpireTime())) {
                return;
            }
            boolean cancelled = orderCancelService.cancel(order);
            log.info("延迟到期取消订单: orderId={}, cancelled={}", orderId, cancelled);
        } catch (Exception e) {
            log.warn("延迟取消消费失败，兜底扫表接管: orderId={}", orderId, e);
        }
    }
}