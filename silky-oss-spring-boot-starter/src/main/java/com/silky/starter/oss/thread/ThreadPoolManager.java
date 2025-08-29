package com.silky.starter.oss.thread;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * 线程池管理器
 *
 * @author zy
 * @date 2025-08-14 14:30
 **/
public class ThreadPoolManager {

    private final ThreadPoolTaskExecutor executor;

    public ThreadPoolManager(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    /**
     * 提交任务
     *
     * @param task 任务
     * @param <T>  任务返回类型
     * @return Future对象
     */
    public <T> Future<T> submitTask(Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * 执行任务
     *
     * @param task 任务
     */
    public void executeTask(Runnable task) {
        executor.execute(task);
    }
}
