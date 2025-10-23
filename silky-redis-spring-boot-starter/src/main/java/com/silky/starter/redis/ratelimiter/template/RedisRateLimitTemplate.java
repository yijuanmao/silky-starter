package com.silky.starter.redis.ratelimiter.template;

import com.silky.starter.redis.ratelimiter.config.RateLimitConfig;
import com.silky.starter.redis.ratelimiter.exception.RateLimitExceededException;
import com.silky.starter.redis.ratelimiter.service.RedisRateLimiter;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis限流模板类
 *
 * @author zy
 * @date 2025-10-23 11:00
 **/
public class RedisRateLimitTemplate {

    private final RedisRateLimiter redisRateLimiter;

    public RedisRateLimitTemplate(RedisRateLimiter redisRateLimiter) {
        this.redisRateLimiter = redisRateLimiter;
    }

    /**
     * 使用默认配置执行限流操作
     *
     * @param key      限流key
     * @param supplier 限流操作
     */
    public <T> T execute(String key, Supplier<T> supplier) {
        return execute(key, supplier, () -> {
            throw new RateLimitExceededException("Rate limit exceeded for key: " + key);
        });
    }

    /**
     * 使用默认配置执行限流操作，支持降级
     *
     * @param key              限流key
     * @param supplier         限流操作
     * @param fallbackSupplier 降级操作
     */
    public <T> T execute(String key, Supplier<T> supplier, Supplier<T> fallbackSupplier) {
        boolean acquired = redisRateLimiter.tryAcquire(key);

        if (acquired) {
            return supplier.get();
        } else {
            return fallbackSupplier.get();
        }
    }

    /**
     * 使用指定配置执行限流操作
     *
     * @param key      限流key
     * @param supplier 限流操作
     * @param config   限流配置
     */
    public <T> T execute(String key, RateLimitConfig config, Supplier<T> supplier) {
        return execute(key, config, supplier, () -> {
            throw new RateLimitExceededException("Rate limit exceeded for key: " + key);
        });
    }

    /**
     * 使用指定配置执行限流操作，支持降级
     *
     * @param key              限流key
     * @param config           限流配置
     * @param supplier         限流操作
     * @param fallbackSupplier 降级操作
     */
    public <T> T execute(String key, RateLimitConfig config,
                         Supplier<T> supplier, Supplier<T> fallbackSupplier) {
        boolean acquired = redisRateLimiter.tryAcquire(key, 1, config);

        if (acquired) {
            return supplier.get();
        } else {
            return fallbackSupplier.get();
        }
    }

    /**
     * 使用指定配置执行限流操作（无返回值）
     *
     * @param key      限流key
     * @param config   限流配置
     * @param runnable 限流操作
     */
    public void execute(String key, RateLimitConfig config, Runnable runnable) {
        execute(key, config, runnable, () -> {
            throw new RateLimitExceededException("Rate limit exceeded for key: " + key);
        });
    }

    /**
     * 使用指定配置执行限流操作（无返回值），支持降级
     *
     * @param key              限流key
     * @param config           限流配置
     * @param runnable         限流操作
     * @param fallbackRunnable 降级操作
     */
    public void execute(String key, RateLimitConfig config,
                        Runnable runnable, Runnable fallbackRunnable) {
        execute(key, config, () -> {
            runnable.run();
            return null;
        }, () -> {
            fallbackRunnable.run();
            return null;
        });
    }

    /**
     * 带超时的限流操作
     *
     * @param key      限流key
     * @param config   限流配置
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @param supplier 限流操作
     */
    public <T> T executeWithTimeout(String key, RateLimitConfig config,
                                    long timeout, TimeUnit timeUnit, Supplier<T> supplier) {
        return executeWithTimeout(key, config, timeout, timeUnit, supplier, () -> {
            throw new RateLimitExceededException("Rate limit wait timeout for key: " + key);
        });
    }

    /**
     * 带超时的限流操作，支持降级
     *
     * @param key              限流key
     * @param config           限流配置
     * @param timeout          超时时间
     * @param timeUnit         时间单位
     * @param supplier         限流操作
     * @param fallbackSupplier 降级操作
     */
    public <T> T executeWithTimeout(String key, RateLimitConfig config,
                                    long timeout, TimeUnit timeUnit,
                                    Supplier<T> supplier, Supplier<T> fallbackSupplier) {
        try {
            boolean acquired = redisRateLimiter.tryAcquire(key, 1, config, timeout, timeUnit);

            if (acquired) {
                return supplier.get();
            } else {
                return fallbackSupplier.get();
            }
        } catch (RateLimitExceededException e) {
            Thread.currentThread().interrupt();
            return fallbackSupplier.get();
        }
    }

    /**
     * 快速创建令牌桶配置
     *
     * @param capacity   容量
     * @param refillRate 补充速率
     * @param timeUnit   时间单位
     */
    public RateLimitConfig createTokenBucketConfig(int capacity, int refillRate, TimeUnit timeUnit) {
        return RateLimitConfig.tokenBucket(capacity, refillRate, timeUnit);
    }

    /**
     * 快速创建固定窗口配置
     *
     * @param windowSize  窗口大小，单位秒
     * @param maxRequests 最大请求数
     */
    public RateLimitConfig createFixedWindowConfig(int windowSize, int maxRequests) {
        return RateLimitConfig.fixedWindow(windowSize, maxRequests);
    }

    /**
     * 快速创建滑动窗口配置
     *
     * @param windowSize  窗口大小，单位秒
     * @param maxRequests 最大请求数
     */
    public RateLimitConfig createSlidingWindowConfig(int windowSize, int maxRequests) {
        return RateLimitConfig.slidingWindow(windowSize, maxRequests);
    }
}
