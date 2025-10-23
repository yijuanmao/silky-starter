package com.silky.starter.redis.ratelimiter.config;

import com.silky.starter.redis.ratelimiter.enums.RateLimitAlgorithm;
import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * 限流配置类
 *
 * @author zy
 * @date 2025-10-23 11:20
 **/
@Data
public class RateLimitConfig {

    /**
     * * 限流算法类型 默认令牌桶算法
     */
    private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;

    /**
     * * 令牌桶算法配置
     */
    private int capacity = 100;

    /**
     * * 令牌桶算法配置
     */
    private int refillRate = 10;

    /**
     * 窗口大小（秒） 默认60秒
     */
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    /**
     * 窗口大小（秒） 默认60秒
     */
    private int windowSize = 60;

    /**
     * 最大请求数 默认100
     */
    private int maxRequests = 100;

    /**
     * 创建默认限流配置，使用令牌桶算法
     *
     * @return RateLimitConfig
     */
    public static RateLimitConfig defaultConfig() {
        return new RateLimitConfig();
    }


    /**
     * 创建令牌桶限流配置
     *
     * @param capacity   桶容量
     * @param refillRate 补充速率
     * @param timeUnit   时间单位
     * @return RateLimitConfig
     */
    public static RateLimitConfig tokenBucket(int capacity, int refillRate, TimeUnit timeUnit) {
        RateLimitConfig config = new RateLimitConfig();
        config.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        config.setCapacity(capacity);
        config.setRefillRate(refillRate);
        config.setTimeUnit(timeUnit);
        return config;
    }


    /**
     * 创建固定窗口限流配置
     *
     * @param windowSize  窗口大小
     * @param maxRequests 最大请求数
     * @return RateLimitConfig
     */
    public static RateLimitConfig fixedWindow(int windowSize, int maxRequests) {
        RateLimitConfig config = new RateLimitConfig();
        config.setAlgorithm(RateLimitAlgorithm.FIXED_WINDOW);
        config.setWindowSize(windowSize);
        config.setMaxRequests(maxRequests);
        return config;
    }


    /**
     * 创建滑动窗口限流配置
     *
     * @param windowSize  窗口大小
     * @param maxRequests 最大请求数
     * @return RateLimitConfig
     */
    public static RateLimitConfig slidingWindow(int windowSize, int maxRequests) {
        RateLimitConfig config = new RateLimitConfig();
        config.setAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW);
        config.setWindowSize(windowSize);
        config.setMaxRequests(maxRequests);
        return config;
    }

}
