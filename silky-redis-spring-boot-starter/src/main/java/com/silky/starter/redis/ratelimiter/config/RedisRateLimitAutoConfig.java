package com.silky.starter.redis.ratelimiter.config;

import com.silky.starter.redis.ratelimiter.aspect.RateLimitAspect;
import com.silky.starter.redis.ratelimiter.properties.RedisRateLimitProperties;
import com.silky.starter.redis.ratelimiter.service.RedisRateLimiter;
import com.silky.starter.redis.ratelimiter.service.impl.DefaultRedisRateLimiterImpl;
import com.silky.starter.redis.ratelimiter.template.RedisRateLimitTemplate;
import com.silky.starter.redis.spel.SpelExpressionResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis限流配置类
 *
 * @author zy
 * @date 2025-10-23 10:39
 **/
@Configuration
@EnableConfigurationProperties({RedisRateLimitProperties.class})
public class RedisRateLimitAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public RedisRateLimiter redisRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        return new DefaultRedisRateLimiterImpl(redisTemplate);
    }

    @Bean
    public SpelExpressionResolver spelExpressionResolver() {
        return new SpelExpressionResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(RedisRateLimiter redisRateLimiter,
                                           SpelExpressionResolver spelExpressionResolver,
                                           RedisRateLimitProperties rateLimitProperties) {
        return new RateLimitAspect(redisRateLimiter, spelExpressionResolver, rateLimitProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisRateLimitTemplate rateLimitTemplate(RedisRateLimiter redisRateLimiter) {
        return new RedisRateLimitTemplate(redisRateLimiter);
    }
}
