package com.example.study.config.listener.repository;

import com.example.study.config.listener.entity.JobTccState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobTccRepository extends JpaRepository<JobTccState, Long> {}
