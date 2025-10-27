package com.silky.starter.excel.core.async.impl.export;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.silky.starter.excel.core.async.ExportAsyncProcessor;
import com.silky.starter.excel.core.async.ProcessorStatus;
import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.enums.AsyncType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import javax.annotation.PreDestroy;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程池异步处理器
 * 使用内置线程池执行导出任务，适用于大多数异步导出场景
 * 支持任务队列、线程池监控和优雅关闭
 *
 * @author zy
 * @date 2025-10-24 14:33
 **/
@Slf4j
public class ExportThreadPoolAsyncProcessor implements ExportAsyncProcessor {

    /**
     * 已处理任务计数器
     * 线程安全的任务计数
     */
    private final AtomicLong processedCount = new AtomicLong(0);

    /**
     * 处理器可用状态
     * volatile保证多线程可见性
     */
    private volatile boolean available = true;

    /**
     * 处理器启动时间
     */
    private final long startTime = System.currentTimeMillis();

    /**
     * 最后活跃时间
     */
    private volatile long lastActiveTime = System.currentTimeMillis();

    /**
     * 底层ThreadPoolExecutor引用
     * 用于获取线程池的运行时状态
     */
    private ThreadPoolExecutor threadPoolExecutor;

    private final ExportEngine exportEngine;

    private final ThreadPoolTaskExecutor taskExecutor;

    public ExportThreadPoolAsyncProcessor(ExportEngine exportEngine,
                                          ThreadPoolTaskExecutor taskExecutor) {
        this.exportEngine = exportEngine;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 获取处理器类型，类型应该与AsyncType枚举值对应，用于在工厂中标识处理器
     * 参照枚举类:{@link AsyncType}
     */
    @Override
    public String getType() {
        return AsyncType.THREAD_POOL.name();
    }

    /**
     * 提交导出任务 将任务提交到异步处理系统，立即返回，任务会在后台执行
     *
     * @param task 要处理的导出任务，包含任务ID、请求参数和记录信息，注意：此方法应该是非阻塞的，提交后立即返回
     */
    @Override
    public void submit(ExportTask<?> task) throws ExcelExportException {
        // 检查处理器状态
        if (!isAvailable()) {
            throw new ExcelExportException("Spring线程池处理器当前不可用，无法接收新任务");
        }
        // 检查线程池状态
        if (taskExecutor == null || threadPoolExecutor.isShutdown() || threadPoolExecutor.isTerminated()) {
            throw new IllegalStateException("Spring线程池未初始化或已关闭");
        }
        try {
            // 使用Spring线程池提交任务
            taskExecutor.execute(() -> {
                try {
                    process(task);
                } catch (Exception e) {
                    log.error("Spring线程池任务执行失败: {}", task.getTaskId(), e);
                }
            });

            log.debug("任务已成功提交到Spring线程池: {}", task.getTaskId());

        } catch (Exception e) {
            log.error("Spring线程池提交任务失败: {}", task.getTaskId(), e);
            throw new ExcelExportException("提交任务到Spring线程池失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理导出任务，实际执行导出任务的核心方法，包含数据查询、Excel生成和文件上传
     *
     * @param task 要处理的导出任务
     */
    @Override
    public void process(ExportTask<?> task) throws ExcelExportException {
        // 更新最后活跃时间
        lastActiveTime = System.currentTimeMillis();

        try {
            log.info("开始处理导出任务: {}, 业务类型: {}", task.getTaskId(), task.getRequest().getBusinessType());

            // 标记任务开始执行
            task.markStart();

            // 调用导出引擎处理任务
            exportEngine.processExportTask(task);

            // 增加处理计数
            processedCount.incrementAndGet();

            log.info("导出任务处理完成: {}, 总处理时间: {}ms", task.getTaskId(), task.getExecuteTime());

        } catch (Exception e) {
            log.error("导出任务处理失败: {}", task.getTaskId(), e);
            throw new ExcelExportException("任务处理失败: " + e.getMessage(), e);
        } finally {
            // 标记任务完成
            task.markFinish();
        }
    }

    /**
     * 初始化处理器
     * 在处理器注册到工厂后调用，用于初始化资源
     */
    @Override
    public void init() throws ExcelExportException {

        // 初始化线程池
//        taskExecutor.initialize();

        // 获取底层ThreadPoolExecutor用于状态监控
        this.threadPoolExecutor = taskExecutor.getThreadPoolExecutor();

        log.info("Spring线程池异步处理器初始化完成，当前线程池状态：活跃线程={}，队列大小={}",
                threadPoolExecutor.getActiveCount(), threadPoolExecutor.getQueue().size());
    }

    /**
     * 销毁处理器
     * 在处理器从工厂注销时调用，用于释放资源
     */
    @PreDestroy
    @Override
    public void destroy() throws ExcelExportException {
        log.info("开始关闭Spring线程池异步处理器...");
        available = false;

        if (taskExecutor != null) {
            // Spring的ThreadPoolTaskExecutor会自动处理优雅关闭
            taskExecutor.shutdown();
            log.info("Spring线程池已发送关闭信号，等待任务完成...");
        }
        log.info("Spring线程池异步处理器关闭完成，总共处理任务数量：{}", processedCount.get());
    }

    /**
     * 检查处理器是否可用，用于判断处理器当前是否能够处理任务
     *
     * @return 如果处理器可用返回true，否则返回false
     */
    @Override
    public boolean isAvailable() {
        return available &&
                threadPoolExecutor != null &&
                !threadPoolExecutor.isShutdown() &&
                !threadPoolExecutor.isTerminated();
    }

    /**
     * 获取处理器状态
     * 返回处理器的当前状态信息，用于监控和管理
     *
     * @return 处理器状态对象，包含类型、可用性、处理数量等信息
     */
    @Override
    public ProcessorStatus getStatus() {
        long queueSize = threadPoolExecutor != null ? threadPoolExecutor.getQueue().size() : 0;
        long activeCount = threadPoolExecutor != null ? threadPoolExecutor.getActiveCount() : 0;

        String message = String.format("活跃线程: %d, 队列大小: %d, 已完成任务: %d",
                activeCount, queueSize, processedCount.get());

        return ProcessorStatus.builder()
                .type(getType())
                .available(isAvailable())
                .message(message)
                .processedCount(processedCount.get())
                .queueSize(queueSize)
                .startTime(LocalDateTimeUtil.ofUTC(startTime))
                .lastActiveTime(LocalDateTimeUtil.ofUTC(lastActiveTime))
                .build();
    }

    /**
     * 获取处理器描述信息
     *
     * @return 处理器描述信息
     */
    @Override
    public String getDescription() {
        return "Async Processor: " + getType();
    }

    /**
     * 提交任务并返回Future（扩展方法）
     * 允许调用方获取任务执行结果
     *
     * @param task 导出任务
     * @return Future对象，可以用于获取任务执行状态和结果
     */
    public Future<?> submitWithFuture(ExportTask<?> task) {
        if (!isAvailable()) {
            throw new ExcelExportException("Spring线程池处理器当前不可用");
        }
        return taskExecutor.submit(() -> {
            process(task);
            return task.getTaskId();
        });
    }

    /**
     * 提交任务并返回ListenableFuture（扩展方法）
     * 支持Spring的异步回调机制
     *
     * @param task 导出任务
     * @return ListenableFuture对象，支持回调监听
     */
    public ListenableFuture<String> submitWithListenableFuture(ExportTask<?> task) {
        if (!isAvailable()) {
            throw new ExcelExportException("Spring线程池处理器当前不可用");
        }
        return taskExecutor.submitListenable(() -> {
            process(task);
            return task.getTaskId();
        });
    }

}
