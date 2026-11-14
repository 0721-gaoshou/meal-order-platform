package com.mealorder.behavior.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mealorder.behavior.entity.UserBehavior;
import com.mealorder.behavior.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 埋点管道：业务线程只入队（不阻塞主链路），定时批量落库
 * flush 失败整批回队下轮重试；埋点属 best-effort，不影响业务事务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BehaviorTracker {

    private final UserBehaviorMapper userBehaviorMapper;

    private final Queue<UserBehavior> queue = new ConcurrentLinkedQueue<>();

    @Value("${behavior.batch-size:200}")
    private int batchSize;

    public void record(Long userId, Long itemId, String action, String scene) {
        UserBehavior behavior = new UserBehavior();
        behavior.setId(IdWorker.getId());
        behavior.setUserId(userId);
        behavior.setItemId(itemId);
        behavior.setAction(action);
        behavior.setScene(scene);
        behavior.setCreatedAt(LocalDateTime.now());
        queue.add(behavior);
    }

    @Scheduled(fixedDelayString = "${behavior.flush-ms:5000}")
    public void flush() {
        List<UserBehavior> batch = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            UserBehavior behavior = queue.poll();
            if (behavior == null) {
                break;
            }
            batch.add(behavior);
        }
        if (batch.isEmpty()) {
            return;
        }
        try {
            userBehaviorMapper.insertBatch(batch);
            log.info("behavior flush: {} rows", batch.size());
        } catch (Exception e) {
            queue.addAll(batch);
            log.warn("behavior flush 失败回队重试: {} rows", batch.size(), e);
        }
    }
}