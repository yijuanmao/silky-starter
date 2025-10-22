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
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用FastJson2序列化值
        FastJson2RedisSerializer<Object> serializer = new FastJson2RedisSerializer<>(Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisCacheTemplate redisCacheTemplate(RedisTemplate<String, Object> template) {
        return new RedisCacheTemplate(template);
    }
}
