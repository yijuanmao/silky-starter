package com.silky.starter.excel.core.model.imports;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.async.BaseAsyncTask;
import com.silky.starter.excel.entity.ImportRecord;
import lombok.*;

/**
 * 导入任务
 *
 * @author zy
 * @date 2025-10-24 11:23
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ImportTask<T> extends BaseAsyncTask {

    /**
     * 任务唯一标识
     * 用于任务跟踪和管理
     */
    private String taskId;

    /**
     * 导入请求配置
     * 包含导出操作的所有参数
     */
    private ImportRequest<T> request;

    /**
     * 导入记录
     * 任务执行的状态和结果信息
     */
    private ImportRecord record;

    /**
     * 任务创建时间戳
     * 用于计算任务执行时间
     */
    private Long createTime;

    /**
     * 任务开始执行时间戳
     * 用于计算实际处理时间
     */
    private Long startTime;

    /**
     * 任务完成时间戳
     * 用于计算总执行时间
     */
    private Long finishTime;

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
     * 创建默认的导出任务
     *
     * @param taskId  任务ID
     * @param request 导出请求
     * @param record  导入记录
     * @param <T>     数据类型
     * @return 导出任务实例
     */
    public static <T> ImportTask<T> create(String taskId, ImportRequest<T> request, ImportRecord record) {
        long now = System.currentTimeMillis();
        return ImportTask.<T>builder()
                .taskId(taskId)
                .request(request)
                .record(record)
                .createTime(now)
                .build();
    }

    /**
     * 标记任务开始执行
     * 设置开始时间并记录执行线程
     */
    public void markStart() {
        this.startTime = System.currentTimeMillis();
        this.executeThread = Thread.currentThread();
    }

    /**
     * 标记任务完成
     * 设置完成时间
     */
    public void markFinish() {
        this.finishTime = System.currentTimeMillis();
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
        long endTime = finishTime != null ? finishTime : System.currentTimeMillis();
        return endTime - startTime;
    }

    /**
     * 检查任务是否超时
     *
     * @return 如果超时返回true，否则返回false
     */
    public boolean isTimeout() {
        if (request.getTimeout() == null) {
            return false;
        }
        return getElapsedTime() > request.getTimeout();
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


    /**
     * 校验任务参数
     *
     * @param task 导入任务
     * @throws IllegalArgumentException 如果任务参数不合法
     */
    public static void validateTaImportTask(ImportTask<?> task) {
        if (task == null) {
            throw new IllegalArgumentException("导入任务不能为null");
        }
        if (StrUtil.isBlank(task.getTaskId())) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        if (task.getRequest() == null) {
            throw new IllegalArgumentException("导入请求不能为null");
        }
        if (task.getRecord() == null) {
            throw new IllegalArgumentException("导入记录不能为null");
        }
    }
}
