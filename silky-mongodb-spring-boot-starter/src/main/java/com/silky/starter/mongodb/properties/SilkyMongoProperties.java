package com.silky.starter.mongodb.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

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
     * 是否启用事务
     */
    private boolean transactionEnabled = false;

    /**
     * 主数据源名称，默认使用第一个数据源作为主数据源
     * 如果配置了primary，则使用指定的数据源作为主数据源
     */
    private String primary;

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
    private Map<String, DataSourceProperties> datasource = new HashMap<>();

    /**
     * 数据源配置
     */
    @Data
    public static class DataSourceProperties {
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

    /**
     * 获取主数据源名称
     * 如果配置了primary，则使用配置的值，否则使用第一个数据源
     */
    public String getPrimaryDataSourceName() {
        if (StringUtils.hasText(primary) && datasource.containsKey(primary)) {
            return primary;
        }
        // 如果没有配置primary或者配置的primary不存在，返回第一个数据源
        if (!datasource.isEmpty()) {
            return datasource.keySet().iterator().next();
        }
        throw new IllegalStateException("No data source configured");
    }

    /**
     * 获取主数据源配置
     */
    public DataSourceProperties getPrimaryDataSource() {
        String primaryName = getPrimaryDataSourceName();
        return datasource.get(primaryName);
    }

    /**
     * 检查指定的数据源是否存在
     */
    public boolean containsDataSource(String dataSourceName) {
        return datasource.containsKey(dataSourceName);
    }
}
