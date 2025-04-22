package com.example.study;

import lombok.RequiredArgsConstructor;
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

            if (jobName == null || !jobs.containsKey(jobName)) {
                throw new IllegalArgumentException("🚫 유효한 job_name을 지정하세요: " + jobName);
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