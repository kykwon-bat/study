package com.example.study.config.listener;

import com.example.study.service.TccStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.batch.core.ExitStatus;

@Component
@RequiredArgsConstructor
@Slf4j
public class StepTccListener implements StepExecutionListener {

    private final TccStateService tccStateService;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        tccStateService.saveStepState(stepExecution.getId(), stepExecution.getJobExecutionId(), "TRY");
        log.info("[TCC][STEP][TRY] {}", stepExecution.getId());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String status = stepExecution.getStatus().isUnsuccessful() ? "CANCEL" : "CONFIRM";
        tccStateService.saveStepState(stepExecution.getId(), stepExecution.getJobExecutionId(), status);
        log.info("[TCC][STEP][{}] {}", status, stepExecution.getId());
        return stepExecution.getExitStatus();
    }
}