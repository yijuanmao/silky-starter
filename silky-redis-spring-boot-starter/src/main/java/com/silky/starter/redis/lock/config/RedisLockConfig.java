package com.silky.starter.redis.lock.config;

import com.silky.starter.redis.lock.template.RedisLockTemplate;
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
public class RedisLockConfig {

    @Bean
    public RedisLockTemplate redisLockTemplate(RedissonClient redissonClient) {
        return new RedisLockTemplate(redissonClient);
    }
}
