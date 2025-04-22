package com.example.study.support;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class ChunkIndexTracker {
    private static final ThreadLocal<Long> threadLocalChunkIndex = new ThreadLocal<>();

    public static void setChunkIndex(long index) {
        threadLocalChunkIndex.set(index);
    }

    public static Long getChunkIndex() {
        return threadLocalChunkIndex.get();
    }

    public static void clear() {
        threadLocalChunkIndex.remove();
    }
}