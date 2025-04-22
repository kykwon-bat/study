package com.example.study.config.listener;

import com.example.study.service.TccStateService;
import com.example.study.support.ChunkIndexTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChunkTccListener implements ChunkListener {

    private final TccStateService tccStateService;
    private final AtomicLong chunkCounter = new AtomicLong(0);

    @Override
    public void beforeChunk(ChunkContext context) {
        long currentChunkIndex = chunkCounter.incrementAndGet();
        ChunkIndexTracker.setChunkIndex(currentChunkIndex);
        log.info("[CHUNK START] Index: {}", currentChunkIndex);
        saveState(context, "TRY");
    }

    @Override
    public void afterChunk(ChunkContext context) {
        saveState(context, "CONFIRM");
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        saveState(context, "CANCEL");
    }

    private void saveState(ChunkContext context, String status) {
        StepContext stepCtx = context.getStepContext();
        long stepId = stepCtx.getStepExecution().getId();
        long chunkCount = stepCtx.getStepExecution().getReadCount();
        tccStateService.saveChunkState(stepId, chunkCount, status);
        log.info("[TCC][CHUNK][{}] step={}, chunk={}", status, stepId, chunkCount);
    }
}
