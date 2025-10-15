package com.silky.starter.rabbitmq.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RabbitMQ持久化配置属性
 *
 * @author zy
 * @date 2025-10-09 18:11
 **/
@Data
@ConfigurationProperties(prefix = "rabbitmq.persistence")
public class RabbitPersistenceProperties {

    /**
     * 是否启用持久化功能 默认启用
     */
    private boolean enabled = true;

    /**
     * 持久化类型 默认内存
     */
    private PersistenceType type = PersistenceType.MEMORY;

    /**
     * 数据库配置
     */
    private DatabaseConfig database = new DatabaseConfig();

    /**
     * MongoDB配置
     */
    private MongoConfig mongo = new MongoConfig();

    /**
     * Redis配置
     */
    private RedisConfig redis = new RedisConfig();


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
         * MongoDB
         */
        MONGO,

        /**
         * Redis
         */
        REDIS
    }


    /**
     * 数据库配置
     */
    @Data
    public static class DatabaseConfig {

        private String tableName = "rabbitmq_messages";

        private boolean autoCreateTable = true;

    }

    /**
     * MongoDB配置
     */
    @Data
    public static class MongoConfig {
        private String collection = "rabbitmq_messages";

    }

    /**
     * Redis配置
     */
    @Data
    public static class RedisConfig {
        private String keyPrefix = "rabbitmq:message:";
        private long ttl = 86400L; // 24小时

    }
}
