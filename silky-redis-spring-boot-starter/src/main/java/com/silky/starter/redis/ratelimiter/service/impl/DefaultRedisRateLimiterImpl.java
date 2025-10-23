package com.silky.starter.redis.ratelimiter.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import com.silky.starter.redis.ratelimiter.annotation.RateLimit;
import com.silky.starter.redis.ratelimiter.config.RateLimitConfig;
import com.silky.starter.redis.ratelimiter.exception.RateLimitExceededException;
import com.silky.starter.redis.ratelimiter.service.RedisRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

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

    // 令牌桶算法Lua脚本
    private static final String TOKEN_BUCKET_SCRIPT =
            "local key = KEYS[1]\n" +
                    "local capacity = tonumber(ARGV[1])\n" +
                    "local refill_rate = tonumber(ARGV[2])\n" +
                    "local refill_time_unit = tonumber(ARGV[3])\n" +
                    "local permits = tonumber(ARGV[4])\n" +
                    "local now = tonumber(ARGV[5])\n" +
                    "\n" +
                    "local bucket = redis.call('HMGET', key, 'tokens', 'last_refill_time')\n" +
                    "local tokens = capacity\n" +
                    "local last_refill_time = now\n" +
                    "\n" +
                    "if bucket[1] then\n" +
                    "    tokens = tonumber(bucket[1])\n" +
                    "    last_refill_time = tonumber(bucket[2])\n" +
                    "    \n" +
                    "    -- 计算需要补充的令牌数\n" +
                    "    local time_passed = now - last_refill_time\n" +
                    "    local tokens_to_add = math.floor(time_passed * refill_rate / refill_time_unit)\n" +
                    "    \n" +
                    "    if tokens_to_add > 0 then\n" +
                    "        tokens = math.min(capacity, tokens + tokens_to_add)\n" +
                    "        last_refill_time = now\n" +
                    "    end\n" +
                    "end\n" +
                    "\n" +
                    "if tokens >= permits then\n" +
                    "    tokens = tokens - permits\n" +
                    "    redis.call('HMSET', key, 'tokens', tokens, 'last_refill_time', last_refill_time)\n" +
                    "    redis.call('EXPIRE', key, math.ceil(capacity / refill_rate * refill_time_unit) * 2)\n" +
                    "    return 1\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";

    // 固定窗口算法Lua脚本
    private static final String FIXED_WINDOW_SCRIPT =
            "local key = KEYS[1]\n" +
                    "local window_size = tonumber(ARGV[1])\n" +
                    "local max_requests = tonumber(ARGV[2])\n" +
                    "local permits = tonumber(ARGV[3])\n" +
                    "\n" +
                    "local current = redis.call('GET', key)\n" +
                    "if current and tonumber(current) >= max_requests then\n" +
                    "    return 0\n" +
                    "else\n" +
                    "    if not current then\n" +
                    "        redis.call('SETEX', key, window_size, permits)\n" +
                    "    else\n" +
                    "        redis.call('INCRBY', key, permits)\n" +
                    "    end\n" +
                    "    return 1\n" +
                    "end";

    // 滑动窗口算法Lua脚本
    private static final String SLIDING_WINDOW_SCRIPT =
            "local key = KEYS[1]\n" +
                    "local window_size = tonumber(ARGV[1])\n" +
                    "local max_requests = tonumber(ARGV[2])\n" +
                    "local permits = tonumber(ARGV[3])\n" +
                    "local now = tonumber(ARGV[4])\n" +
                    "local clear_before = now - window_size\n" +
                    "\n" +
                    "redis.call('ZREMRANGEBYSCORE', key, 0, clear_before)\n" +
                    "local current = redis.call('ZCOUNT', key, clear_before + 1, now)\n" +
                    "\n" +
                    "if current + permits > max_requests then\n" +
                    "    return 0\n" +
                    "else\n" +
                    "    for i = 1, permits do\n" +
                    "        redis.call('ZADD', key, now, now .. ':' .. math.random(1000000))\n" +
                    "    end\n" +
                    "    redis.call('EXPIRE', key, window_size)\n" +
                    "    return 1\n" +
                    "end";

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedisScript<Long> tokenBucketScript;

    private final RedisScript<Long> fixedWindowScript;

    private final RedisScript<Long> slidingWindowScript;

    public DefaultRedisRateLimiterImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, Long.class);
        this.fixedWindowScript = new DefaultRedisScript<>(FIXED_WINDOW_SCRIPT, Long.class);
        this.slidingWindowScript = new DefaultRedisScript<>(SLIDING_WINDOW_SCRIPT, Long.class);
    }

    /**
     * 获取令牌，获取不到则阻塞等待
     *
     * @param key 限流key
     */
    @Override
    public boolean acquire(String key) throws RateLimitExceededException {
        return tryAcquire(key, 30, TimeUnit.SECONDS);
    }

    /**
     * 获取指定数量的令牌，获取不到则阻塞等待
     *
     * @param key     限流key
     * @param permits 令牌数量
     */
    @Override
    public boolean acquire(String key, int permits) throws RateLimitExceededException {
        return tryAcquire(key, permits, 30, TimeUnit.SECONDS);
    }

    /**
     * 尝试获取令牌
     *
     * @param key 限流key
     */
    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1);
    }

    /**
     * 尝试获取指定数量的令牌
     *
     * @param key     限流key
     * @param permits 令牌数量
     */
    @Override
    public boolean tryAcquire(String key, int permits) {
        // 默认使用令牌桶算法，容量100，每秒10个令牌
        return tryAcquire(key, permits, RateLimitConfig.defaultConfig());
    }

    /**
     * 尝试获取指定数量的令牌（非阻塞）
     *
     * @param key     限流key
     * @param permits 令牌数量
     * @param config  限流配置
     */
    public boolean tryAcquire(String key, int permits, RateLimitConfig config) {
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

        return result != null && result == 1;
    }

    /**
     * 尝试获取令牌（非阻塞）
     *
     * @param key       限流key
     * @param rateLimit 限流注解配置
     */
    public boolean tryAcquire(String key, RateLimit rateLimit) throws RateLimitExceededException{
        RateLimitConfig config = convertToRateLimitConfig(rateLimit);
        return tryAcquire(key, 1, config);
    }

    /**
     * 获取指定数量的令牌，获取不到则阻塞等待
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
     * 获取指定数量的令牌，获取不到则阻塞等待
     *
     * @param key     限流key
     * @param permits 令牌数量
     * @param timeout 等待超时时间
     * @param unit    时间单位
     */
    @Override
    public boolean tryAcquire(String key, int permits, long timeout, TimeUnit unit) throws RateLimitExceededException {
        long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < endTime) {
            if (tryAcquire(key, permits)) {
                return true;
            }
            // 等待100ms后重试
            ThreadUtil.sleep(100);
        }
        return false;
    }

    /**
     * 获取指定数量的令牌，获取不到则阻塞等待
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
        long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < endTime) {
            if (tryAcquire(key, permits, config)) {
                return true;
            }
            ThreadUtil.sleep(100);
        }
        return false;
    }

    /**
     * 获取令牌，获取不到则阻塞等待
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
        redisTemplate.delete(key);
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
     * 将注解配置转换为内部配置
     *
     * @param rateLimit 限流注解
     * @return RateLimitConfig
     */
    private RateLimitConfig convertToRateLimitConfig(RateLimit rateLimit) {
        RateLimitConfig config = new RateLimitConfig();
        config.setAlgorithm(rateLimit.algorithm());
        config.setCapacity(rateLimit.capacity());
        config.setRefillRate(rateLimit.refillRate());
        config.setTimeUnit(rateLimit.timeUnit());
        config.setWindowSize(rateLimit.windowSize());
        config.setMaxRequests(rateLimit.maxRequests());
        return config;
    }


}
