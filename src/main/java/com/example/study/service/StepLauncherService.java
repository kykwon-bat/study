package com.example.study.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StepLauncherService {

    // writer 레지스트리 (jobName → ItemWriter)
    private final Map<String, ItemWriter<Object>> writerRegistry = new ConcurrentHashMap<>();

    /**
     * StepWriter를 jobName에 따라 등록
     */
    public void registerWriter(String jobName, ItemWriter<?> writer) {
        writerRegistry.put(jobName, (ItemWriter<Object>) writer);
        log.info("📝 Writer 등록됨: {}", jobName);
    }

    /**
     * 큐가 비어있을 때 Writer만 호출하는 로직
     */
    public void launchStepWriterFromJob(String jobName, List<Object> items) {
        ItemWriter<Object> writer = writerRegistry.get(jobName);

        if (writer == null) {
            log.warn("❌ Writer가 존재하지 않음: {}", jobName);
            return;
        }

        try {
            log.info("🚀 Writer 실행 시작 - jobName: {}, 아이템 수: {}", jobName, items.size());
            writer.write((Chunk<?>) items);
            log.info("✅ Writer 처리 완료");
        } catch (Exception e) {
            log.error("❌ Writer 실행 중 예외 발생 - jobName: {}", jobName, e);
        }
    }
}
