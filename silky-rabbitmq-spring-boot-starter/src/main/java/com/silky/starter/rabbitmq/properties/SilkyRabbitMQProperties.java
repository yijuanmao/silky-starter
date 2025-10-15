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
@ConfigurationProperties(prefix = SilkyRabbitMQProperties.PREFIX)
public class SilkyRabbitMQProperties {

    public static final String PREFIX = "spring.rabbitmq.silky";

    /**
     * 是否启用Silky组件
     */
    private boolean enabled = true;

    @NestedConfigurationProperty
    private PersistenceProperties persistence = new PersistenceProperties();

    @NestedConfigurationProperty
    private SendProperties send = new SendProperties();


    /**
     * PersistenceProperties RabbitMQ持久化配置属性
     */
    @Getter
    @Setter
    public static class PersistenceProperties {

        /**
         * 是否启用消息持久化功能
         */
        private boolean enabled = false;

        /**
         * 持久化类型
         */
        private PersistenceType type = PersistenceType.MEMORY;


        public enum PersistenceType {

            /**
             * 内存
             */
            MEMORY,

            /**
             * 数据库
             */
            DATABASE,

            /**
             * Redis
             */
            REDIS,

            /**
             * MongoDB
             */
            MONGO,

            /**
             * 自定义
             */
            CUSTOM
        }
    }

}




