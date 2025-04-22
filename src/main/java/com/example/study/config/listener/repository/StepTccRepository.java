package com.example.study.config.listener.repository;

import com.example.study.config.listener.entity.StepTccState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepTccRepository extends JpaRepository<StepTccState, Long> {}
