package com.silky.starter.excel.core.thread;

import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 *
 * @author zy
 * @date 2025-08-14 15:47
 **/
public class ExportThreadPoolConfig {

    /**
     * 默认线程池配置
     *
     * @return {@link ThreadPoolTaskExecutor}
     */
    @Bean(name = "exportThreadPoolTaskExecutor")
    public ThreadPoolTaskExecutor exportThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor defaultExecutor = new ThreadPoolTaskExecutor();
        //核心线程数目 ,cpu个数 * 2
        defaultExecutor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        //指定最大线程数
        defaultExecutor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        //队列中最大的数目
        defaultExecutor.setQueueCapacity(128);
        //线程空闲后的最大存活时间,单位：s
        defaultExecutor.setKeepAliveSeconds(60);
        // 线程池对拒绝任务(无线程可用)的处理策略
        defaultExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //线程名称前缀
        defaultExecutor.setThreadNamePrefix("silky-excel-");
        return defaultExecutor;
    }
}
