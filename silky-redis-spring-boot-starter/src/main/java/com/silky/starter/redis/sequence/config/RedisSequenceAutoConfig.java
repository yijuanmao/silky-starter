package com.silky.starter.redis.sequence.config;

import com.silky.starter.redis.sequence.RedisSequenceGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * redis缓存配置类
 *
 * @author zy
 * @date 2025-10-21 15:29
 **/
@Configuration
public class RedisSequenceAutoConfig {

    @Bean
    public RedisSequenceGenerator redisSequenceGenerator(RedisTemplate<String, Object> template) {
        return new RedisSequenceGenerator(template);
    }
}
