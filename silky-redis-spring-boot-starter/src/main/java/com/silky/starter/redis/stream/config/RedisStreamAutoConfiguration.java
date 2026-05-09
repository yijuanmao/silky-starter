package com.silky.starter.redis.stream.config;

import com.silky.starter.redis.stream.consumer.RedisStreamAnnotationProcessor;
import com.silky.starter.redis.stream.core.MessageRepository;
import com.silky.starter.redis.stream.core.NoOpMessageRepository;
import com.silky.starter.redis.stream.producer.RedisMessageQueueTemplate;
import com.silky.starter.redis.stream.registry.MessageListenerContainerRegistry;
import com.silky.starter.redis.stream.support.StreamThreadPoolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Redis Stream 自动配置类
 * <p>
 * 仅当 {@code silky.redis.stream.enabled=true} 时生效，避免不必要的性能开销。
 * 用户可以通过注入自定义 Bean 来覆盖默认行为：
 * <ul>
 *   <li>自定义持久化：实现 {@link MessageRepository} 并注入 Spring 容器</li>
 *   <li>自定义线程池：注入名为 {@code silkyStreamExecutor} 的 {@link Executor} Bean</li>
 * </ul>
 * </p>
 *
 * @author zy
 * @date 2026-05-09
 */
@Configuration
@EnableConfigurationProperties(RedisStreamProperties.class)
@ConditionalOnProperty(prefix = "silky.redis.stream", name = "enabled", havingValue = "true")
public class RedisStreamAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamAutoConfiguration.class);

    /**
     * 消息仓库（持久化）默认空实现
     * 若用户已实现 {@link MessageRepository}，此 Bean 不会注入
     */
    @Bean
    @ConditionalOnMissingBean(MessageRepository.class)
    public MessageRepository noOpMessageRepository() {
        log.debug("[RedisStream] 未检测到 MessageRepository 实现，使用 NoOpMessageRepository（不做持久化）");
        return new NoOpMessageRepository();
    }

    /**
     * 消费者线程池（默认使用 ThreadPoolTaskExecutor）
     * 若用户已提供名为 silkyStreamExecutor 的 Executor Bean，使用用户自定义的
     */
    @Bean("silkyStreamExecutor")
    @ConditionalOnMissingBean(name = "silkyStreamExecutor")
    public ThreadPoolTaskExecutor silkyStreamExecutor(RedisStreamProperties properties) {
        return StreamThreadPoolFactory.create(properties.getConsumer().getThreadPool());
    }

    /**
     * 容器注册器
     */
    @Bean
    public MessageListenerContainerRegistry messageListenerContainerRegistry(
            StringRedisTemplate stringRedisTemplate,
            MessageRepository messageRepository,
            RedisStreamProperties properties,
            @Qualifier("silkyStreamExecutor") Executor executor) {
        return new MessageListenerContainerRegistry(stringRedisTemplate, messageRepository, properties, executor);
    }

    /**
     * 生产者模板
     */
    @Bean
    @ConditionalOnMissingBean(RedisMessageQueueTemplate.class)
    public RedisMessageQueueTemplate redisMessageQueueTemplate(
            StringRedisTemplate stringRedisTemplate,
            MessageRepository messageRepository) {
        log.info("[RedisStream] RedisMessageQueueTemplate 初始化完成");
        return new RedisMessageQueueTemplate(stringRedisTemplate, messageRepository);
    }

    /**
     * 注解驱动处理器（扫描 @RedisStreamListener）
     */
    @Bean
    public RedisStreamAnnotationProcessor redisStreamAnnotationProcessor(
            MessageListenerContainerRegistry registry,
            RedisStreamProperties properties) {
        return new RedisStreamAnnotationProcessor(registry, properties);
    }
}
