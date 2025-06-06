package com.example.study.config.listener.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class ChunkTccState {
    @Id
    @GeneratedValue
    private Long id;
    private Long stepId;
    private Long chunkCount;
    private String status;
    private LocalDateTime createdAt;
}