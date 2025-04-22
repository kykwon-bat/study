package com.example.study.scheduler;

import com.example.study.repository.ItemProcessorRegistry;
import com.example.study.service.RedisQueueService;
import com.example.study.service.StepLauncherService;
import com.example.study.service.dto.JobQueuePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisQueuePollingTask {

    private final RedisQueueService redisQueueService;
    private final ItemProcessorRegistry processorRegistry;
    private final StepLauncherService stepLauncherService;
    //private final EmployeeService employeeService; // 실제 DB 업데이트 등 처리할 서비스

    @Scheduled(fixedDelay = 5000)
    public void processQueuedItems() {
        Map<String, List<Object>> jobPayloadMap = new HashMap<>();
        Object item;

        while ((item = redisQueueService.dequeue()) != null) {
            log.error("👀 클래스: {}, 로더: {}", item.getClass(), item.getClass().getClassLoader());

            if (item instanceof JobQueuePayload queueItem) {
                String jobName = queueItem.getJobName();
                Object payload = queueItem.getPayload();

                log.info("🔁 Job: {}, Payload Type: {}", jobName, payload.getClass().getSimpleName());

                try {
                    ItemProcessor<Object, Object> processor = processorRegistry.get(jobName);
                    if (processor == null) {
                        log.warn("❌ 해당 jobName에 대한 processor가 없습니다: {}", jobName);
                        continue;
                    }

                    Object result = processor.process(payload);
                    log.info("✅ Processor 결과: {}", result);

                    if (result != null) {
                        jobPayloadMap.computeIfAbsent(jobName, k -> new ArrayList<>()).add(result);
                    }
                } catch (Exception e) {
                    log.error("❌ Processor 실행 중 오류: {}", e.getMessage(), e);
                }
            } else {
                log.warn("⚠️ 예상치 못한 큐 객체 타입: {}", item.getClass());
            }
        }


        // 📝 모든 큐 처리 후 job별 ItemWriter 호출
        jobPayloadMap.forEach((jobName, results) -> {
            try {
                stepLauncherService.launchStepWriterFromJob(jobName, results);
            } catch (Exception e) {
                log.error("❌ Writer 실행 중 오류 (jobName: {}): {}", jobName, e.getMessage(), e);
            }
        });
    }
}