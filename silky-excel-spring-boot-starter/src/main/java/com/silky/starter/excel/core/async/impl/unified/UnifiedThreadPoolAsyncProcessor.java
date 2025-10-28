package com.silky.starter.excel.core.async.impl.unified;

import com.silky.starter.excel.core.async.AsyncTask;
import com.silky.starter.excel.core.async.model.ProcessorStatus;
import com.silky.starter.excel.core.async.UnifiedAsyncProcessor;
import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.engine.ImportEngine;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统一线程池异步处理器
 *
 * @author zy
 * @date 2025-10-27 21:54
 **/
@Slf4j
public class UnifiedThreadPoolAsyncProcessor implements UnifiedAsyncProcessor {

    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong exportCount = new AtomicLong(0);
    private final AtomicLong importCount = new AtomicLong(0);
    private volatile boolean available = true;
    private final long startTime = System.currentTimeMillis();
    private volatile long lastActiveTime = System.currentTimeMillis();
    private ThreadPoolExecutor threadPoolExecutor;

    private final ExportEngine exportEngine;
    private final ImportEngine importEngine;
    private final ThreadPoolTaskExecutor taskExecutor;

    public UnifiedThreadPoolAsyncProcessor(ExportEngine exportEngine,
                                           ImportEngine importEngine,
                                           ThreadPoolTaskExecutor taskExecutor) {
        this.exportEngine = exportEngine;
        this.importEngine = importEngine;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 获取处理器类型
     *
     * @return 处理器类型
     */
    @Override
    public String getType() {
        return AsyncType.THREAD_POOL.name();
    }

    /**
     * 提交任务到统一线程池
     *
     * @param task 任务
     */
    @Override
    public ExcelProcessResult submit(AsyncTask task) throws ExcelExportException {
        if (!isAvailable()) {
            throw new ExcelExportException("统一线程池处理器当前不可用，无法接收新任务");
        }
        if (taskExecutor == null || threadPoolExecutor.isShutdown() || threadPoolExecutor.isTerminated()) {
            throw new IllegalStateException("统一线程池未初始化或已关闭");
        }
        try {
            taskExecutor.execute(() -> {
                try {
                    this.process(task);
                } catch (Exception e) {
                    log.error("统一线程池任务执行失败: {}", task.getTaskId(), e);
                }
            });
            Long costTime = startTime - lastActiveTime;
            log.debug("任务已成功提交到统一线程池: {}, 类型: {}", task.getTaskId(), task.getTaskType());
            if (task instanceof ExportTask) {
                return ExcelProcessResult.success(task.getTaskId(), processedCount.get(), exportCount.get(), costTime);
            } else if (task instanceof ImportTask) {
                return ExcelProcessResult.success(task.getTaskId(), processedCount.get(), importCount.get(), costTime);
            } else {
                return ExcelProcessResult.fail(task.getTaskId(), "不支持的任务类型: " + task.getClass().getName());
            }
        } catch (Exception e) {
            log.error("统一线程池提交任务失败: {}", task.getTaskId(), e);
            return ExcelProcessResult.fail(task.getTaskId(), "统一线程池提交任务失败: " + e.getMessage());
        }
    }

    /**
     * 处理任务
     *
     * @param task 任务
     */
    @Override
    public ExcelProcessResult process(AsyncTask task) throws ExcelExportException {
        lastActiveTime = System.currentTimeMillis();
        try {
            log.info("开始处理任务: {}, 类型: {}, 业务类型: {}",
                    task.getTaskId(), task.getTaskType(), task.getBusinessType());

            ExcelProcessResult result;
            if (task instanceof ExportTask) {
                ExportTask<?> exportTask = (ExportTask<?>) task;
                exportTask.markStart();
                result = exportEngine.processExportTask(exportTask);
                exportCount.incrementAndGet();
            } else if (task instanceof ImportTask) {
                ImportTask<?> importTask = (ImportTask<?>) task;
                importTask.markStart();
                result = importEngine.processImportTask(importTask);
                importCount.incrementAndGet();
            } else {
                throw new IllegalArgumentException("不支持的任务类型: " + task.getClass().getName());
            }
            processedCount.incrementAndGet();
            log.info("线程异步任务处理完成: {}, 类型: {}", task.getTaskId(), task.getTaskType());

            return result;
        } catch (Exception e) {
            log.error("线程异步任务处理失败: {}", task.getTaskId(), e);
            return ExcelProcessResult.fail(task.getTaskId(), "线程异步任务处理失败: " + e.getMessage());
        } finally {
            // 标记任务完成
            if (task instanceof ExportTask) {
                ((ExportTask<?>) task).markFinish();
            } else if (task instanceof ImportTask) {
                ((ImportTask<?>) task).markFinish();
            }
        }
    }

    /**
     * 是否支持该任务类型
     *
     * @param taskType 任务类型
     * @return 是否支持
     */
    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.EXPORT || taskType == TaskType.IMPORT;
    }

    /**
     * 初始化处理器
     */
    @Override
    public void init() throws ExcelExportException {
        this.threadPoolExecutor = taskExecutor.getThreadPoolExecutor();
        log.info("统一线程池异步处理器初始化完成");
    }

    /**
     * 销毁处理器
     */
    @PreDestroy
    @Override
    public void destroy() throws ExcelExportException {
        log.info("silky-excel-开始关闭统一线程池异步处理器...");
        available = false;

        if (taskExecutor != null) {
            taskExecutor.shutdown();
            log.info("silky-excel-统一线程池已发送关闭信号，等待任务完成...");
        }
        log.info("silky-excel-统一线程池异步处理器关闭完成，总共处理任务数量：{} (导出: {}, 导入: {})",
                processedCount.get(), exportCount.get(), importCount.get());
    }

    /**
     * 处理器是否可用
     *
     * @return 是否可用
     */
    @Override
    public boolean isAvailable() {
        return available && threadPoolExecutor != null &&
                !threadPoolExecutor.isShutdown() && !threadPoolExecutor.isTerminated();
    }

    /**
     * 获取处理器状态
     *
     * @return 处理器状态
     */
    @Override
    public ProcessorStatus getStatus() {
        long queueSize = threadPoolExecutor != null ? threadPoolExecutor.getQueue().size() : 0;
        long activeCount = threadPoolExecutor != null ? threadPoolExecutor.getActiveCount() : 0;

        String message = String.format("活跃线程: %d, 队列大小: %d, 已完成任务: %d (导出: %d, 导入: %d)",
                activeCount, queueSize, processedCount.get(), exportCount.get(), importCount.get());

        return ProcessorStatus.builder()
                .type(getType())
                .available(isAvailable())
                .message(message)
                .processedCount(processedCount.get())
                .queueSize(queueSize)
                .startTime(LocalDateTime.now())
                .lastActiveTime(LocalDateTime.now())
                .build();
    }
}
