package com.mealorder.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * M5 延迟取消主通道：x-delayed-message 交换机（依赖定制镜像的延迟插件）
 * 交换机/队列/路由键全部走配置，management 控制台可见，便于证明配置被读到
 */
@Configuration
public class MqConfig {

    @Value("${order.mq.delay-exchange}")
    private String delayExchange;

    @Value("${order.mq.delay-queue}")
    private String delayQueue;

    @Value("${order.mq.routing-key}")
    private String routingKey;

    @Bean
    public CustomExchange orderDelayExchange() {
        return new CustomExchange(delayExchange, "x-delayed-message", true, false,
                Map.of("x-delayed-type", "direct"));
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(delayQueue).build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(routingKey).noargs();
    }
}