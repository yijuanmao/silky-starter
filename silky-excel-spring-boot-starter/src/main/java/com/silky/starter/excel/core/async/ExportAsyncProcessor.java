package com.silky.starter.excel.core.async;

import cn.hutool.core.collection.CollectionUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.core.model.export.ExportTask;
import lombok.Getter;

/**
 * 导出异步处理器接口
 *
 * @author zy
 * @date 2025-10-27 17:50
 **/
public interface ExportAsyncProcessor extends AsyncProcessor {

    /**
     * 提交导出任务
     * 将任务提交到异步处理系统，立即返回，任务会在后台执行
     * 此方法应该是非阻塞的，提交后立即返回
     *
     * @param task 要处理的导出任务，包含任务ID、请求参数和记录信息
     */
    ExcelProcessResult submit(ExportTask<?> task) throws ExcelExportException;

    /**
     * 处理导出任务
     * 实际执行导出任务的核心方法，包含数据查询、Excel生成和文件上传
     * 此方法会阻塞当前线程直到任务完成
     *
     * @param task 要处理的导出任务
     */
    ExcelProcessResult process(ExportTask<?> task) throws ExcelExportException;

    /**
     * 批量提交导出任务
     * 同时提交多个导出任务，提高批量处理效率
     * 默认实现依次提交每个任务，子类可以重写此方法进行优化
     *
     * @param tasks 要处理的导出任务列表
     */
    default void submitBatch(java.util.List<ExportTask<?>> tasks) throws ExcelExportException {
        if (CollectionUtil.isEmpty(tasks)) {
            return;
        }
        for (ExportTask<?> task : tasks) {
            submit(task);
        }
    }

    /**
     * 批量处理导出任务
     * 同步处理多个导出任务，适用于需要严格控制资源使用的场景
     * 默认实现依次处理每个任务，子类可以重写此方法进行优化
     *
     * @param tasks 要处理的导出任务列表
     */
    default void processBatch(java.util.List<ExportTask<?>> tasks) throws ExcelExportException {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        for (ExportTask<?> task : tasks) {
            this.process(task);
        }
    }

    /**
     * 检查任务是否正在处理中
     * 用于监控任务执行状态
     *
     * @param taskId 任务ID
     * @return 如果任务正在处理中返回true，否则返回false
     */
    default boolean isTaskProcessing(String taskId) {
        return false; // 默认实现，子类应该重写此方法
    }

    /**
     * 取消正在处理的导出任务
     * 尝试取消指定任务的执行，如果任务已经开始执行可能无法取消
     *
     * @param taskId 要取消的任务ID
     * @return 如果成功取消返回true，如果任务不存在或无法取消返回false
     */
    default boolean cancelTask(String taskId) {
        return false; // 默认实现，子类应该重写此方法
    }

    /**
     * 获取任务执行状态
     * 查询指定任务的当前执行状态
     *
     * @param taskId 任务ID
     * @return 任务状态信息，如果任务不存在返回null
     */
    default TaskExecutionStatus getTaskStatus(String taskId) {
        return null; // 默认实现，子类应该重写此方法
    }

    /**
     * 获取正在处理的任务列表
     * 返回当前正在执行的所有导出任务
     *
     * @return 正在处理的任务ID列表
     */
    default java.util.List<String> getProcessingTasks() {
        return java.util.Collections.emptyList(); // 默认实现
    }

    /**
     * 获取队列中的任务数量
     * 返回等待处理的任务数量（如果适用）
     *
     * @return 队列中的任务数量
     */
    default int getQueueSize() {
        return 0; // 默认实现
    }

    /**
     * 获取处理器容量信息
     * 返回处理器的处理能力相关信息
     *
     * @return 处理器容量信息
     */
    default ProcessorCapacity getCapacity() {
        return ProcessorCapacity.unknown();
    }

    /**
     * 验证导出任务
     * 在执行前验证任务的合法性
     *
     * @param task 要验证的导出任务
     * @throws IllegalArgumentException 当任务不合法时抛出
     */
    default void validateTask(ExportTask<?> task) throws IllegalArgumentException {
        if (task == null) {
            throw new IllegalArgumentException("导出任务不能为null");
        }
        if (task.getTaskId() == null || task.getTaskId().trim().isEmpty()) {
            throw new IllegalArgumentException("导出任务ID不能为空");
        }
        if (task.getRequest() == null) {
            throw new IllegalArgumentException("导出请求不能为null");
        }
        if (task.getRecord() == null) {
            throw new IllegalArgumentException("导出记录不能为null");
        }
    }

    /**
     * 任务执行状态内部类
     */
    @Getter
    class TaskExecutionStatus {
        private final String taskId;
        private final String status;
        private final String message;
        private final Long startTime;
        private final Long endTime;
        private final Integer progress;
        private final String currentStep;

        public TaskExecutionStatus(String taskId, String status, String message,
                                   Long startTime, Long endTime, Integer progress, String currentStep) {
            this.taskId = taskId;
            this.status = status;
            this.message = message;
            this.startTime = startTime;
            this.endTime = endTime;
            this.progress = progress;
            this.currentStep = currentStep;
        }

        /**
         * 获取执行时长（毫秒）
         */
        public Long getExecutionTime() {
            if (startTime == null) return null;
            Long end = endTime != null ? endTime : System.currentTimeMillis();
            return end - startTime;
        }

        /**
         * 检查任务是否完成
         */
        public boolean isCompleted() {
            return "COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
        }

        /**
         * 检查任务是否成功
         */
        public boolean isSuccess() {
            return "COMPLETED".equals(status);
        }

        public static TaskExecutionStatus create(String taskId, String status, String message) {
            return new TaskExecutionStatus(taskId, status, message,
                    System.currentTimeMillis(), null, 0, null);
        }

        public static TaskExecutionStatus processing(String taskId, Integer progress, String currentStep) {
            return new TaskExecutionStatus(taskId, "PROCESSING", null,
                    System.currentTimeMillis(), null, progress, currentStep);
        }

        public static TaskExecutionStatus completed(String taskId) {
            long now = System.currentTimeMillis();
            return new TaskExecutionStatus(taskId, "COMPLETED", "任务执行完成",
                    now - 1000L, now, 100, "完成");
        }

        public static TaskExecutionStatus failed(String taskId, String message) {
            long now = System.currentTimeMillis();
            return new TaskExecutionStatus(taskId, "FAILED", message,
                    now - 1000L, now, 0, "失败");
        }
    }

    /**
     * 处理器容量信息内部类
     */
    @Getter
    class ProcessorCapacity {
        private final Integer maxConcurrentTasks;
        private final Integer maxQueueSize;
        private final Long maxMemoryUsage;
        private final Integer cpuCores;

        public ProcessorCapacity(Integer maxConcurrentTasks, Integer maxQueueSize,
                                 Long maxMemoryUsage, Integer cpuCores) {
            this.maxConcurrentTasks = maxConcurrentTasks;
            this.maxQueueSize = maxQueueSize;
            this.maxMemoryUsage = maxMemoryUsage;
            this.cpuCores = cpuCores;
        }

        /**
         * 创建未知容量信息
         */
        public static ProcessorCapacity unknown() {
            return new ProcessorCapacity(null, null, null, null);
        }

        /**
         * 创建标准容量信息
         */
        public static ProcessorCapacity standard() {
            return new ProcessorCapacity(10, 100, 1024L * 1024 * 1024, // 1GB
                    Runtime.getRuntime().availableProcessors());
        }

        /**
         * 创建高容量信息
         */
        public static ProcessorCapacity high() {
            return new ProcessorCapacity(50, 500, 2048L * 1024 * 1024, // 2GB
                    Runtime.getRuntime().availableProcessors());
        }

        /**
         * 检查容量是否已知
         */
        public boolean isKnown() {
            return maxConcurrentTasks != null && maxQueueSize != null &&
                    maxMemoryUsage != null && cpuCores != null;
        }

        @Override
        public String toString() {
            if (!isKnown()) {
                return "ProcessorCapacity[unknown]";
            }
            return String.format("ProcessorCapacity[concurrent=%d, queue=%d, memory=%dMB, cores=%d]",
                    maxConcurrentTasks, maxQueueSize, maxMemoryUsage / 1024 / 1024, cpuCores);
        }
    }
}
