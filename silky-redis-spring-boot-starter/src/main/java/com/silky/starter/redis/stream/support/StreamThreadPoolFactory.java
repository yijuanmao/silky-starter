package com.silky.starter.redis.stream.support;

import com.silky.starter.redis.stream.config.RedisStreamProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Redis Stream 消费者线程池工厂
 * <p>
 * 基于 {@link ThreadPoolTaskExecutor} 创建线程池，支持优雅关闭。
 * 用户可以通过注入自定义的 {@link Executor} Bean 来覆盖默认线程池。
 * </p>
 *
 * @author zy
 * @date 2026-05-09
 */
public class StreamThreadPoolFactory {

    private static final Logger log = LoggerFactory.getLogger(StreamThreadPoolFactory.class);

    /**
     * 根据配置创建线程池
     *
     * @param config 线程池配置
     * @return 已初始化的线程池
     */
    public static ThreadPoolTaskExecutor create(RedisStreamProperties.ThreadPoolConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCoreSize());
        executor.setMaxPoolSize(config.getMaxSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(config.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(config.getAwaitTerminationSeconds());
        // 拒绝策略：由调用方线程执行（降级兜底，不丢失消息）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        log.info("[RedisStream] 消费者线程池初始化完成: coreSize={}, maxSize={}, queueCapacity={}, prefix={}",
                config.getCoreSize(), config.getMaxSize(), config.getQueueCapacity(),
                config.getThreadNamePrefix());

        return executor;
    }
}
