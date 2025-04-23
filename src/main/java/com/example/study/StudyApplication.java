package com.example.study;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@SpringBootApplication
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class StudyApplication{

    private final JobLauncher jobLauncher;
    private final Map<String, Job> jobs;

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        SpringApplication.run(StudyApplication.class, args);
    }

    @Bean
    public ApplicationRunner jobRunner(ApplicationArguments args) {
        return arguments -> {

            System.out.println("✅ Redis Host: " + env.getProperty("spring.data.redis.host"));

            String jobName = arguments.containsOption("job_name")
                    ? arguments.getOptionValues("job_name").get(0)
                    : null;

            if (jobName == null) {
                log.error("❌ 실행할 job_name 파라미터가 없습니다. 예: --job_name=employeeJob");
                System.exit(1);
            }

            if (!jobs.containsKey(jobName)) {
                log.error("❌ 등록되지 않은 Job 이름입니다: {}", jobName);
                System.exit(1);
            }

            Job job = jobs.get(jobName);

            JobParameters params = new JobParametersBuilder()
                    .addString("job_name", jobName)
                    .addLong("timestamp", System.currentTimeMillis()) // 중복 방지용
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(job, params);
            System.out.println("✅ 실행 완료: " + execution.getStatus());
        };
    }
}