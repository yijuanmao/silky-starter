package com.silky.starter.redis.lock.config;

import com.silky.starter.redis.lock.aspect.RedisLockAspect;
import com.silky.starter.redis.lock.template.RedisLockTemplate;
import com.silky.starter.redis.spel.SpelExpressionResolver;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redis分布式锁配置类
 *
 * @author zy
 * @date 2025-10-22 11:30
 **/
@Configuration
public class RedisLockAutoConfig {

    @Bean
    public RedisLockTemplate redisLockTemplate(RedissonClient redissonClient) {
        return new RedisLockTemplate(redissonClient);
    }
    @Bean
    public RedisLockAspect redisLockAspect(RedissonClient redissonClient, SpelExpressionResolver ppelExpressionResolver) {
        return new RedisLockAspect(redissonClient, ppelExpressionResolver);
    }
}
