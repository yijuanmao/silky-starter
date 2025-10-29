package com.silky.starter.excel.core.async.executor;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.silky.starter.excel.core.async.*;
import com.silky.starter.excel.core.async.factory.AsyncProcessorFactory;
import com.silky.starter.excel.core.async.model.ProcessorStatus;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.TaskType;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * * 异步执行器，
 * 统一的任务提交入口，根据配置自动选择合适的异步处理器，
 * 提供任务提交、状态监控和降级处理功能
 *
 * @author zy
 * @date 2025-10-24 13:56
 **/
public class AsyncExecutor implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AsyncExecutor.class);

    /**
     * 总提交任务计数器
     */
    private final AtomicLong totalSubmittedTasks = new AtomicLong(0);

    /**
     * 成功提交任务计数器
     */
    private final AtomicLong successSubmittedTasks = new AtomicLong(0);

    /**
     * 降级处理任务计数器
     */
    private final AtomicLong fallbackTasks = new AtomicLong(0);

    /**
     * 按任务类型统计的计数器
     */
    private final Map<TaskType, AtomicLong> taskTypeCounters = new ConcurrentHashMap<>(4);

    /**
     * 按异步类型统计的计数器
     */
    private final Map<AsyncType, AtomicLong> asyncTypeCounters = new ConcurrentHashMap<>(9);

    private final AsyncType defaultAsyncType;

    /**
     * 执行器启动时间
     */
    private final long executorStartTime = System.currentTimeMillis();

    private final AsyncProcessorFactory processorFactory;

    private final SilkyExcelProperties properties;

    public AsyncExecutor(AsyncProcessorFactory processorFactory,
                         SilkyExcelProperties properties) {
        this.processorFactory = processorFactory;
        this.properties = properties;
        this.defaultAsyncType = properties.getAsync().getDefaultType();

        // 初始化统计计数器
        for (TaskType taskType : TaskType.values()) {
            taskTypeCounters.put(taskType, new AtomicLong(0));
        }
        for (AsyncType asyncType : AsyncType.values()) {
            asyncTypeCounters.put(asyncType, new AtomicLong(0));
        }
    }

    @Override
    public void afterPropertiesSet() {
        this.init();
    }

    /**
     * 初始化方法
     */
    public void init() {
        log.info("开始初始化异步执行器...");

        // 检查处理器工厂状态
        if (!processorFactory.isInitialized()) {
            log.warn("异步处理器工厂未初始化，异步执行器可能无法正常工作");
        }
        // 检查默认处理器是否可用
        boolean defaultAvailable = checkDefaultProcessorAvailable(defaultAsyncType);
        if (!defaultAvailable) {
            log.warn("默认异步处理器不可用: {}, 将使用降级策略", defaultAsyncType);
        }
        log.info("异步执行器初始化完成，默认异步类型: {}, 可用状态: {}", defaultAsyncType, defaultAvailable);
    }


    /**
     * 统一提交任务 - 指定异步方式
     *
     * @param task      异步任务
     * @param asyncType 异步处理类型，如果为null则使用配置的默认类型
     */
    public ExcelProcessResult submit(AsyncTask task, AsyncType asyncType) {
        // 参数校验
        validateAsyncTask(task);

        // 增加总提交计数
        totalSubmittedTasks.incrementAndGet();

        // 更新任务类型统计
        taskTypeCounters.get(task.getTaskType()).incrementAndGet();

        // 检查异步处理是否启用
        if (!properties.getAsync().isEnabled()) {
            log.debug("异步处理被禁用，使用同步方式执行任务: {}", task.getTaskId());
            return processSync(task);
        }
        // 确定目标异步类型
        AsyncType targetType = determineAsyncType(asyncType);

        // 更新异步类型统计
        asyncTypeCounters.get(targetType).incrementAndGet();
        try {
            // 优先使用统一处理器
            if (useUnifiedProcessor(task, targetType)) {
                return submitWithUnifiedProcessor(task, targetType);
            } else {
                // 使用专门的处理器
                return submitWithSpecializedProcessor(task, targetType);
            }

        } catch (Exception e) {
            log.error("任务提交失败: {}, 错误: {}, 降级为同步执行",
                    task.getTaskId(), e.getMessage());
            fallbackTasks.incrementAndGet();
            return processSync(task);
        }
    }


    // ==================== 专门任务提交方法 ====================

    /**
     * 提交导出任务 - 使用默认异步方式
     *
     * @param task 导出任务
     */
    public void submitExport(ExportTask<?> task) {
        submitExport(task, null);
    }

    /**
     * 提交导出任务 - 指定异步方式
     *
     * @param task      导出任务
     * @param asyncType 异步处理类型
     */
    public void submitExport(ExportTask<?> task, AsyncType asyncType) {
        validateExportTask(task);
        submitExportTask(task, determineAsyncType(asyncType));
    }

    /**
     * 提交导入任务 - 指定异步方式
     *
     * @param task      导入任务
     * @param asyncType 异步处理类型
     */
    public ExcelProcessResult submitImport(ImportTask<?> task, AsyncType asyncType) {
        validateImportTask(task);
        return this.submitImportTask(task, determineAsyncType(asyncType));
    }

    /**
     * 提交导出任务的具体实现
     */
    private ExcelProcessResult submitExportTask(ExportTask<?> task, AsyncType asyncType) {
        totalSubmittedTasks.incrementAndGet();
        taskTypeCounters.get(TaskType.EXPORT).incrementAndGet();
        asyncTypeCounters.get(asyncType).incrementAndGet();

        if (!properties.getAsync().isEnabled()) {
            log.debug("异步导出被禁用，使用同步方式执行任务: {}", task.getTaskId());
            processExportSync(task);
            return ExcelProcessResult.fail(task.getTaskId(), "任务被取消");
        }

        try {
            ExportAsyncProcessor processor = processorFactory.getExportProcessor(asyncType);

            if (!processor.isAvailable()) {
                log.warn("导出处理器 {} 不可用，任务: {}, 降级为同步执行", asyncType, task.getTaskId());
                fallbackTasks.incrementAndGet();
                return processExportSync(task);
            }

            ExcelProcessResult result = processor.submit(task);
            successSubmittedTasks.incrementAndGet();

            log.debug("导出任务提交成功: {}, 处理器: {}", task.getTaskId(), asyncType);
            return result;
        } catch (Exception e) {
            log.error("导出处理器 {} 提交任务失败: {}, 错误: {}, 降级为同步执行",
                    asyncType, task.getTaskId(), e.getMessage());
            fallbackTasks.incrementAndGet();
            return processExportSync(task);
        }
    }

    /**
     * 提交导入任务的具体实现
     */
    private ExcelProcessResult submitImportTask(ImportTask<?> task, AsyncType asyncType) {
        totalSubmittedTasks.incrementAndGet();
        taskTypeCounters.get(TaskType.IMPORT).incrementAndGet();
        asyncTypeCounters.get(asyncType).incrementAndGet();

        if (!properties.getAsync().isEnabled()) {
            log.debug("异步导入被禁用，使用同步方式执行任务: {}", task.getTaskId());
            return processImportSync(task);
        }
        try {
            ImportAsyncProcessor processor = processorFactory.getImportProcessor(asyncType);

            if (!processor.isAvailable()) {
                log.warn("导入处理器 {} 不可用，任务: {}, 降级为同步执行", asyncType, task.getTaskId());
                fallbackTasks.incrementAndGet();
                return processImportSync(task);
            }

            ExcelProcessResult result = processor.submit(task);
            successSubmittedTasks.incrementAndGet();

            log.debug("导入任务提交成功: {}, 处理器: {}", task.getTaskId(), asyncType);

            return result;
        } catch (Exception e) {
            log.error("导入处理器 {} 提交任务失败: {}, 错误: {}, 降级为同步执行",
                    asyncType, task.getTaskId(), e.getMessage());
            fallbackTasks.incrementAndGet();
            return processImportSync(task);
        }
    }

    /**
     * 判断是否使用统一处理器
     */
    private boolean useUnifiedProcessor(AsyncTask task, AsyncType asyncType) {
        try {
            UnifiedAsyncProcessor processor = processorFactory.getUnifiedProcessor(asyncType);
            return processor.isAvailable() && processor.supports(task.getTaskType());
        } catch (Exception e) {
            log.debug("统一处理器不可用或不支持该任务类型: {}, {}", asyncType, task.getTaskType());
            return false;
        }
    }

    /**
     * 使用统一处理器提交任务
     */
    private ExcelProcessResult submitWithUnifiedProcessor(AsyncTask task, AsyncType asyncType) {
        UnifiedAsyncProcessor processor = processorFactory.getUnifiedProcessor(asyncType);

        if (!processor.isAvailable()) {
            throw new ExcelExportException("统一处理器不可用: " + asyncType);
        }

        ExcelProcessResult result = processor.submit(task);
        successSubmittedTasks.incrementAndGet();

        log.debug("任务提交成功(统一处理器): {}, 类型: {}, 处理器: {}",
                task.getTaskId(), task.getTaskType(), asyncType);

        return result;
    }

    /**
     * 使用专门处理器提交任务
     */
    private ExcelProcessResult submitWithSpecializedProcessor(AsyncTask task, AsyncType asyncType) {
        if (task.getTaskType() == TaskType.EXPORT) {
            return submitExportTask((ExportTask<?>) task, asyncType);
        } else if (task.getTaskType() == TaskType.IMPORT) {
            return submitImportTask((ImportTask<?>) task, asyncType);
        } else {
            throw new IllegalArgumentException("不支持的任务类型: " + task.getTaskType());
        }
    }

    // ==================== 同步处理方法 ====================

    /**
     * 统一同步处理任务
     */
    private ExcelProcessResult processSync(AsyncTask task) {
        try {
            if (task.getTaskType() == TaskType.EXPORT) {
                return processExportSync((ExportTask<?>) task);
            } else if (task.getTaskType() == TaskType.IMPORT) {
                return processImportSync((ImportTask<?>) task);
            } else {
                throw new IllegalArgumentException("不支持的任务类型: " + task.getTaskType());
            }
        } catch (Exception e) {
            log.error("同步处理任务失败: {}", task.getTaskId(), e);
            return ExcelProcessResult.fail(task.getTaskId(), "任务处理失败: " + e.getMessage());
        }
    }

    /**
     * 同步处理导出任务
     *
     * @param task 导出任务
     */
    private ExcelProcessResult processExportSync(ExportTask<?> task) {
        try {
            ExportAsyncProcessor syncProcessor = processorFactory.getExportProcessor(AsyncType.SYNC);
            ExcelProcessResult result = syncProcessor.process(task);
            log.debug("同步导出任务处理完成: {}", task.getTaskId());
            return result;
        } catch (Exception e) {
            log.error("同步处理导出任务失败: {}", task.getTaskId(), e);
            return ExcelProcessResult.fail(task.getTaskId(), "导出任务处理失败: " + e.getMessage());
        }
    }

    /**
     * 同步处理导入任务
     */
    private ExcelProcessResult processImportSync(ImportTask<?> task) {
        try {
            ImportAsyncProcessor syncProcessor = processorFactory.getImportProcessor(AsyncType.SYNC);
            ExcelProcessResult result = syncProcessor.process(task);
            log.debug("同步导入任务处理完成: {}", task.getTaskId());
            return result;
        } catch (Exception e) {
            log.error("同步处理导入任务失败: {}", task.getTaskId(), e);
            throw new ExcelExportException("导入任务处理失败: " + e.getMessage(), e);
        }
    }

    // ==================== 处理器管理方法 ====================

    /**
     * 获取所有处理器状态
     */
    public Map<String, ProcessorStatus> getProcessorStatus() {
        return processorFactory.getAllProcessorStatus();
    }

    /**
     * 获取指定处理器的状态
     */
    public ProcessorStatus getProcessorStatus(String type) {
        try {
            // 尝试获取统一处理器
            UnifiedAsyncProcessor unifiedProcessor = processorFactory.getUnifiedProcessor(type);
            return unifiedProcessor.getStatus();
        } catch (Exception e) {
            // 忽略，继续尝试其他类型
        }

        try {
            // 尝试获取导出处理器
            ExportAsyncProcessor exportProcessor = processorFactory.getExportProcessor(type);
            return exportProcessor.getStatus();
        } catch (Exception e) {
            // 忽略，继续尝试其他类型
        }

        try {
            // 尝试获取导入处理器
            ImportAsyncProcessor importProcessor = processorFactory.getImportProcessor(type);
            return importProcessor.getStatus();
        } catch (Exception e) {
            log.warn("获取处理器状态失败: {}", type, e);
            return null;
        }
    }

    /**
     * 获取指定异步类型的处理器状态
     */
    public ProcessorStatus getProcessorStatus(AsyncType asyncType) {
        return getProcessorStatus(asyncType.name());
    }

    /**
     * 注册自定义处理器
     */
    public void registerProcessor(AsyncProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("处理器不能为null");
        }

        try {
            if (processor instanceof UnifiedAsyncProcessor) {
                processorFactory.registerUnifiedProcessor((UnifiedAsyncProcessor) processor);
            } else if (processor instanceof ExportAsyncProcessor) {
                processorFactory.registerExportProcessor((ExportAsyncProcessor) processor);
            } else if (processor instanceof ImportAsyncProcessor) {
                processorFactory.registerImportProcessor((ImportAsyncProcessor) processor);
            } else {
                throw new IllegalArgumentException("不支持的处理器类型: " + processor.getClass().getName());
            }
        } catch (Exception e) {
            log.error("自定义处理器注册失败: {}", processor.getType(), e);
            throw new ExcelExportException("处理器注册失败: " + e.getMessage(), e);
        }
    }

    /**
     * 注销处理器
     */
    public boolean unregisterProcessor(String type) {
        return processorFactory.unregisterProcessor(type);
    }

    /**
     * 注销指定异步类型的处理器
     */
    public boolean unregisterProcessor(AsyncType asyncType) {
        return unregisterProcessor(asyncType.name());
    }

    /**
     * 检查处理器是否可用
     */
    public boolean isProcessorAvailable(String type) {
        return processorFactory.isProcessorAvailable(type);
    }

    /**
     * 检查指定异步类型的处理器是否可用
     */
    public boolean isProcessorAvailable(AsyncType asyncType) {
        return isProcessorAvailable(asyncType.name());
    }

    /**
     * 获取所有可用的处理器类型
     */
    public List<String> getAvailableTypes() {
        return processorFactory.getAvailableTypes();
    }

    // ==================== 状态监控方法 ====================

    /**
     * 获取执行器状态信息
     */
    public ExecutorStatus getExecutorStatus() {
        Map<String, ProcessorStatus> processorStatus = getProcessorStatus();

        // 计算任务类型分布
        Map<TaskType, Long> taskTypeDistribution = new HashMap<>(taskTypeCounters.size() + 2);
        for (Map.Entry<TaskType, AtomicLong> entry : taskTypeCounters.entrySet()) {
            taskTypeDistribution.put(entry.getKey(), entry.getValue().get());
        }

        // 计算异步类型分布
        Map<AsyncType, Long> asyncTypeDistribution = new HashMap<>(asyncTypeCounters.size() + 2);
        for (Map.Entry<AsyncType, AtomicLong> entry : asyncTypeCounters.entrySet()) {
            asyncTypeDistribution.put(entry.getKey(), entry.getValue().get());
        }

        return ExecutorStatus.builder()
                .totalSubmittedTasks(totalSubmittedTasks.get())
                .successSubmittedTasks(successSubmittedTasks.get())
                .fallbackTasks(fallbackTasks.get())
                .processorCount(processorStatus.size())
                .availableProcessorCount(getAvailableProcessorCount(processorStatus))
                .taskTypeDistribution(taskTypeDistribution)
                .asyncTypeDistribution(asyncTypeDistribution)
                .startTime(LocalDateTimeUtil.ofUTC(executorStartTime))
                .uptime(System.currentTimeMillis() - executorStartTime)
                .build();
    }

    /**
     * 获取执行器健康状态
     */
    public HealthStatus getHealthStatus() {
        ExecutorStatus status = getExecutorStatus();

        // 检查处理器可用性
        boolean processorsHealthy = status.getProcessorAvailableRate() > 0.5;

        // 检查提交成功率
        boolean submitHealthy = status.getSubmitSuccessRate() > 0.8;

        // 检查降级率
        boolean fallbackHealthy = status.getFallbackRate() < 0.2;

        HealthStatus.HealthLevel level;
        String message;

        if (processorsHealthy && submitHealthy && fallbackHealthy) {
            level = HealthStatus.HealthLevel.HEALTHY;
            message = "异步执行器运行正常";
        } else if (submitHealthy && fallbackHealthy) {
            level = HealthStatus.HealthLevel.DEGRADED;
            message = "异步执行器运行降级，部分处理器不可用";
        } else {
            level = HealthStatus.HealthLevel.UNHEALTHY;
            message = "异步执行器运行异常，提交失败率或降级率过高";
        }

        return HealthStatus.builder()
                .level(level)
                .message(message)
                .executorStatus(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 获取任务统计信息
     */
    public TaskStatistics getTaskStatistics() {
        return TaskStatistics.builder()
                .totalSubmittedTasks(totalSubmittedTasks.get())
                .successSubmittedTasks(successSubmittedTasks.get())
                .fallbackTasks(fallbackTasks.get())
                .exportTasks(taskTypeCounters.get(TaskType.EXPORT).get())
                .importTasks(taskTypeCounters.get(TaskType.IMPORT).get())
                .asyncTypeDistribution(new HashMap<>(asyncTypeCounters))
                .startTime(LocalDateTimeUtil.ofUTC(executorStartTime))
                .build();
    }

    /**
     * 重置统计信息（主要用于测试）
     */
    public void resetStatistics() {
        totalSubmittedTasks.set(0);
        successSubmittedTasks.set(0);
        fallbackTasks.set(0);

        for (AtomicLong counter : taskTypeCounters.values()) {
            counter.set(0);
        }

        for (AtomicLong counter : asyncTypeCounters.values()) {
            counter.set(0);
        }

        log.info("执行器统计信息已重置");
    }

    // ==================== 工具方法 ====================

    /**
     * 确定异步处理类型
     */
    private AsyncType determineAsyncType(AsyncType asyncType) {
        // 优先使用参数指定的类型
        if (asyncType != null) {
            return asyncType;
        }
        // 其次使用全局默认配置
        return defaultAsyncType;
    }

    /**
     * 获取可用处理器数量
     */
    private long getAvailableProcessorCount(Map<String, ProcessorStatus> processorStatus) {
        return processorStatus.values().stream()
                .filter(ProcessorStatus::isAvailable)
                .count();
    }

    /**
     * 验证异步任务
     */
    private void validateAsyncTask(AsyncTask task) {
        if (task == null) {
            throw new IllegalArgumentException("异步任务不能为null");
        }
        task.validate();
    }

    /**
     * 验证导出任务
     */
    private void validateExportTask(ExportTask<?> task) {
        if (task == null) {
            throw new IllegalArgumentException("导出任务不能为null");
        }
        task.validate();
    }

    /**
     * 验证导入任务
     */
    private void validateImportTask(ImportTask<?> task) {
        if (task == null) {
            throw new IllegalArgumentException("导入任务不能为null");
        }
        task.validate();
    }

    // ==================== 生命周期方法 ====================

    /**
     * 销毁方法
     */
    @PreDestroy
    public void destroy() {
        log.info("开始关闭异步执行器...");

        // 打印最终统计信息
        ExecutorStatus finalStatus = getExecutorStatus();
        log.info("异步执行器关闭统计 - 总提交任务: {}, 成功提交: {}, 降级处理: {}, 提交成功率: {:.2f}%",
                finalStatus.getTotalSubmittedTasks(),
                finalStatus.getSuccessSubmittedTasks(),
                finalStatus.getFallbackTasks(),
                finalStatus.getSubmitSuccessRate() * 100);

        log.info("异步执行器关闭完成");
    }

    /**
     * 检查默认处理器是否可用
     */
    private boolean checkDefaultProcessorAvailable(AsyncType defaultType) {
        try {
            // 尝试获取统一处理器
            processorFactory.getUnifiedProcessor(defaultType);
            return true;
        } catch (Exception e) {
            log.debug("默认统一处理器不可用: {}", defaultType);
        }

        try {
            // 尝试获取导出处理器
            processorFactory.getExportProcessor(defaultType);
            return true;
        } catch (Exception e) {
            log.debug("默认导出处理器不可用: {}", defaultType);
        }

        try {
            // 尝试获取导入处理器
            processorFactory.getImportProcessor(defaultType);
            return true;
        } catch (Exception e) {
            log.debug("默认导入处理器不可用: {}", defaultType);
        }

        return false;
    }


    // ==================== 内部状态类 ====================

    /**
     * 执行器状态内部类
     */
    @Data
    @Builder
    public static class ExecutorStatus {
        private long totalSubmittedTasks;
        private long successSubmittedTasks;
        private long fallbackTasks;
        private int processorCount;
        private long availableProcessorCount;
        private Map<TaskType, Long> taskTypeDistribution;
        private Map<AsyncType, Long> asyncTypeDistribution;
        private LocalDateTime startTime;
        private long uptime;

        public double getSubmitSuccessRate() {
            return totalSubmittedTasks > 0 ?
                    (double) successSubmittedTasks / totalSubmittedTasks : 0.0;
        }

        public double getFallbackRate() {
            return totalSubmittedTasks > 0 ?
                    (double) fallbackTasks / totalSubmittedTasks : 0.0;
        }

        public double getProcessorAvailableRate() {
            return processorCount > 0 ?
                    (double) availableProcessorCount / processorCount : 0.0;
        }

        public double getAverageSubmitRate() {
            long minutes = uptime / (60 * 1000);
            return minutes > 0 ? (double) totalSubmittedTasks / minutes : 0.0;
        }

        public String getStatusDescription() {
            return String.format(
                    "执行器状态: 总任务=%d, 成功率=%.1f%%, 降级率=%.1f%%, 处理器=%d/%d (%.1f%%)",
                    totalSubmittedTasks,
                    getSubmitSuccessRate() * 100,
                    getFallbackRate() * 100,
                    availableProcessorCount, processorCount,
                    getProcessorAvailableRate() * 100
            );
        }
    }

    /**
     * 健康状态内部类
     */
    @Data
    @Builder
    public static class HealthStatus {

        /**
         * 健康等级
         */
        private HealthLevel level;

        /**
         * 健康信息
         */
        private String message;

        /**
         * 执行器状态
         */
        private ExecutorStatus executorStatus;

        /**
         * 时间戳
         */
        private LocalDateTime timestamp;

        public enum HealthLevel {
            HEALTHY, DEGRADED, UNHEALTHY, OFFLINE
        }

        public boolean isHealthy() {
            return level == HealthLevel.HEALTHY;
        }

        public String getHealthDescription() {
            return String.format("健康状态: %s - %s", level, message);
        }
    }

    /**
     * 任务统计信息
     */
    @Data
    @Builder
    public static class TaskStatistics {

        /**
         * 总提交任务数
         */
        private long totalSubmittedTasks;

        /**
         * 提交成功任务数
         */
        private long successSubmittedTasks;

        /**
         * 降级处理任务数
         */
        private long fallbackTasks;

        /**
         * 导出任务数
         */
        private long exportTasks;

        /**
         * 导入任务数
         */
        private long importTasks;

        /**
         * 异步任务数
         */
        private Map<AsyncType, AtomicLong> asyncTypeDistribution;

        /**
         * 执行器启动时间
         */
        private LocalDateTime startTime;

        public double getExportRatio() {
            return totalSubmittedTasks > 0 ? (double) exportTasks / totalSubmittedTasks : 0.0;
        }

        public double getImportRatio() {
            return totalSubmittedTasks > 0 ? (double) importTasks / totalSubmittedTasks : 0.0;
        }

        public Map<AsyncType, Long> getAsyncTypeDistributionCounts() {
            Map<AsyncType, Long> distribution = new HashMap<>();
            for (Map.Entry<AsyncType, AtomicLong> entry : asyncTypeDistribution.entrySet()) {
                distribution.put(entry.getKey(), entry.getValue().get());
            }
            return distribution;
        }
    }

}
