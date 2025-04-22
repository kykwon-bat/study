package com.example.study.config.listener.repository;

import com.example.study.config.listener.entity.ChunkTccState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChunkTccRepository extends JpaRepository<ChunkTccState, Long> {}