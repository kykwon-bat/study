package com.example.study.service;

import com.example.study.service.dto.JobQueuePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String QUEUE_KEY = "rate_limited_queue";

    public void enqueue(String jobName, Object item) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("jobName", jobName);
        wrapper.put("payload", item);
        redisTemplate.opsForList().rightPush(QUEUE_KEY, wrapper);
        log.debug("✅ Redis Queue에 저장됨: jobName={}, payload={}", jobName, item);
    }

    public JobQueuePayload dequeue() {
        return (JobQueuePayload) redisTemplate.opsForList().leftPop(QUEUE_KEY);
    }

    public List<Object> getAllQueuedItems() {
        Long size = redisTemplate.opsForList().size(QUEUE_KEY);
        return size == null || size == 0
                ? Collections.emptyList()
                : redisTemplate.opsForList().range(QUEUE_KEY, 0, size);
    }

    public void clearQueue() {
        redisTemplate.delete(QUEUE_KEY);
    }

    public void enqueueWithJobName(String jobName, Object payload) {
        JobQueuePayload wrapper = new JobQueuePayload(jobName, payload);
        redisTemplate.opsForList().rightPush(QUEUE_KEY, wrapper);
    }
}