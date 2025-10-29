package com.silky.starter.excel.core.async.impl.unified;

import com.silky.starter.excel.core.async.AsyncTask;
import com.silky.starter.excel.core.async.UnifiedAsyncProcessor;
import com.silky.starter.excel.core.async.model.ProcessorStatus;
import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.engine.ImportEngine;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.TaskType;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统一同步异步处理器实现
 *
 * @author zy
 * @date 2025-10-27 21:44
 **/
@Slf4j
public class UnifiedSyncAsyncProcessor implements UnifiedAsyncProcessor {

    /**
     * 已处理任务计数器
     */
    private final AtomicLong processedCount = new AtomicLong(0);

    /**
     * 导出任务计数器
     */
    private final AtomicLong exportCount = new AtomicLong(0);

    /**
     * 导入任务计数器
     */
    private final AtomicLong importCount = new AtomicLong(0);

    /**
     * 处理器可用状态
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

    private final ExportEngine exportEngine;

    private final ImportEngine importEngine;

    public UnifiedSyncAsyncProcessor(ExportEngine exportEngine, ImportEngine importEngine) {
        this.exportEngine = exportEngine;
        this.importEngine = importEngine;
    }

    /**
     * 处理任务
     */
    @Override
    public String getType() {
        return AsyncType.SYNC.name();
    }

    /**
     * 提交任务
     *
     * @param task 要处理的任务
     */
    @Override
    public ExcelProcessResult submit(AsyncTask task) throws ExcelExportException {
        if (!isAvailable()) {
            throw new IllegalStateException("统一同步处理器当前不可用，无法处理任务");
        }
        log.debug("开始同步执行任务: {}, 类型: {}", task.getTaskId(), task.getTaskType());
        try {
            ExcelProcessResult result = this.process(task);
            log.debug("同步任务执行完成: {}", task.getTaskId());
            return result;
        } catch (Exception e) {
            log.error("同步任务执行失败: {}", task.getTaskId(), e);
            return ExcelProcessResult.fail(task.getTaskId(), "同步执行任务失败: " + e.getMessage());
        }
    }

    /**
     * 处理任务
     *
     * @param task 任务
     * @return 处理结果
     */
    @Override
    public ExcelProcessResult process(AsyncTask task) throws ExcelExportException {
        lastActiveTime = System.currentTimeMillis();
        try {
            log.info("开始同步处理任务: {}, 类型: {}, 业务类型: {}", task.getTaskId(), task.getTaskType(), task.getBusinessType());

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
            log.info("同步任务处理完成: {}, 类型: {}", task.getTaskId(), task.getTaskType());

            return result;
        } catch (Exception e) {
            log.error("同步任务处理失败: {}", task.getTaskId(), e);
            return ExcelProcessResult.fail(task.getTaskId(), "同步任务处理失败: " + e.getMessage());
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
     * @return 是否支持, true 支持，false 不支持
     */
    @Override
    public boolean supports(TaskType taskType) {
        return taskType == TaskType.EXPORT || taskType == TaskType.IMPORT;
    }

    /**
     * 处理器是否可用
     *
     * @return 是否可用
     */
    @Override
    public boolean isAvailable() {
        return available;
    }

    /**
     * 获取处理器状态
     *
     * @return 处理器状态
     */
    @Override
    public ProcessorStatus getStatus() {
        String message = String.format("已处理任务: %d (导出: %d, 导入: %d), 运行时长: %dms",
                processedCount.get(), exportCount.get(), importCount.get(), getUptime());
        return ProcessorStatus.builder()
                .type(getType())
                .available(isAvailable())
                .message(message)
                .processedCount(processedCount.get())
                .startTime(java.time.LocalDateTime.now())
                .lastActiveTime(LocalDateTime.now())
                .build();
    }

    /**
     * 获取运行时间
     *
     * @return 处理器描述
     */
    private long getUptime() {
        return System.currentTimeMillis() - startTime;
    }
}
