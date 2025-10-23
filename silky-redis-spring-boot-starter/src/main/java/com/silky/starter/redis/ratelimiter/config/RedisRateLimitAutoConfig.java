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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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
    public RedisRateLimiter redisRateLimiter(RedisConnectionFactory redisConnectionFactory) {
        // 创建专门用于限流器的RedisTemplate
        RedisTemplate<String, Object> redisTemplate = createRateLimitRedisTemplate(redisConnectionFactory);
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

    /**
     * 限流器使用的RedisTemplate
     *
     * @param connectionFactory Redis连接工厂
     * @return 限流器专用的RedisTemplate
     */
    private RedisTemplate<String, Object> createRateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 对于限流器，我们使用String序列化器，因为Lua脚本需要字符串参数
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        template.setDefaultSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
