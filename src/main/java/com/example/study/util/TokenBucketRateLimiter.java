package com.example.study.util;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@EnableConfigurationProperties(CacheProperties.class)
@Slf4j
public class TokenBucketRateLimiter{

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;
    private final String key = "token_bucket"; // Redis 키
    private final CacheProperties cacheProperties;
    private final int capacity;
    private final int refillTokens;
    private final long intervalMillis;

    public TokenBucketRateLimiter(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry, CacheProperties cacheProperties,
                                  @Value("${rate-limiter.capacity}") int capacity,
                                  @Value("${rate-limiter.refill.tokens}") int refillTokens,
                                  @Value("${rate-limiter.refill.interval:-1}") long configuredIntervalMillis) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
        this.cacheProperties = cacheProperties;
        this.capacity = capacity;
        this.refillTokens = refillTokens;

        // ✅ cacheProperties에서 TTL 설정을 가져와 interval로 활용 (우선순위: 명시된 값 > TTL > 기본 1000ms)
        Duration ttl = cacheProperties.getRedis().getTimeToLive();
        this.intervalMillis = configuredIntervalMillis > 0 ? configuredIntervalMillis
                : (ttl != null && !ttl.isZero()) ? ttl.toMillis()
                : 1000L;
    }

    public synchronized boolean tryConsume() {
        long now = Instant.now().getEpochSecond();

        String lastRefillKey = key + ":last_refill";
        String tokensKey = key + ":tokens";

        String lastRefillStr = redisTemplate.opsForValue().get(lastRefillKey);
        String tokenStr = redisTemplate.opsForValue().get(tokensKey);

        long lastRefill = lastRefillStr != null ? Long.parseLong(lastRefillStr) : now;

        long tokens = Optional.ofNullable(redisTemplate.opsForValue().get(tokensKey))
                .map(Long::parseLong).orElse((long) capacity);

        long elapsed = now - lastRefill;
        double refillRatePerSec = (double) refillTokens / (intervalMillis / 1000.0);
        long newTokens = (long) (elapsed * refillRatePerSec);

//        log.info("🕒 현재 시각: {}",
//                Instant.ofEpochSecond(now)
//                        .atZone(ZoneId.systemDefault()) // ZonedDateTime
//                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
//        log.info("🕒 마지막 리필 시각: {}",
//                Instant.ofEpochSecond(lastRefill)
//                        .atZone(ZoneId.systemDefault())
//                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
//        log.info("⏱ 경과 시간: {}초", elapsed);
//        log.info("💧 리필 비율: {} tokens/sec", String.format("%.4f", refillRatePerSec));
//        log.info("💧 리필 대상 토큰: {}", newTokens);
//        log.info("🪙 기존 토큰: {}", tokens);

        // 초기화
        if (lastRefillStr == null || tokenStr == null) {
            redisTemplate.opsForValue().set(lastRefillKey, String.valueOf(now));
            redisTemplate.opsForValue().set(tokensKey, String.valueOf(capacity));
            System.out.println("🔧 토큰 버킷 초기화: " + capacity + "개");
            return tryConsume(); // 재귀 호출
        }

        System.out.println("🔥 Redis Token Bucket 상태");
        System.out.println("남은 토큰 수: " + tokens);
        System.out.println("마지막 refill 시간 (epoch 초): " + lastRefill);

        if (newTokens > 0) {
            tokens = Math.min(capacity, tokens + newTokens);
            redisTemplate.opsForValue().set(tokensKey, String.valueOf(tokens));
            redisTemplate.opsForValue().set(lastRefillKey, String.valueOf(now));
            log.info("🔄 토큰 리필됨: 새 토큰 수={}, 총 토큰 수={}", newTokens, tokens);
            meterRegistry.counter("token_bucket.refill_count").increment();
        }

        meterRegistry.gauge("token_bucket.tokens_remaining", tokens);

        if (tokens > 0) {
            redisTemplate.opsForValue().decrement(tokensKey);
            log.info("✅ 토큰 소비 후 남은 수: {}", tokens - 1);
            meterRegistry.counter("token_bucket.requests_allowed").increment();
            return true;
        }else{
            log.warn("⛔ 토큰 부족! 현재 남은 수: {}", tokens);
            meterRegistry.counter("token_bucket.requests_denied").increment();
            return false;
        }
    }
}