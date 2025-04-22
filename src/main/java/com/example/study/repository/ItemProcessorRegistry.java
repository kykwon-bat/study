package com.example.study.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ItemProcessorRegistry {

    private final Map<String, ItemProcessor<?, ?>> processorMap = new HashMap<>();

    public void register(String jobName, ItemProcessor<?, ?> processor) {
        processorMap.put(jobName, processor);
    }

    public ItemProcessor<Object, Object> get(String jobName) {
        return (ItemProcessor<Object, Object>) processorMap.get(jobName);
    }
}