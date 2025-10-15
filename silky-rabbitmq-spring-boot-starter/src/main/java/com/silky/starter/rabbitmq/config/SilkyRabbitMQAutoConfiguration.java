package com.silky.starter.rabbitmq.config;

import com.silky.starter.rabbitmq.aop.RabbitMessageAspect;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.persistence.NoOpMessagePersistenceService;
import com.silky.starter.rabbitmq.properties.SilkyRabbitMQProperties;
import com.silky.starter.rabbitmq.serialization.FastJson2MessageSerializer;
import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
import com.silky.starter.rabbitmq.template.DefaultRabbitSendTemplate;
import com.silky.starter.rabbitmq.template.RabbitSendTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * RabbitMQ自动配置（使用Fastjson2序列化）
 *
 * @author zy
 * @date 2025-10-12 10:14
 **/
@Configuration
@EnableConfigurationProperties(SilkyRabbitMQProperties.class)
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "spring.rabbitmq.silky", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SilkyRabbitMQAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SilkyRabbitMQAutoConfiguration.class);

    private final SilkyRabbitMQProperties properties;
    private MessagePersistenceService persistenceService;

    public SilkyRabbitMQAutoConfiguration(SilkyRabbitMQProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, SilkyRabbitMQProperties properties) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // 配置超时设置
        if (properties.getSend().isUseTimeout()) {
            rabbitTemplate.setReplyTimeout(properties.getSend().getSyncTimeout());
        }
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitMqMessageSerializer fastJson2MessageSerializer() {
        return new FastJson2MessageSerializer();
    }

    /**
     * 默认的空持久化服务
     * 只有当没有其他 MessagePersistenceService 实现时才创建
     */
    @Bean
    @ConditionalOnMissingBean(MessagePersistenceService.class)
    public MessagePersistenceService messagePersistenceService() {
        NoOpMessagePersistenceService service = new NoOpMessagePersistenceService();
        this.persistenceService = service;
        return service;
    }

    @Bean
    @ConditionalOnMissingBean(RabbitSendTemplate.class)
    @ConditionalOnBean({RabbitTemplate.class, RabbitMqMessageSerializer.class})
    public RabbitSendTemplate rabbitSenderTemplate(RabbitTemplate rabbitTemplate, RabbitMqMessageSerializer messageSerializer,
                                                   MessagePersistenceService messagePersistenceService) {

        // 记录持久化配置
        boolean persistenceEnabled = properties.getPersistence().isEnabled();
        String persistenceType = properties.getPersistence().getType().name();

        logger.info("Initializing Silky RabbitSenderTemplate - Persistence: {} (type: {})",
                persistenceEnabled ? "enabled" : "disabled", persistenceType);

        logger.info("Send Configuration - Mode: {}, Timeout: {}ms, Retry: {} (max: {}), Async Pool: {}",
                properties.getSend().getDefaultSendMode(),
                properties.getSend().getSyncTimeout(),
                properties.getSend().isEnableRetry() ? "enabled" : "disabled",
                properties.getSend().getMaxRetryCount(),
                properties.getSend().getAsyncThreadPoolSize());
        return new DefaultRabbitSendTemplate(rabbitTemplate, messageSerializer, properties, messagePersistenceService);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitMessageAspect rabbitMessageAspect(RabbitSendTemplate rabbitSendTemplate, MessagePersistenceService messagePersistenceService) {
        return new RabbitMessageAspect(rabbitSendTemplate, messagePersistenceService);
    }


    @PostConstruct
    public void initialize() {
        logger.info("Silky RabbitMQ AutoConfiguration initialized");
        if (persistenceService != null) {
            persistenceService.initialize();
        }
    }

    @PreDestroy
    public void destroy() {
        logger.info("Silky RabbitMQ AutoConfiguration destroying");
        if (persistenceService != null) {
            persistenceService.destroy();
        }
    }

}
