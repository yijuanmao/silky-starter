package com.silky.starter.redis.config;

import com.silky.starter.redis.cache.config.RedisCacheConfig;
import com.silky.starter.redis.geo.config.RedisGeoAutoConfig;
import com.silky.starter.redis.lock.config.RedisLockAutoConfig;
import com.silky.starter.redis.ratelimiter.config.RedisRateLimitAutoConfig;
import com.silky.starter.redis.sequence.config.RedisSequenceAutoConfig;
import com.silky.starter.redis.spel.SpelExpressionResolver;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * redis自动配置类
 *
 * @author zy
 * @date 2025-10-21 15:01
 **/
@Configuration
@Import({
        RedisCacheConfig.class,
        RedisLockAutoConfig.class,
        RedisGeoAutoConfig.class,
        RedisSequenceAutoConfig.class,
        RedisRateLimitAutoConfig.class
})
@ConditionalOnClass(RedisTemplate.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisComponentAutoConfiguration {

    @Bean
    public SpelExpressionResolver ppelExpressionResolver() {
        return new SpelExpressionResolver();
    }
}
