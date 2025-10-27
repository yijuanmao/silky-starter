package com.silky.starter.excel.core.async;

import cn.hutool.core.collection.CollectionUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.imports.ImportTask;
import lombok.Getter;

/**
 * 导入异步处理器接口
 *
 * @author zy
 * @date 2025-10-27 17:50
 **/
public interface ImportAsyncProcessor extends AsyncProcessor {

    /**
     * 提交导入任务
     * 将任务提交到异步处理系统，立即返回，任务会在后台执行
     * 此方法应该是非阻塞的，提交后立即返回
     *
     * @param task 要处理的导入任务，包含任务ID、请求参数和记录信息
     */
    void submit(ImportTask<?> task) throws ExcelExportException;

    /**
     * 处理导入任务
     * 实际执行导入任务的核心方法，包含文件下载、数据读取、数据验证和数据导入
     * 此方法会阻塞当前线程直到任务完成
     *
     * @param task 要处理的导入任务
     */
    void process(ImportTask<?> task) throws ExcelExportException;

    /**
     * 批量提交导入任务
     * 同时提交多个导入任务，提高批量处理效率
     * 默认实现依次提交每个任务，子类可以重写此方法进行优化
     *
     * @param tasks 要处理的导入任务列表
     */
    default void submitBatch(java.util.List<ImportTask<?>> tasks) throws ExcelExportException {
        if (CollectionUtil.isEmpty(tasks)) {
            return;
        }
        for (ImportTask<?> task : tasks) {
            submit(task);
        }
    }

    /**
     * 批量处理导入任务
     * 同步处理多个导入任务，适用于需要严格控制资源使用的场景
     * 默认实现依次处理每个任务，子类可以重写此方法进行优化
     *
     * @param tasks 要处理的导入任务列表
     */
    default void processBatch(java.util.List<ImportTask<?>> tasks) throws ExcelExportException {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        for (ImportTask<?> task : tasks) {
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
     * 取消正在处理的导入任务
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
     * 返回当前正在执行的所有导入任务
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
     * 验证导入任务
     * 在执行前验证任务的合法性
     *
     * @param task 要验证的导入任务
     * @throws IllegalArgumentException 当任务不合法时抛出
     */
    default void validateTask(ImportTask<?> task) throws IllegalArgumentException {
        if (task == null) {
            throw new IllegalArgumentException("导入任务不能为null");
        }
        if (task.getTaskId() == null || task.getTaskId().trim().isEmpty()) {
            throw new IllegalArgumentException("导入任务ID不能为空");
        }
        if (task.getRequest() == null) {
            throw new IllegalArgumentException("导入请求不能为null");
        }
        if (task.getRecord() == null) {
            throw new IllegalArgumentException("导入记录不能为null");
        }
        // 验证导入请求的必要字段
        if (task.getRequest().getFileUrl() == null || task.getRequest().getFileUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("导入文件URL不能为空");
        }
        if (task.getRequest().getDataClass() == null) {
            throw new IllegalArgumentException("导入数据类不能为null");
        }
    }

    /**
     * 预检查导入任务
     * 在执行前检查任务的可行性，如文件是否存在、格式是否支持等
     *
     * @param task 要检查的导入任务
     * @return 预检查结果
     */
    default PreCheckResult preCheckTask(ImportTask<?> task) {
        try {
            validateTask(task);
            return PreCheckResult.success("预检查通过");
        } catch (Exception e) {
            return PreCheckResult.failed(e.getMessage());
        }
    }

    /**
     * 获取导入统计信息
     * 返回处理器的导入任务统计信息
     *
     * @return 导入统计信息
     */
    default ImportStatistics getImportStatistics() {
        return new ImportStatistics(0, 0, 0, 0);
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
        private final Long processedRows;
        private final Long totalRows;
        private final Long successRows;
        private final Long failedRows;

        public TaskExecutionStatus(String taskId, String status, String message,
                                   Long startTime, Long endTime, Integer progress, String currentStep,
                                   Long processedRows, Long totalRows, Long successRows, Long failedRows) {
            this.taskId = taskId;
            this.status = status;
            this.message = message;
            this.startTime = startTime;
            this.endTime = endTime;
            this.progress = progress;
            this.currentStep = currentStep;
            this.processedRows = processedRows;
            this.totalRows = totalRows;
            this.successRows = successRows;
            this.failedRows = failedRows;
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
         * 获取成功率
         */
        public Double getSuccessRate() {
            if (processedRows == null || processedRows == 0) return 0.0;
            return successRows != null ? (double) successRows / processedRows : 0.0;
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
                    System.currentTimeMillis(), null, 0, null, 0L, 0L, 0L, 0L);
        }

        public static TaskExecutionStatus processing(String taskId, Integer progress, String currentStep,
                                                     Long processedRows, Long totalRows, Long successRows, Long failedRows) {
            return new TaskExecutionStatus(taskId, "PROCESSING", null,
                    System.currentTimeMillis(), null, progress, currentStep,
                    processedRows, totalRows, successRows, failedRows);
        }

        public static TaskExecutionStatus completed(String taskId, Long totalRows, Long successRows) {
            long now = System.currentTimeMillis();
            return new TaskExecutionStatus(taskId, "COMPLETED", "导入任务执行完成",
                    now - 1000L, now, 100, "完成", totalRows, totalRows, successRows, totalRows - successRows);
        }

        public static TaskExecutionStatus failed(String taskId, String message) {
            long now = System.currentTimeMillis();
            return new TaskExecutionStatus(taskId, "FAILED", message,
                    now - 1000L, now, 0, "失败", 0L, 0L, 0L, 0L);
        }
    }

    /**
     * 预检查结果内部类
     */
    @Getter
    class PreCheckResult {
        private final boolean success;
        private final String message;
        private final String details;

        public PreCheckResult(boolean success, String message, String details) {
            this.success = success;
            this.message = message;
            this.details = details;
        }

        public static PreCheckResult success(String message) {
            return new PreCheckResult(true, message, null);
        }

        public static PreCheckResult failed(String message) {
            return new PreCheckResult(false, message, null);
        }

        public static PreCheckResult failed(String message, String details) {
            return new PreCheckResult(false, message, details);
        }
    }

    /**
     * 导入统计信息内部类
     */
    @Getter
    class ImportStatistics {
        private final long totalTasks;
        private final long successTasks;
        private final long failedTasks;
        private final long totalRows;

        public ImportStatistics(long totalTasks, long successTasks, long failedTasks, long totalRows) {
            this.totalTasks = totalTasks;
            this.successTasks = successTasks;
            this.failedTasks = failedTasks;
            this.totalRows = totalRows;
        }

        /**
         * 获取成功率
         */
        public double getSuccessRate() {
            return totalTasks > 0 ? (double) successTasks / totalTasks : 0.0;
        }

        /**
         * 获取失败率
         */
        public double getFailedRate() {
            return totalTasks > 0 ? (double) failedTasks / totalTasks : 0.0;
        }

        /**
         * 获取平均每任务行数
         */
        public double getAverageRowsPerTask() {
            return totalTasks > 0 ? (double) totalRows / totalTasks : 0.0;
        }

        @Override
        public String toString() {
            return String.format("ImportStatistics[total=%d, success=%d, failed=%d, rows=%d, successRate=%.2f%%]",
                    totalTasks, successTasks, failedTasks, totalRows, getSuccessRate() * 100);
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
        private final Long maxFileSize;

        public ProcessorCapacity(Integer maxConcurrentTasks, Integer maxQueueSize,
                                 Long maxMemoryUsage, Integer cpuCores, Long maxFileSize) {
            this.maxConcurrentTasks = maxConcurrentTasks;
            this.maxQueueSize = maxQueueSize;
            this.maxMemoryUsage = maxMemoryUsage;
            this.cpuCores = cpuCores;
            this.maxFileSize = maxFileSize;
        }

        /**
         * 创建未知容量信息
         */
        public static ProcessorCapacity unknown() {
            return new ProcessorCapacity(null, null, null, null, null);
        }

        /**
         * 创建标准容量信息
         */
        public static ProcessorCapacity standard() {
            return new ProcessorCapacity(5, 50, 512L * 1024 * 1024, // 512MB
                    Runtime.getRuntime().availableProcessors(), 100L * 1024 * 1024); // 100MB
        }

        /**
         * 创建高容量信息
         */
        public static ProcessorCapacity high() {
            return new ProcessorCapacity(20, 200, 1024L * 1024 * 1024, // 1GB
                    Runtime.getRuntime().availableProcessors(), 500L * 1024 * 1024); // 500MB
        }

        /**
         * 检查容量是否已知
         */
        public boolean isKnown() {
            return maxConcurrentTasks != null && maxQueueSize != null &&
                    maxMemoryUsage != null && cpuCores != null && maxFileSize != null;
        }

        @Override
        public String toString() {
            if (!isKnown()) {
                return "ProcessorCapacity[unknown]";
            }
            return String.format("ProcessorCapacity[concurrent=%d, queue=%d, memory=%dMB, cores=%d, maxFile=%dMB]",
                    maxConcurrentTasks, maxQueueSize, maxMemoryUsage / 1024 / 1024,
                    cpuCores, maxFileSize / 1024 / 1024);
        }
    }
}
