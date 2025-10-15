package com.silky.starter.rabbitmq.properties;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * RabbitMQ增强配置属性
 *
 * @author zy
 * @date 2025-10-09 18:06
 **/
@Data
@ConfigurationProperties(prefix = RabbitMQProperties.PREFIX)
public class RabbitMQProperties {

    public static final String PREFIX = "silky";

    @NestedConfigurationProperty
    private PersistenceProperties persistence = new PersistenceProperties();

    @NestedConfigurationProperty
    private SendProperties send = new SendProperties();


    /**
     * PersistenceProperties RabbitMQ持久化配置属性
     */
    @Getter
    @Setter
    public static class PersistenceProperties extends RabbitPersistenceProperties {

        /**
         * 是否启用消息持久化功能
         */
        private boolean enabled = false;

        /**
         * 持久化类型
         */
        private PersistenceType type = PersistenceType.MEMORY;
    }


}




