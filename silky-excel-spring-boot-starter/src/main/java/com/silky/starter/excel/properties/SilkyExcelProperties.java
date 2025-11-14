package com.silky.starter.excel.properties;

import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.CompressionType;
import com.silky.starter.excel.enums.StorageType;
import lombok.Builder;
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
     * 是否启用Silky Excel
     */
    private boolean enabled = true;

    /**
     * 异步处理配置
     */
    private Async async = new Async();

    /**
     * 导出配置
     */
    private Export export = new Export();

    /**
     * 导入配置
     */
    private Import imports = new Import();

    /**
     * 存储配置
     */
    private Storage storage = new Storage();

    /**
     * 压缩配置
     */
    private CompressionConfig compression = CompressionConfig.defaultConfig();

    @Data
    public static class Storage {

        /**
         * 默认存储类型
         * 可选值：LOCAL, REDIS, MONGO, OSS
         * 默认值：LOCAL
         */
        private StorageType storageType = StorageType.LOCAL;

        /**
         * 本地存储配置
         */
        private LocalConfig local = new LocalConfig();
    }

    @Data
    public static class Async {

        /**
         * 是否启用异步处理
         */
        private boolean enabled = true;

        /**
         * 默认异步类型
         */
        private AsyncType asyncType = AsyncType.THREAD_POOL;

        /**
         * 线程池配置
         */
        private ThreadPool threadPool = new ThreadPool();

        @Data
        public static class ThreadPool {

            /**
             * 核心线程数
             */
            private int corePoolSize = 5;

            /**
             * 最大线程数
             */
            private int maxPoolSize = 20;

            /**
             * 队列容量
             */
            private int queueCapacity = 100;

            /**
             * 线程存活时间（秒）
             */
            private int keepAliveSeconds = 60;
        }
    }

    @Data
    public static class Export {

        /**
         * 每个Sheet的最大行数
         */
        private long maxRowsPerSheet = 200000;

        /**
         * 批处理大小
         */
        private int batchSize = 1000;

        /**
         * 临时文件路径
         */
        private String tempFilePath = "./temp/exports";

        /**
         * 超时时间（分钟）
         */
        private long timeoutMinutes = 30;

        /**
         * 是否启用导出进度记录
         */
        private boolean enableProgress;
    }

    @Data
    public static class Import {

        /**
         * 分页大小
         */
        private int pageSize = 1000;

        /**
         * 最大错误数量
         */
        private int maxErrorCount = 100;

        /**
         * 最大读取文件数量
         */
        private int maxReadCount = 10000;

        /**
         * 临时文件路径
         */
        private String tempFilePath = "./temp/imports";

        /**
         * 超时时间（分钟）
         */
        private long timeoutMinutes = 60;

        /**
         * 是否启用事务
         */
        private boolean enableTransaction = true;

        /**
         * 是否跳过表头
         */
        private boolean skipHeader = false;
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
    @Builder
    public static class CompressionConfig {

        /**
         * 是否启用压缩
         */
        private boolean enabled;

        /**
         * 压缩类型
         */
        private CompressionType type;

        /**
         * 压缩级别 0-9
         */
        private int compressionLevel;

        /**
         * 是否分割大文件
         */
        private boolean splitLargeFiles;

        /**
         * 分割大小（字节）
         */
        private long splitSize;


        public static CompressionConfig defaultConfig() {
            return CompressionConfig.builder()
                    .enabled(false)
                    .type(CompressionType.ZIP)
                    .compressionLevel(6)
                    .splitLargeFiles(false)
                    .splitSize(100 * 1024 * 1024)
                    .build();
        }
    }
}
