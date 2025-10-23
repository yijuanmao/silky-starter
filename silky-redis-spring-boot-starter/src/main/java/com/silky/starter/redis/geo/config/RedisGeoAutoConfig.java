package com.silky.starter.redis.geo.config;

import com.silky.starter.redis.geo.template.RedisGeoTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * redis geo配置类
 *
 * @author zy
 * @date 2025-10-22 11:33
 **/
@Configuration
public class RedisGeoAutoConfig {

    @Bean
    public RedisGeoTemplate redisGeoTemplate(RedisTemplate<String, Object> redisTemplate) {
        return new RedisGeoTemplate(redisTemplate);
    }
}
