package com.example.study.job;

import com.example.study.config.listener.ChunkTccListener;
import com.example.study.config.listener.JobTccListener;
import com.example.study.config.listener.StepTccListener;
import com.example.study.repository.EmployeeRepository;
import com.example.study.repository.ItemProcessorRegistry;
import com.example.study.service.RedisQueueService;
import com.example.study.support.ChunkIndexTracker;
import com.example.study.util.TokenBucketRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class Employee {

    private final EmployeeRepository employeeRepository;

    @Autowired
    private TokenBucketRateLimiter rateLimiter;

    @Qualifier("taskExecutor")
    private final TaskExecutor taskExecutor;

    @Autowired
    private final RedisQueueService queueService;

    @Autowired
    private final ItemProcessorRegistry processorRegistry;

    @Bean
    public Job employeeJob(JobRepository jobRepository, Step employeeStep, JobTccListener jobListener) {
        return new JobBuilder("employeeJob", jobRepository)
                .start(employeeStep)
                .listener(jobListener)
                .build();
    }

    @Bean
    public Step employeeStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, StepTccListener stepListener, ChunkTccListener chunkListener) {
        return new StepBuilder("employeeStep", jobRepository)
                .<com.example.study.entity.Employee, com.example.study.entity.Employee>chunk(10, transactionManager)
                .reader(employeeItemReader())
                .processor(employeeItemProcessor())
                .writer(employeeItemWriter())
                .listener(stepListener)
                .listener(chunkListener)
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<com.example.study.entity.Employee> employeeItemReader() {
        return new ItemReader<>() {
            private Iterator<com.example.study.entity.Employee> iterator;
            @Override
            public com.example.study.entity.Employee read() {
                if (iterator == null) {
                    List<com.example.study.entity.Employee> employees = employeeRepository.findAll();
                    iterator = employees.iterator();
                }

                if (iterator.hasNext()) {
                    com.example.study.entity.Employee emp = iterator.next();
                    log.info("[READ] {}", emp);
                    return emp;
                } else {
                    return null;
                }
            }
        };
    }

    @Bean
    @StepScope
    public ItemProcessor<com.example.study.entity.Employee, com.example.study.entity.Employee> employeeItemProcessor() {
        ItemProcessor<com.example.study.entity.Employee, com.example.study.entity.Employee> processor = item -> {
            Long chunkIndex = ChunkIndexTracker.getChunkIndex();
            log.info("[PROCESS] {}, handled in chunk {}", item, chunkIndex);

            if (!rateLimiter.tryConsume()) {
                log.warn("Rate limited. Queueing item: {}", item);
                queueService.enqueueWithJobName("employeeJob", item);
                return null;
            }

            item.setSalary(item.getSalary().multiply(new BigDecimal("1.1")));
            log.info("[PROCESS] {}", item);
            return item;
        };

        processorRegistry.register("employeeJob", processor);
        return processor;
    }

    @Bean
    @StepScope
    public ItemWriter<com.example.study.entity.Employee> employeeItemWriter() {
        return items -> {
            employeeRepository.saveAll(items);
            for (com.example.study.entity.Employee emp : items) {
                log.info("[WRITE] {}", emp);
            }
        };
    }
}