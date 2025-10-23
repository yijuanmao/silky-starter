package com.silky.starter.redis.cache.config;

import com.silky.starter.redis.cache.serializer.FastJson2RedisSerializer;
import com.silky.starter.redis.cache.template.RedisCacheTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redis缓存配置类
 *
 * @author zy
 * @date 2025-10-21 15:29
 **/
@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheTemplate redisCacheTemplate(RedisTemplate<String, Object> template) {
        return new RedisCacheTemplate(template);
    }
}
