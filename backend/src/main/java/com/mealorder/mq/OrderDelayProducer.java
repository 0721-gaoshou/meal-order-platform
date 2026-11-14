package com.mealorder.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 下单提交后投递延迟取消消息：x-delay = 距 expire_time 的毫秒数
 * 发送失败只告警不抛错——扫表兜底始终保留，MQ 故障不影响下单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDelayProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${order.mq.delay-exchange}")
    private String delayExchange;

    @Value("${order.mq.routing-key}")
    private String routingKey;

    public void sendCancelDelay(Long orderId, LocalDateTime expireTime) {
        long delayMs = Math.max(0, Duration.between(LocalDateTime.now(), expireTime).toMillis());
        rabbitTemplate.convertAndSend(delayExchange, routingKey, orderId.toString(), message -> {
            message.getMessageProperties().setHeader("x-delay", (int) Math.min(delayMs, Integer.MAX_VALUE));
            return message;
        });
        log.info("延迟取消消息已投递: orderId={}, delayMs={}", orderId, delayMs);
    }
}