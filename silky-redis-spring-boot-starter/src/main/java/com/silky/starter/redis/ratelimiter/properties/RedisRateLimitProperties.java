package com.silky.starter.redis.ratelimiter.properties;

import com.silky.starter.redis.ratelimiter.enums.RateLimitAlgorithm;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 消息发送配置属性
 *
 * @author zy
 * @date 2025-10-09 18:07
 **/
@Data
@ConfigurationProperties(prefix = RedisRateLimitProperties.REDIS_PREFIX)
public class RedisRateLimitProperties {

    public static final String REDIS_PREFIX = "spring.redis.rate-limit";

    /**
     * 限流策略配置映射
     */
    private Map<String, StrategyConfig> strategies = new HashMap<>();

    /**
     * 默认限流策略配置
     */
    private StrategyConfig defaultStrategy = new StrategyConfig();


    /**
     * 根据策略名称获取配置
     */
    public StrategyConfig getStrategy(String name) {
        return strategies.getOrDefault(name, defaultStrategy);
    }

    /**
     * 检查策略是否存在
     */
    public boolean hasStrategy(String name) {
        return strategies.containsKey(name);
    }

    /**
     * 限流策略配置类
     */
    @Setter
    @Getter
    public static class StrategyConfig {

        /**
         * 限流算法
         */
        private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;

        /**
         * 令牌桶限流配置
         */
        private int capacity = 100;

        /**
         * 令牌桶填充速率
         */
        private int refillRate = 10;

        /**
         * 令牌桶算法时间单位
         */
        private TimeUnit timeUnit = TimeUnit.SECONDS;

        /**
         * 固定窗口限流配置
         */
        private int windowSize = 60;

        /**
         * 固定窗口限流最大请求数
         */
        private int maxRequests = 100;

        /**
         * 是否启用该策略
         */
        private boolean enabled = true;

    }

}
