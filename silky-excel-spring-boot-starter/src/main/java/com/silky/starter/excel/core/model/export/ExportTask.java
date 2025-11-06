package com.silky.starter.excel.core.model.export;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.silky.starter.excel.core.async.BaseAsyncTask;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 导出任务
 *
 * @author zy
 * @date 2025-10-24 11:23
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExportTask<T> extends BaseAsyncTask {

    /**
     * 导出请求配置
     * 包含导出操作的所有参数
     */
    private ExportRequest<T> request;

    /**
     * 任务创建时间戳
     * 用于计算任务执行时间
     */
    private Long createTime;

    /**
     * 任务开始执行
     */
    private LocalDateTime startTime;

    /**
     * 任务完成
     */
    private LocalDateTime finishTime;

    /**
     * 任务执行线程
     * 记录执行任务的线程信息（用于监控和调试）
     */
    private transient Thread executeThread;

    /**
     * 任务执行上下文
     * 用于在任务执行过程中传递额外信息
     */
    private transient Object context;

    /**
     * 标记任务开始执行
     * 设置开始时间并记录执行线程
     */
    public void markStart() {
        this.startTime = LocalDateTime.now();
        this.executeThread = Thread.currentThread();
    }

    /**
     * 标记任务完成
     * 设置完成时间
     */
    public void markFinish() {
        this.finishTime = LocalDateTime.now();
    }

    /**
     * 获取任务创建到当前的时间
     *
     * @return 耗时（毫秒）
     */
    public long getElapsedTime() {
        long now = System.currentTimeMillis();
        return now - createTime;
    }

    /**
     * 获取任务执行时间
     *
     * @return 执行时间（毫秒），如果任务未开始返回0
     */
    public long getExecuteTime() {
        if (startTime == null) {
            return 0;
        }
        LocalDateTime endTime = finishTime == null ? LocalDateTime.now() : finishTime;
        return LocalDateTimeUtil.toEpochMilli(endTime) - LocalDateTimeUtil.toEpochMilli(startTime);
    }

    /**
     * 检查任务是否超时
     *
     * @param timeoutMinutes 任务超时时间（分钟）
     * @return 如果超时返回true，否则返回false
     */
    public boolean isTimeout(long timeoutMinutes) {
        return startTime != null && LocalDateTime.now().isAfter(startTime.plusMinutes(timeoutMinutes));
    }

    /**
     * 检查任务是否正在执行
     *
     * @return 如果正在执行返回true，否则返回false
     */
    public boolean isRunning() {
        return startTime != null && finishTime == null;
    }

    /**
     * 检查任务是否已完成
     *
     * @return 如果已完成返回true，否则返回false
     */
    public boolean isFinished() {
        return finishTime != null;
    }

}
