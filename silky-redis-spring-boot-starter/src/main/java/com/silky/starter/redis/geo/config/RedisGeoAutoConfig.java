package com.silky.starter.redis.geo.config;

import com.silky.starter.redis.geo.properties.GeoProperties;
import com.silky.starter.redis.geo.template.RedisGeoTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis Geo 自动配置类
 * <p>
 * 使用 {@link StringRedisTemplate} 代替 {@link org.springframework.data.redis.core.RedisTemplate}
 * 执行 Geo 操作，避免 FastJson2RedisSerializer 对 Geo 反序列化结果的干扰。
 *
 * @author zy
 * @date 2025-10-22 11:33
 **/
@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties(GeoProperties.class)
public class RedisGeoAutoConfig {

    @Bean
    public RedisGeoTemplate redisGeoTemplate(StringRedisTemplate stringRedisTemplate,
                                             GeoProperties geoProperties) {
        return new RedisGeoTemplate(stringRedisTemplate, geoProperties.getCoordinateSystem());
    }
}
