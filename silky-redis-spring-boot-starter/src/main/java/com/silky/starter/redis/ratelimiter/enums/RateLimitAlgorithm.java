package com.silky.starter.redis.ratelimiter.enums;

/**
 * 限流算法类型
 *
 * @author zy
 * @date 2025-10-23 10:32
 **/
public enum RateLimitAlgorithm {

    /**
     * 令牌桶算法 - 平滑限流，允许突发流量
     */
    TOKEN_BUCKET,

    /**
     * 固定窗口算法 - 简单计数器限流
     */
    FIXED_WINDOW,

    /**
     * 滑动窗口算法 - 精准限流，避免临界问题
     */
    SLIDING_WINDOW
}
