package com.silky.starter.rabbitmq.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * RabbitMQ增强配置属性
 *
 * @author zy
 * @date 2025-10-09 18:06
 **/
@Data
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {

    @NestedConfigurationProperty
    private PersistenceProperties persistence = new PersistenceProperties();

    @NestedConfigurationProperty
    private SendProperties send = new SendProperties();


    /**
     * PersistenceProperties RabbitMQ持久化配置属性
     */
    public static class PersistenceProperties extends RabbitPersistenceProperties {
    }


}




