package com.silky.starter.redis.ratelimiter.service;

import com.silky.starter.redis.ratelimiter.annotation.RateLimit;
import com.silky.starter.redis.ratelimiter.config.RateLimitConfig;
import com.silky.starter.redis.ratelimiter.exception.RateLimitExceededException;

import java.util.concurrent.TimeUnit;

/**
 * Redis限流器接口
 *
 * @author zy
 * @date 2025-10-23 10:36
 **/
public interface RedisRateLimiter {

    /**
     * 获取令牌，获取不到则阻塞等待
     *
     * @param key 限流key
     */
    boolean acquire(String key) throws RateLimitExceededException;

    /**
     * 获取指定数量的令牌，获取不到则阻塞等待
     *
     * @param key     限流key
     * @param permits 令牌数量
     */
    boolean acquire(String key, int permits) throws RateLimitExceededException;

    /**
     * 尝试获取令牌（非阻塞）
     *
     * @param key 限流key
     */
    boolean tryAcquire(String key) throws RateLimitExceededException;

    /**
     * 尝试获取指定数量的令牌（非阻塞）
     *
     * @param key     限流key
     * @param permits 令牌数量
     */
    boolean tryAcquire(String key, int permits) throws RateLimitExceededException;


    /**
     * 尝试获取指定数量的令牌（非阻塞）
     *
     * @param key     限流key
     * @param permits 令牌数量
     * @param config  限流配置
     */
    boolean tryAcquire(String key, int permits, RateLimitConfig config) throws RateLimitExceededException;

    /**
     * 尝试获取令牌（非阻塞）
     *
     * @param key       限流key
     * @param rateLimit 限流注解配置
     */
    boolean tryAcquire(String key, RateLimit rateLimit) throws RateLimitExceededException;

    /**
     * 获取指定数量的令牌，获取不到则阻塞等待
     *
     * @param key     限流key
     * @param timeout 等待超时时间
     * @param unit    时间单位
     */
    boolean tryAcquire(String key, long timeout, TimeUnit unit) throws RateLimitExceededException;

    /**
     * 获取指定数量的令牌，获取不到则阻塞等待
     *
     * @param key     限流key
     * @param permits 令牌数量
     * @param timeout 等待超时时间
     * @param unit    时间单位
     */
    boolean tryAcquire(String key, int permits, long timeout, TimeUnit unit) throws RateLimitExceededException;

    /**
     * 获取指定数量的令牌，获取不到则阻塞等待
     *
     * @param key     限流key
     * @param permits 令牌数量
     * @param config  限流配置
     * @param timeout 等待超时时间
     * @param unit    时间单位
     */
    boolean tryAcquire(String key, int permits, RateLimitConfig config, long timeout, TimeUnit unit)
            throws RateLimitExceededException;


    /**
     * 获取令牌，获取不到则阻塞等待
     *
     * @param key 限流key
     */
    RateLimitConfig getConfig(String key);

    /**
     * 重置限流器
     *
     * @param key 限流key
     */
    void reset(String key);
}
