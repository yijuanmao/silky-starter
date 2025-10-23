package com.silky.starter.redis.ratelimiter.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import com.silky.starter.redis.ratelimiter.annotation.RateLimit;
import com.silky.starter.redis.ratelimiter.config.RateLimitConfig;
import com.silky.starter.redis.ratelimiter.exception.RateLimitExceededException;
import com.silky.starter.redis.ratelimiter.service.RedisRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis限流器默认实现
 *
 * @author zy
 * @date 2025-10-23 10:38
 **/
@Slf4j
public class DefaultRedisRateLimiterImpl implements RedisRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedisScript<Long> tokenBucketScript;

    private final RedisScript<Long> fixedWindowScript;

    private final RedisScript<Long> slidingWindowScript;

    public DefaultRedisRateLimiterImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;

        // 从Resource目录加载Lua脚本
        this.tokenBucketScript = loadLuaScript("lua/token_bucket.lua");
        this.fixedWindowScript = loadLuaScript("lua/fixed_window.lua");
        this.slidingWindowScript = loadLuaScript("lua/sliding_window.lua");
    }

    /**
     * 获取令牌，获取不到则阻塞等待（默认30秒超时）
     *
     * @param key 限流key
     */
    @Override
    public boolean acquire(String key) throws RateLimitExceededException {
        return this.acquire(key, 1);
    }

    /**
     * 获取指定数量的令牌，获取不到则阻塞等待（默认30秒超时）
     *
     * @param key     限流key
     * @param permits 令牌数量
     */
    @Override
    public boolean acquire(String key, int permits) throws RateLimitExceededException {
        return this.tryAcquire(key, permits, 30, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取令牌（非阻塞）
     *
     * @param key 限流key
     */
    @Override
    public boolean tryAcquire(String key) {
        return this.tryAcquire(key, 1);
    }

    /**
     * 尝试获取指定数量的令牌（非阻塞）
     *
     * @param key     限流key
     * @param permits 令牌数量
     */
    @Override
    public boolean tryAcquire(String key, int permits) {
        // 默认使用令牌桶算法，容量100，每秒10个令牌
        return this.tryAcquire(key, permits, RateLimitConfig.defaultConfig());
    }

    /**
     * 尝试获取令牌（非阻塞）
     *
     * @param key       限流key
     * @param rateLimit 限流注解配置
     */
    public boolean tryAcquire(String key, RateLimit rateLimit) throws RateLimitExceededException {
        RateLimitConfig config = RateLimitConfig.buideRateLimitConfig(rateLimit);
        return this.tryAcquire(key, 1, config);
    }

    /**
     * 尝试获取令牌（带超时）
     *
     * @param key     限流key
     * @param timeout 等待超时时间
     * @param unit    时间单位
     */
    @Override
    public boolean tryAcquire(String key, long timeout, TimeUnit unit) throws RateLimitExceededException {
        return tryAcquire(key, 1, timeout, unit);
    }

    /**
     * 尝试获取指定数量的令牌（带超时）
     *
     * @param key     限流key
     * @param permits 令牌数量
     * @param timeout 等待超时时间
     * @param unit    时间单位
     */
    @Override
    public boolean tryAcquire(String key, int permits, long timeout, TimeUnit unit) throws RateLimitExceededException {
        // 使用默认配置
        return tryAcquire(key, permits, RateLimitConfig.defaultConfig(), timeout, unit);
    }

    /**
     * 尝试获取指定数量的令牌（非阻塞）
     *
     * @param key     限流key
     * @param permits 令牌数量
     * @param config  限流配置
     */
    public boolean tryAcquire(String key, int permits, RateLimitConfig config) {
        long startTime = System.currentTimeMillis();
        try {
            List<String> keys = Collections.singletonList(key);
            // 当前时间戳（秒）
            long now = System.currentTimeMillis() / 1000;

            Long result;
            switch (config.getAlgorithm()) {
                case TOKEN_BUCKET:
                    long refillTimeUnit = convertTimeUnitToSeconds(config.getTimeUnit());
                    Object[] tokenBucketArgs = {
                            config.getCapacity(),
                            config.getRefillRate(),
                            refillTimeUnit,
                            permits,
                            now
                    };
                    result = redisTemplate.execute(tokenBucketScript, keys, tokenBucketArgs);
                    break;

                case FIXED_WINDOW:
                    Object[] fixedWindowArgs = {
                            config.getWindowSize(),
                            config.getMaxRequests(),
                            permits
                    };
                    result = redisTemplate.execute(fixedWindowScript, keys, fixedWindowArgs);
                    break;

                case SLIDING_WINDOW:
                    Object[] slidingWindowArgs = {
                            config.getWindowSize(),
                            config.getMaxRequests(),
                            permits,
                            now
                    };
                    result = redisTemplate.execute(slidingWindowScript, keys, slidingWindowArgs);
                    break;

                default:
                    throw new RateLimitExceededException("Unsupported rate limit algorithm: " + config.getAlgorithm());
            }

            boolean acquired = result != null && result == 1;
            long costTime = System.currentTimeMillis() - startTime;

            if (log.isDebugEnabled()) {
                log.debug("Rate limit tryAcquire - key: {}, permits: {}, algorithm: {}, acquired: {}, cost: {}ms",
                        key, permits, config.getAlgorithm(), acquired, costTime);
            }

            return acquired;

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            log.error("Rate limit tryAcquire failed - key: {}, permits: {}, cost: {}ms, error: {}",
                    key, permits, costTime, e.getMessage(), e);
            throw new RateLimitExceededException("Rate limit acquire failed: " + e.getMessage(), e);
        }
    }

    /**
     * 尝试获取指定数量的令牌（带超时和配置）
     *
     * @param key     限流key
     * @param permits 令牌数量
     * @param config  限流配置
     * @param timeout 等待超时时间
     * @param unit    时间单位
     */
    @Override
    public boolean tryAcquire(String key, int permits, RateLimitConfig config, long timeout, TimeUnit unit)
            throws RateLimitExceededException {

        long timeoutMillis = unit.toMillis(timeout);
        long startTime = System.currentTimeMillis();
        long remainingTime = timeoutMillis;
        int attemptCount = 0;

        try {
            // 在超时时间内循环尝试获取令牌
            while (remainingTime > 0) {
                attemptCount++;

                // 尝试获取令牌
                if (tryAcquire(key, permits, config)) {
                    long totalWaitTime = System.currentTimeMillis() - startTime;
                    if (log.isDebugEnabled()) {
                        log.debug("Rate limit acquired successfully - key: {}, attempts: {}, totalWait: {}ms",
                                key, attemptCount, totalWaitTime);
                    }
                    return true;
                }

                // 计算已经过去的时间
                long elapsedTime = System.currentTimeMillis() - startTime;
                remainingTime = timeoutMillis - elapsedTime;

                if (remainingTime <= 0) {
                    break; // 超时了
                }

                // 使用退避策略：随着重试次数增加，等待时间逐渐变长
                long sleepTime = calculateBackoffTime(attemptCount, remainingTime);
                if (sleepTime > 0) {
                    if (log.isTraceEnabled()) {
                        log.trace("Rate limit waiting - key: {}, attempt: {}, sleep: {}ms, remaining: {}ms",
                                key, attemptCount, sleepTime, remainingTime);
                    }
                    ThreadUtil.sleep(sleepTime);
                }

                // 更新已用时间
                elapsedTime = System.currentTimeMillis() - startTime;
                remainingTime = timeoutMillis - elapsedTime;
            }

            // 超时前的最后一次尝试
            boolean finalAttempt = tryAcquire(key, permits, config);
            long totalWaitTime = System.currentTimeMillis() - startTime;

            if (!finalAttempt) {
                log.warn("Rate limit timeout - key: {}, attempts: {}, totalWait: {}ms, timeout: {}ms", key, attemptCount, totalWaitTime, timeoutMillis);
            } else {
                log.debug("Rate limit final attempt success - key: {}, attempts: {}, totalWait: {}ms", key, attemptCount, totalWaitTime);
            }
            return finalAttempt;

        } catch (Exception e) {
            long waitTime = System.currentTimeMillis() - startTime;
            log.error("Rate limit wait failed - key: {}, waitTime: {}ms, error: {}",
                    key, waitTime, e.getMessage(), e);
            throw new RateLimitExceededException("Rate limit wait failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取限流配置
     *
     * @param key 限流key
     */
    @Override
    public RateLimitConfig getConfig(String key) {
        // 简化实现，实际应该从Redis或配置中心获取
        // 这里返回默认配置
        return RateLimitConfig.defaultConfig();
    }

    /**
     * 重置限流器
     *
     * @param key 限流key
     */
    @Override
    public void reset(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Rate limit reset - key: {}", key);
        } catch (Exception e) {
            log.error("Rate limit reset failed - key: {}, error: {}", key, e.getMessage(), e);
            throw new RateLimitExceededException("Rate limit reset failed: " + e.getMessage(), e);
        }
    }

    /**
     * 计算退避等待时间
     *
     * @param attemptCount  尝试次数
     * @param remainingTime 剩余时间
     * @return 等待时间(毫秒)
     */
    private long calculateBackoffTime(int attemptCount, long remainingTime) {
        // 基础等待时间
        long baseWaitTime = 100L;

        // 指数退避：随着尝试次数增加，等待时间变长，但不超过剩余时间
        long backoffTime = Math.min(baseWaitTime * attemptCount, 1000L); // 最大等待1秒

        // 确保不超过剩余时间
        return Math.min(backoffTime, remainingTime);
    }

    /**
     * 将TimeUnit转换为秒数
     */
    private long convertTimeUnitToSeconds(TimeUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS:
                return 1;
            case MINUTES:
                return 60;
            case HOURS:
                return 3600;
            case DAYS:
                return 86400;
            default:
                return 1;
        }
    }

    /**
     * 从classpath加载Lua脚本文件
     */
    private RedisScript<Long> loadLuaScript(String scriptPath) {
        try {
            ClassPathResource resource = new ClassPathResource(scriptPath);
            String scriptContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(scriptContent);
            redisScript.setResultType(Long.class);

            log.debug("Successfully loaded Lua script: {}", scriptPath);
            return redisScript;
        } catch (Exception e) {
            log.error("Failed to load Lua script from: {}", scriptPath, e);
            throw new RateLimitExceededException("Failed to load Lua script: " + scriptPath, e);
        }
    }

}
