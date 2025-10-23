package com.silky.starter.redis.ratelimiter.aspect;

import com.silky.starter.redis.ratelimiter.annotation.RateLimit;
import com.silky.starter.redis.ratelimiter.config.RateLimitConfig;
import com.silky.starter.redis.ratelimiter.exception.RateLimitExceededException;
import com.silky.starter.redis.ratelimiter.service.RedisRateLimiter;
import com.silky.starter.redis.spel.SpelExpressionResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Redis限流切面
 *
 * @author zy
 * @date 2025-10-23 10:52
 **/
@Aspect
@Order(1)
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RedisRateLimiter redisRateLimiter;
    private final SpelExpressionResolver spelExpressionResolver;

    public RateLimitAspect(RedisRateLimiter redisRateLimiter, SpelExpressionResolver spelExpressionResolver) {
        this.redisRateLimiter = redisRateLimiter;
        this.spelExpressionResolver = spelExpressionResolver;
    }


    /**
     * 环绕通知，处理限流逻辑
     *
     * @param joinPoint 切点
     * @param rateLimit 限流注解
     * @return 方法执行结果
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = resolveKey(joinPoint, rateLimit);

        boolean acquired;

        if (rateLimit.block()) {
            if (rateLimit.timeout() > 0) {
                try {
                    // 带超时的阻塞获取
                    acquired = redisRateLimiter.tryAcquire(
                            key,
                            1, // 默认获取1个令牌
                            RateLimitConfig.buideRateLimitConfig(rateLimit),
                            rateLimit.timeout(),
                            TimeUnit.SECONDS
                    );
                    log.debug("Blocking acquire with timeout {}s, key: {}, result: {}", rateLimit.timeout(), key, acquired);
                } catch (RateLimitExceededException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Rate limit wait interrupted, key: {}", key);
                    return executeFallbackMethod(joinPoint, rateLimit.fallbackMethod());
                }
            } else {
                // 无限等待的阻塞获取（不推荐生产环境使用）
                try {
                    acquired = redisRateLimiter.acquire(key);
                    log.debug("Blocking acquire without timeout, key: {}, result: {}", key, acquired);
                } catch (RateLimitExceededException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Rate limit wait interrupted, key: {}", key);
                    return executeFallbackMethod(joinPoint, rateLimit.fallbackMethod());
                }
            }
        } else {
            // 非阻塞获取令牌
            acquired = redisRateLimiter.tryAcquire(key, rateLimit);
            log.debug("Non-blocking acquire, key: {}, result: {}", key, acquired);
        }
        if (!acquired) {
            log.warn("Rate limit exceeded, key: {}", key);

            if (!rateLimit.fallbackMethod().isEmpty()) {
                return executeFallbackMethod(joinPoint, rateLimit.fallbackMethod());
            } else {
                throw new RateLimitExceededException("Rate limit exceeded for key: " + key);
            }
        }
        log.debug("Rate limit passed, key: {}", key);
        return joinPoint.proceed();
    }

    /**
     * 解析限流key
     *
     * @param joinPoint 切点
     * @param rateLimit 限流注解
     * @return 限流key
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        return spelExpressionResolver.resolve(rateLimit.key(), joinPoint);
    }


    /**
     * 执行降级方法
     *
     * @param joinPoint      切点
     * @param fallbackMethod 降级方法名
     * @return 降级方法执行结果
     * @throws Throwable 异常
     */
    private Object executeFallbackMethod(ProceedingJoinPoint joinPoint, String fallbackMethod) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object target = joinPoint.getTarget();

        try {
            Method fallback = target.getClass().getMethod(fallbackMethod, method.getParameterTypes());
            return fallback.invoke(target, joinPoint.getArgs());
        } catch (NoSuchMethodException e) {
            log.error("Fallback method not found: {}", fallbackMethod);
            throw new RateLimitExceededException("Fallback method not found: " + fallbackMethod);
        }
    }

}
