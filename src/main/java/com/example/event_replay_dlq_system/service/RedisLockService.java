package com.example.event_replay_dlq_system.service;


import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisLockService {


    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisLockService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     *
     * @param lockKey The Lock Key
     * @param timeOut TTL in seconds
     * @return true if lock acquired, false if already locked
     */
    public boolean acquireLock(String lockKey, int timeOut) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(timeOut)));

    }


    /**
     * Release lock manually
     *
     * @param lockKey The Lock Key
     */
    public void releaseLock(String lockKey) {
        Boolean deleted = redisTemplate.delete(lockKey);
        if (deleted) {
            log.debug("{} has been released", lockKey);
        } else {
            log.debug("{} not found ", lockKey);
        }
    }

    /**
     * Get remaining TTL of lock
     *
     * @param lockKey The Lock KEY
     * @return TTL in seconds, -1 if no expiration, -2 if key doesn't exist
     */
    public Long getLockTTL(String lockKey) {
        return redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
    }




}
