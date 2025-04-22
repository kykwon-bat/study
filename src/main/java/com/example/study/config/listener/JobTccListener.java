package com.example.study.config.listener;

import com.example.study.service.TccStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobTccListener implements JobExecutionListener {

    private final TccStateService tccStateService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        tccStateService.saveJobState(jobExecution.getId(), "TRY");
        log.info("[TCC][JOB][TRY] {}", jobExecution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String status = jobExecution.getStatus().isUnsuccessful() ? "CANCEL" : "CONFIRM";
        tccStateService.saveJobState(jobExecution.getId(), status);
        log.info("[TCC][JOB][{}] {}", status, jobExecution.getId());

//        List<Object> queuedItems = redisQueueService.getAllQueuedItems();
//        if (!queuedItems.isEmpty()) {
//            System.out.println("❗ Rate Limited Items in Redis Queue:");
//            for (Object item : queuedItems) {
//                System.out.println(" -> " + item);
//            }
//        } else {
//            System.out.println("✅ No rate-limited items found in Redis.");
//        }
    }
}