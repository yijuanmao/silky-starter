package com.silky.starter.redis.stream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis Stream 配置属性
 * <p>
 * 通过 {@code silky.redis.stream.*} 配置，控制 Stream 消费行为。
 * </p>
 *
 * <pre>
 * silky:
 *   redis:
 *     stream:
 *       enabled: true
 *       default-group: my-group
 *       consumer:
 *         batch-size: 10
 *         poll-timeout-ms: 2000
 *         max-retries: 3
 *         concurrency: 1
 *         thread-pool:
 *           core-size: 5
 *           max-size: 20
 *           queue-capacity: 100
 *           thread-name-prefix: silky-stream-
 *       dead-letter:
 *         enabled: true
 *         suffix: ":dead"
 *       monitor:
 *         enabled: true
 *         slow-threshold-ms: 500
 * </pre>
 *
 * @author zy
 * @date 2026-05-09
 */
@Data
@ConfigurationProperties(prefix = "silky.redis.stream")
public class RedisStreamProperties {

    /**
     * 是否启用 Redis Stream 功能，默认 false
     */
    private boolean enabled = false;

    /**
     * 全局默认消费者组名，可被注解 group 属性覆盖
     */
    private String defaultGroup = "silky-default-group";

    /**
     * 消费者配置
     */
    private ConsumerConfig consumer = new ConsumerConfig();

    /**
     * 死信队列配置
     */
    private DeadLetterConfig deadLetter = new DeadLetterConfig();

    /**
     * 监控配置
     */
    private MonitorConfig monitor = new MonitorConfig();

    // ======================== 内部配置类 ========================

    @Data
    public static class ConsumerConfig {
        /**
         * 每次 XREADGROUP 拉取的最大消息数，默认 10
         */
        private int batchSize = 10;

        /**
         * 阻塞读取超时时间（毫秒），0 表示永久阻塞，默认 2000ms
         */
        private long pollTimeoutMs = 2000;

        /**
         * 最大重试次数（超过后进入死信队列），默认 3
         */
        private int maxRetries = 3;

        /**
         * Pending 消息认为超时的阈值（毫秒），超时后尝试 XCLAIM，默认 30000ms（30秒）
         */
        private long pendingTimeoutMs = 30000;

        /**
         * 每个 streamKey 默认并发消费者数量，默认 1
         */
        private int concurrency = 1;

        /**
         * 线程池配置
         */
        private ThreadPoolConfig threadPool = new ThreadPoolConfig();
    }

    @Data
    public static class ThreadPoolConfig {
        /**
         * 核心线程数，默认 5
         */
        private int coreSize = 5;

        /**
         * 最大线程数，默认 20
         */
        private int maxSize = 20;

        /**
         * 任务队列容量，默认 100
         */
        private int queueCapacity = 100;

        /**
         * 线程名前缀，默认 silky-stream-
         */
        private String threadNamePrefix = "silky-stream-";

        /**
         * 等待任务完成后再关闭线程池（优雅关闭），默认 true
         */
        private boolean waitForTasksToCompleteOnShutdown = true;

        /**
         * 优雅关闭等待超时时间（秒），默认 30
         */
        private int awaitTerminationSeconds = 30;
    }

    @Data
    public static class DeadLetterConfig {
        /**
         * 是否启用死信队列，默认 true
         */
        private boolean enabled = true;

        /**
         * 死信 Stream Key 后缀，默认 ":dead"
         */
        private String suffix = ":dead";
    }

    @Data
    public static class MonitorConfig {
        /**
         * 是否启用慢消费监控日志，默认 true
         */
        private boolean enabled = true;

        /**
         * 慢消费告警阈值（毫秒），超过则打印 WARN 日志，默认 500ms
         */
        private long slowThresholdMs = 500;
    }
}
