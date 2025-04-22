package com.example.study.service;

import com.example.study.config.listener.entity.ChunkTccState;
import com.example.study.config.listener.entity.JobTccState;
import com.example.study.config.listener.entity.StepTccState;
import com.example.study.config.listener.repository.ChunkTccRepository;
import com.example.study.config.listener.repository.JobTccRepository;
import com.example.study.config.listener.repository.StepTccRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TccStateService {
    private final JobTccRepository jobRepo;
    private final StepTccRepository stepRepo;
    private final ChunkTccRepository chunkRepo;

    public void saveJobState(Long jobId, String status) {
        jobRepo.save(JobTccState.builder()
                .jobId(jobId)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void saveStepState(Long stepId, Long jobId, String status) {
        stepRepo.save(StepTccState.builder()
                .stepId(stepId)
                .jobId(jobId)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build());
    }

    public void saveChunkState(Long stepId, Long chunkCount, String status) {
        chunkRepo.save(ChunkTccState.builder()
                .stepId(stepId)
                .chunkCount(chunkCount)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build());
    }
}