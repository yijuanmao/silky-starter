package com.silky.starter.rabbitmq.config;

import com.silky.starter.rabbitmq.aop.RabbitMessageAspect;
import com.silky.starter.rabbitmq.properties.RabbitMQProperties;
import com.silky.starter.rabbitmq.serialization.FastJson2MessageSerializer;
import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
import com.silky.starter.rabbitmq.template.DefaultRabbitSendTemplate;
import com.silky.starter.rabbitmq.template.RabbitSendTemplate;
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
@ConditionalOnProperty(prefix = "silky.rabbitmq", name = "send.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RabbitMqMessageSerializer fastJson2MessageSerializer() {
        return new FastJson2MessageSerializer();
    }

    @Bean
    @ConditionalOnMissingBean(RabbitSendTemplate.class)
    public RabbitSendTemplate rabbitSenderTemplate(RabbitTemplate rabbitTemplate, RabbitMqMessageSerializer messageSerializer, RabbitMQProperties properties) {
        return new DefaultRabbitSendTemplate(rabbitTemplate, messageSerializer, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitMessageAspect rabbitMessageAspect(RabbitSendTemplate rabbitSendTemplate) {
        return new RabbitMessageAspect(rabbitSendTemplate);
    }

}
