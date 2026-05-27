package com.silky.starter.excel.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Silky Excel 配置属性。
 *
 * @author zy
 * @date 2025-10-24 11:51
 **/
@Data
@ConfigurationProperties(prefix = "silky.excel")
public class SilkyExcelProperties {

    /**
     * 是否启用 Silky Excel。
     */
    private boolean enabled = true;

    /**
     * 导出配置。
     */
    private Export export = new Export();

    /**
     * 导入配置。
     */
    private Import imports = new Import();

    /**
     * 存储配置。
     */
    private Storage storage = new Storage();

    /**
     * 异步线程池配置。
     */
    private Async async = new Async();

    @Data
    public static class Export {

        /**
         * 每次从数据源分页读取的数据量。
         */
        private int pageSize = 1000;

        /**
         * 每个 Sheet 的最大行数，超过后自动创建新 Sheet。
         */
        private long maxRowsPerSheet = 200000;

        /**
         * 导出任务超时时间（分钟）。
         */
        private long timeoutMinutes = 30;

        /**
         * 是否记录导出进度。
         */
        private boolean progressEnabled = true;
    }

    @Data
    public static class Import {

        /**
         * 每批读取并提交给业务导入器的数据量。
         */
        private int pageSize = 1000;

        /**
         * 最大错误数量，超过后停止导入。
         */
        private int maxErrorCount = 100;

        /**
         * 导入任务超时时间（分钟）。
         */
        private long timeoutMinutes = 60;

        /**
         * 表头行数。FastExcel 会从表头之后开始映射数据。
         */
        private int headRowNumber = 1;
    }

    @Data
    public static class Storage {

        /**
         * 默认存储类型。内置 local，其他类型由业务方实现 StorageStrategy。
         */
        private String type = "local";

        /**
         * 本地存储配置。
         */
        private LocalConfig local = new LocalConfig();
    }

    @Data
    public static class LocalConfig {

        /**
         * 本地存储基础路径。
         */
        private String basePath = "/tmp/silky-excel";
    }

    @Data
    public static class Async {

        /**
         * 线程池配置。
         */
        private ThreadPool threadPool = new ThreadPool();

        @Data
        public static class ThreadPool {

            /**
             * 核心线程数。
             */
            private int corePoolSize = 5;

            /**
             * 最大线程数。
             */
            private int maxPoolSize = 20;

            /**
             * 队列容量。
             */
            private int queueCapacity = 100;

            /**
             * 线程存活时间（秒）。
             */
            private int keepAliveSeconds = 60;
        }
    }
}
