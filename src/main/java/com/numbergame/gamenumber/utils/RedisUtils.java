package com.numbergame.gamenumber.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for Redis operations
 */
@Component
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtils(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Acquire distributed lock
     *
     * @param lockKey Lock key
     * @param timeout Lock timeout in seconds
     * @return true if lock acquired
     */
    public boolean acquireLock(String lockKey, long timeout) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", timeout, TimeUnit.SECONDS)
        );
    }

    /**
     * Release distributed lock
     *
     * @param lockKey Lock key
     */
    public void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }

    /**
     * Set value with expiration
     *
     * @param key Redis key
     * @param value Value to set
     * @param timeout Timeout
     * @param unit Time unit
     */
    public void setWithExpiration(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * Get value by key
     *
     * @param key Redis key
     * @return Value or null
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Delete key
     *
     * @param key Redis key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Check if key exists
     *
     * @param key Redis key
     * @return true if exists
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Increment value
     *
     * @param key Redis key
     * @return Incremented value
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * Add to sorted set
     *
     * @param key Sorted set key
     * @param value Value
     * @param score Score
     */
    public void addToSortedSet(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }
}

