package com.silky.starter.excel.properties;

import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.StorageType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Silky Excel 配置属性
 *
 * @author zy
 * @date 2025-10-24 11:51
 **/
@Data
@ConfigurationProperties(prefix = "silky.excel")
public class SilkyExcelProperties {

    /**
     * 是否启用Silky Excel组件
     * 默认值：true
     */
    private boolean enabled = true;

    /**
     * 异步处理配置
     */
    private AsyncConfig async = new AsyncConfig();

    /**
     * 存储配置
     */
    private StorageConfig storage = new StorageConfig();

    /**
     * 线程池配置
     */
    private ThreadPoolConfig threadPool = new ThreadPoolConfig();

    /**
     * 任务配置
     */
    private TaskConfig task = new TaskConfig();

    /**
     * 监控配置
     */
    private MonitorConfig monitor = new MonitorConfig();

    @Data
    public static class AsyncConfig {
        /**
         * 默认异步处理类型
         * 可选值：SYNC, THREAD_POOL, SPRING_ASYNC, DATABASE, CUSTOM
         * 默认值：THREAD_POOL
         */
        private AsyncType defaultType = AsyncType.THREAD_POOL;

        /**
         * 是否启用异步导出
         * 默认值：true
         */
        private boolean enabled = true;

        /**
         * 自定义处理器配置
         */
        private CustomProcessorConfig custom = new CustomProcessorConfig();
    }

    @Data
    public static class CustomProcessorConfig {
        /**
         * 是否启用自定义处理器扫描
         * 默认值：false
         */
        private boolean enabled = false;

        /**
         * 自定义处理器扫描包路径
         * 多个包路径用逗号分隔
         */
        private String scanPackages;
    }

    @Data
    public static class StorageConfig {
        /**
         * 默认存储类型
         * 可选值：LOCAL, REDIS, MONGO, OSS
         * 默认值：LOCAL
         */
        private StorageType defaultType = StorageType.LOCAL;

        /**
         * 本地存储配置
         */
        private LocalConfig local = new LocalConfig();

        /**
         * Redis存储配置
         */
        private RedisConfig redis = new RedisConfig();

        /**
         * MongoDB存储配置
         */
        private MongoConfig mongo = new MongoConfig();

        /**
         * OSS存储配置
         */
        private OssConfig oss = new OssConfig();
    }

    @Data
    public static class LocalConfig {
        /**
         * 本地存储基础路径
         * 默认值：/tmp/silky-excel
         */
        private String basePath = "/tmp/silky-excel";

        /**
         * 是否自动清理临时文件
         * 默认值：true
         */
        private boolean autoClean = true;

        /**
         * 清理间隔（秒）
         * 默认值：3600（1小时）
         */
        private long cleanInterval = 3600;

        /**
         * 文件保留天数
         * 默认值：7
         */
        private int retentionDays = 7;
    }

    @Data
    public static class RedisConfig {
        /**
         * 是否启用Redis存储
         * 默认值：false
         */
        private boolean enabled = false;

        /**
         * Redis Key前缀
         * 默认值：silky:excel:
         */
        private String keyPrefix = "silky:excel:";

        /**
         * 文件数据过期时间（秒）
         * 默认值：86400（24小时）
         */
        private long expireSeconds = 86400;
    }

    @Data
    public static class MongoConfig {
        /**
         * 是否启用MongoDB存储
         * 默认值：false
         */
        private boolean enabled = false;

        /**
         * 集合名称
         * 默认值：excel_files
         */
        private String collectionName = "excel_files";
    }

    @Data
    public static class OssConfig {
        /**
         * 是否启用OSS存储
         * 默认值：false
         */
        private boolean enabled = false;

        /**
         * OSS访问密钥
         */
        private String accessKey;

        /**
         * OSS秘密密钥
         */
        private String secretKey;

        /**
         * OSS端点
         */
        private String endpoint;

        /**
         * OSS桶名称
         */
        private String bucketName;

        /**
         * OSS区域
         */
        private String region;

        /**
         * URL过期时间（秒）
         * 默认值：3600（1小时）
         */
        private long urlExpire = 3600;
    }

    @Data
    public static class ThreadPoolConfig {

        /**
         * 核心线程数
         * 默认值：10
         */
        private int coreSize = 10;

        /**
         * 最大线程数
         * 默认值：50
         */
        private int maxSize = 50;

        /**
         * 队列容量
         * 默认值：1000
         */
        private int queueCapacity = 1000;

        /**
         * 线程空闲时间（秒）
         * 默认值：60
         */
        private int keepAliveSeconds = 60;

        /**
         * 线程名前缀
         * 默认值：silky-excel-
         */
        private String threadNamePrefix = "silky-excel-";

        /**
         * 是否允许核心线程超时
         * 默认值：false
         */
        private boolean allowCoreThreadTimeOut = false;

        /**
         * 关闭时是否等待任务完成
         * 默认值：true
         */
        private boolean waitForTasksToCompleteOnShutdown = true;

        /**
         * 等待任务完成的最大时间（秒）
         * 默认值：30
         */
        private int awaitTerminationSeconds = 30;
    }

    @Data
    public static class TaskConfig {
        /**
         * 默认分页大小
         * 默认值：2000
         */
        private int defaultPageSize = 2000;

        /**
         * 任务默认超时时间（毫秒）
         * 默认值：3600000（1小时）
         */
        private long defaultTimeout = 3600000;

        /**
         * 最大重试次数
         * 默认值：3
         */
        private int maxRetryCount = 3;

        /**
         * 重试间隔（毫秒）
         * 默认值：5000
         */
        private long retryInterval = 5000;
    }

    @Data
    public static class MonitorConfig {
        /**
         * 是否启用监控
         * 默认值：true
         */
        private boolean enabled = true;

        /**
         * 监控数据保留天数
         * 默认值：30
         */
        private int retentionDays = 30;

        /**
         * 监控数据采集间隔（秒）
         * 默认值：60
         */
        private int collectInterval = 60;
    }

    /**
     * 获取完整的配置信息字符串
     *
     * @return 配置信息
     */
    public String getConfigInfo() {
        return String.format(
                "Silky Excel配置: 启用=%s, 默认异步方式=%s, 默认存储=%s, 线程池=%d/%d, 默认分页=%d",
                enabled, async.getDefaultType(), storage.getDefaultType(),
                threadPool.getCoreSize(), threadPool.getMaxSize(), task.getDefaultPageSize()
        );
    }
}
