package com.silky.starter.excel.core.async.impl.export;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.silky.starter.excel.core.async.ExportAsyncProcessor;
import com.silky.starter.excel.core.async.model.ProcessorStatus;
import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.enums.AsyncType;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 同步处理器 在当前线程中立即执行导出任务，适用于小数据量或需要立即返回结果的场景 ，简单可靠，没有异步调用的复杂性
 *
 * @author zy
 * @date 2025-10-24 14:49
 **/
@Slf4j
public class ExportSyncAsyncProcessor implements ExportAsyncProcessor {

    /**
     * 已处理任务计数器
     */
    private final AtomicLong processedCount = new AtomicLong(0);

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

    public ExportSyncAsyncProcessor(ExportEngine exportEngine) {
        this.exportEngine = exportEngine;
    }

    /**
     * 获取处理器类型
     *
     * @return 处理器类型字符串 "SYNC"
     */
    @Override
    public String getType() {
        return AsyncType.SYNC.name();
    }

    /**
     * 提交导出任务
     * 同步执行，立即处理任务并阻塞直到完成
     *
     * @param task 要处理的导出任务
     */
    @Override
    public ExcelProcessResult submit(ExportTask<?> task) throws ExcelExportException {
        // 检查处理器状态
        if (!isAvailable()) {
            throw new IllegalStateException("同步处理器当前不可用，无法处理任务");
        }
        log.debug("开始同步执行导出任务: {}", task.getTaskId());
        try {
            // 直接调用process方法执行任务
            ExcelProcessResult result = process(task);
            log.debug("同步导出任务执行完成: {}", task.getTaskId());
            return result;
        } catch (Exception e) {
            log.error("同步导出任务执行失败: {}", task.getTaskId(), e);
            return ExcelProcessResult.fail(task.getTaskId(), "同步导出任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 处理导出任务
     * 实际执行导出逻辑
     *
     * @param task 要处理的导出任务
     */
    @Override
    public ExcelProcessResult process(ExportTask<?> task) throws ExcelExportException {
        // 更新最后活跃时间
        lastActiveTime = System.currentTimeMillis();

        try {
            log.info("开始同步处理导出任务: {}, 业务类型: {}",
                    task.getTaskId(), task.getRequest().getBusinessType());

            // 标记任务开始执行
            task.markStart();

            // 调用导出引擎处理任务
            ExcelProcessResult result = exportEngine.processExportTask(task);

            // 增加处理计数
            processedCount.incrementAndGet();

            log.debug("同步导出任务处理完成: {}, 总处理时间: {}ms", task.getTaskId(), task.getExecuteTime());
            return result;
        } catch (Exception e) {
            log.error("同步导出任务处理失败: {}", task.getTaskId(), e);
            return ExcelProcessResult.fail(task.getTaskId(), "同步任务处理失败: " + e.getMessage());
        } finally {
            // 标记任务完成
            task.markFinish();
        }
    }

    /**
     * 检查处理器是否可用
     *
     * @return 如果处理器可用返回true，否则返回false
     */
    @Override
    public boolean isAvailable() {
        return available;
    }

    /**
     * 获取处理器状态
     *
     * @return 包含详细状态信息的ProcessorStatus对象
     */
    @Override
    public ProcessorStatus getStatus() {
        String message = String.format("已处理任务: %d, 运行时长: %dms", processedCount.get(), getUptime());

        return ProcessorStatus.builder()
                .type(getType())
                .available(isAvailable())
                .message(message)
                .processedCount(processedCount.get())
                .startTime(LocalDateTimeUtil.ofUTC(startTime))
                .lastActiveTime(LocalDateTimeUtil.ofUTC(lastActiveTime))
                .build();
    }

    /**
     * 获取处理器描述信息
     *
     * @return 处理器描述
     */
    @Override
    public String getDescription() {
        return "同步处理器 - 在当前线程中立即执行导出任务";
    }

    /**
     * 设置处理器可用状态
     * 可以用于临时禁用处理器
     *
     * @param available 是否可用
     */
    public void setAvailable(boolean available) {
        this.available = available;
        log.info("同步处理器可用状态设置为: {}", available);
    }

    /**
     * 获取处理器运行时长
     *
     * @return 运行时长（毫秒）
     */
    private long getUptime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 获取处理器空闲时长
     *
     * @return 空闲时长（毫秒）
     */
    private long getIdleTime() {
        return System.currentTimeMillis() - lastActiveTime;
    }
}
