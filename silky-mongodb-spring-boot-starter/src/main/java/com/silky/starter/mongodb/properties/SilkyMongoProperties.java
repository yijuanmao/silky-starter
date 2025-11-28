package com.silky.starter.mongodb.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

import static com.silky.starter.mongodb.properties.SilkyMongoProperties.MONGO_DB_PROVIDER;

/**
 * Mongodb配置属性类
 *
 * @author zy
 * @date 2025-09-04 16:04
 **/
@Data
@ConfigurationProperties(prefix = MONGO_DB_PROVIDER)
public class SilkyMongoProperties {

    public static final String MONGO_DB_PROVIDER = "silky.mongodb";

    /**
     * 是否启用Mongodb
     */
    private boolean enabled = true;

    /**
     * 是否打印Mongodb操作日志，默认不打印
     */
    private boolean printLog;

    /**
     * 打印执行时间
     */
    protected Long slowTime;

    /**
     * 数据源配置
     */
    private Map<String, SilkyDataSourceProperties> datasource = new HashMap<>();

    /**
     * 数据源配置
     */
    @Data
    public static class SilkyDataSourceProperties {
        /**
         * 数据库连接地址
         */
        private String uri;

        /**
         * 数据库名称
         */
        private String database;

        /**
         * 读写分离配置
         */
        private ReadWriteSeparation readWriteSeparation;

    }

    /**
     * 读写分离配置
     */
    @Data
    public static class ReadWriteSeparation {

        /**
         * 是否启用读写分离
         */
        private boolean enabled = false;

        /**
         * 读数据库连接地址
         */
        private String readUri;

        /**
         * 读数据库名称,可选
         */
        private String readDatabase;

    }
}
