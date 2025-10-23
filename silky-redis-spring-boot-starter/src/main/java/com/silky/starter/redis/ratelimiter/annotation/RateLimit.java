package com.silky.starter.redis.ratelimiter.annotation;


import com.silky.starter.redis.ratelimiter.enums.RateLimitAlgorithm;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 *
 * @author zy
 * @date 2025-10-23 10:32
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流key，支持SpEL表达式
     */
    String key();

    /**
     * 限流算法，默认令牌桶
     */
    RateLimitAlgorithm algorithm() default RateLimitAlgorithm.TOKEN_BUCKET;

    /**
     * 令牌桶容量（令牌桶算法专用）
     */
    int capacity() default 100;

    /**
     * 令牌填充速率（令牌桶算法专用）
     */
    int refillRate() default 10;

    /**
     * 时间单位（令牌桶算法专用）
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 时间窗口大小，单位秒（固定/滑动窗口算法专用）
     */
    int windowSize() default 60;

    /**
     * 最大请求数（固定/滑动窗口算法专用）
     */
    int maxRequests() default 100;

    /**
     * 限流后的降级方法名，必须和目标方法在同一个类中
     */
    String fallbackMethod() default "";

    /**
     * 是否阻塞等待令牌
     */
    boolean block() default false;

    /**
     * 阻塞等待超时时间（秒）
     */
    long timeout() default 0;
}
