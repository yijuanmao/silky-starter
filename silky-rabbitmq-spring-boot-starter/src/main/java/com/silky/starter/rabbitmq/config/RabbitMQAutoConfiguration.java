package com.silky.starter.rabbitmq.config;

import com.silky.starter.rabbitmq.aop.RabbitMessageAspect;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.persistence.NoOpMessagePersistenceService;
import com.silky.starter.rabbitmq.properties.RabbitMQProperties;
import com.silky.starter.rabbitmq.serialization.FastJson2MessageSerializer;
import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
import com.silky.starter.rabbitmq.template.DefaultRabbitSendTemplate;
import com.silky.starter.rabbitmq.template.RabbitSendTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ自动配置（使用Fastjson2序列化）
 *
 * @author zy
 * @date 2025-10-12 10:14
 **/
@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
@ConditionalOnClass({RabbitTemplate.class})
//@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
//@ConditionalOnProperty(prefix = "silky.rabbitmq", name = "send.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "silky.send.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQAutoConfiguration {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, RabbitMQProperties properties) {
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

    @Bean
    @ConditionalOnMissingBean(MessagePersistenceService.class)
    @ConditionalOnProperty(prefix = "silky.rabbitmq.persistence", name = "enabled", havingValue = "false", matchIfMissing = true)
    public MessagePersistenceService messagePersistenceService() {
        return new NoOpMessagePersistenceService();
    }

    @Bean
    @ConditionalOnMissingBean(RabbitSendTemplate.class)
    public RabbitSendTemplate rabbitSenderTemplate(RabbitTemplate rabbitTemplate, RabbitMqMessageSerializer messageSerializer,
                                                   RabbitMQProperties properties, MessagePersistenceService messagePersistenceService) {
        return new DefaultRabbitSendTemplate(rabbitTemplate, messageSerializer, properties, messagePersistenceService);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitMessageAspect rabbitMessageAspect(RabbitSendTemplate rabbitSendTemplate, MessagePersistenceService messagePersistenceService) {
        return new RabbitMessageAspect(rabbitSendTemplate, messagePersistenceService);
    }

}
