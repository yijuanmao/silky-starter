package com.silky.starter.excel.core.async;

import com.silky.starter.excel.core.async.factory.AsyncProcessorFactory;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * * 异步执行器，统一的任务提交入口，根据配置自动选择合适的异步处理器，提供任务提交、状态监控和降级处理功能
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
     * 执行器启动时间
     */
    private final long executorStartTime = System.currentTimeMillis();

    private final AsyncProcessorFactory processorFactory;

    private final SilkyExcelProperties properties;

    public AsyncExecutor(AsyncProcessorFactory processorFactory,
                         SilkyExcelProperties properties) {
        this.processorFactory = processorFactory;
        this.properties = properties;
    }

    /**
     * 初始化方法
     * 检查异步处理器工厂状态和配置
     */
    public void init() {
        log.info("开始初始化异步执行器...");

        // 检查处理器工厂状态
        if (!processorFactory.isInitialized()) {
            log.warn("异步处理器工厂未初始化，异步执行器可能无法正常工作");
        }

        // 检查默认处理器是否可用
        AsyncType defaultType = properties.getAsync().getDefaultType();
        boolean defaultAvailable = processorFactory.isProcessorAvailable(defaultType.name());

        if (!defaultAvailable) {
            log.warn("默认异步处理器不可用: {}, 将使用降级策略", defaultType);
        }

        log.info("异步执行器初始化完成，默认异步类型: {}, 可用状态: {}", defaultType, defaultAvailable);
    }

    /**
     * 提交导出任务 - 使用配置的默认异步方式，这是最常用的任务提交方法
     *
     * @param task 导出任务，包含任务ID、请求参数和记录信息
     */
    public void submitExport(ExportTask<?> task) {
        this.submitExport(task, null);
    }

    /**
     * 提交导出任务 - 指定异步方式，允许调用方覆盖默认的异步处理方式
     *
     * @param task      导出任务
     * @param asyncType 异步处理类型，如果为null则使用配置的默认类型
     */
    public void submitExport(ExportTask<?> task, AsyncType asyncType) {
        // 参数校验
        ExportTask.validateTaExportTask(task);

        // 增加总提交计数
        totalSubmittedTasks.incrementAndGet();

        // 检查异步导出是否启用
        if (!properties.getAsync().isEnabled()) {
            log.debug("异步导出被禁用，使用同步方式执行任务: {}", task.getTaskId());
            processSync(task);
            return;
        }

        // 确定目标异步类型
        AsyncType targetType = determineAsyncType(asyncType);

        try {
            // 获取对应的异步处理器
            AsyncProcessor processor = processorFactory.getProcessor(targetType);

            // 检查处理器是否可用
            if (!processor.isAvailable()) {
                log.warn("处理器 {} 不可用，任务: {}, 降级为同步执行",
                        targetType, task.getTaskId());
                fallbackTasks.incrementAndGet();
                processSync(task);
                return;
            }

            // 提交任务到处理器
            processor.submit(task);
            successSubmittedTasks.incrementAndGet();

            log.debug("任务提交成功: {}, 处理器: {}", task.getTaskId(), targetType);

        } catch (Exception e) {
            log.error("处理器 {} 提交任务失败: {}, 错误: {}, 降级为同步执行",
                    targetType, task.getTaskId(), e.getMessage());

            fallbackTasks.incrementAndGet();
            processSync(task);
        }
    }


    /**
     * 提交导入任务 - 指定异步方式，允许调用方覆盖默认的异步处理方式
     *
     * @param task      导入任务
     * @param asyncType 异步处理类型，如果为null则使用配置的默认类型
     */
    public void submitImport(ImportTask<?> task, AsyncType asyncType) {
        // 参数校验
        ImportTask.validateTaImportTask(task);

        // 增加总提交计数
        totalSubmittedTasks.incrementAndGet();

        // 检查异步导出是否启用
        if (!properties.getAsync().isEnabled()) {
            log.debug("异步导出被禁用，使用同步方式执行任务: {}", task.getTaskId());
            processSync(task);
            return;
        }

        // 确定目标异步类型
        AsyncType targetType = determineAsyncType(asyncType);

        try {
            // 获取对应的异步处理器
            AsyncProcessor processor = processorFactory.getProcessor(targetType);

            // 检查处理器是否可用
            if (!processor.isAvailable()) {
                log.warn("处理器 {} 不可用，任务: {}, 降级为同步执行",
                        targetType, task.getTaskId());
                fallbackTasks.incrementAndGet();
                processSync(task);
                return;
            }

            // 提交任务到处理器
            processor.submit(task);
            successSubmittedTasks.incrementAndGet();

            log.debug("任务提交成功: {}, 处理器: {}", task.getTaskId(), targetType);

        } catch (Exception e) {
            log.error("处理器 {} 提交任务失败: {}, 错误: {}, 降级为同步执行",
                    targetType, task.getTaskId(), e.getMessage());

            fallbackTasks.incrementAndGet();
            processSync(task);
        }
    }

    /**
     * 同步处理任务
     * 在调用者线程中立即执行任务，用于降级处理或同步场景
     *
     * @param task 导出任务
     */
    private void processExportSync(ExportTask<?> task) {
        try {
            // 获取同步处理器
            AsyncProcessor syncProcessor = processorFactory.getProcessor(AsyncType.SYNC);

            // 直接处理任务（同步执行）
            syncProcessor.process(task);

            log.debug("同步任务处理完成: {}", task.getTaskId());

        } catch (Exception e) {
            log.error("同步处理任务失败: {}", task.getTaskId(), e);
            throw new ExcelExportException("任务处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 同步处理任务 在调用者线程中立即执行任务，用于降级处理或同步场景
     *
     * @param task 导入任务
     */
    private void processImportSync(ImportTask<?> task) {
        try {
            // 获取同步处理器
            AsyncProcessor syncProcessor = processorFactory.getProcessor(AsyncType.SYNC);

            // 直接处理任务（同步执行）
            syncProcessor.process(task);

            log.debug("同步任务处理完成: {}", task.getTaskId());

        } catch (Exception e) {
            log.error("同步处理任务失败: {}", task.getTaskId(), e);
            throw new ExcelExportException("任务处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取所有处理器状态
     * 用于监控和管理所有异步处理器的运行状态
     *
     * @return 处理器类型到状态的映射表
     * <p>
     * 使用示例：
     * {@code
     * Map<String, ProcessorStatus> statusMap = asyncExecutor.getProcessorStatus();
     * statusMap.forEach((type, status) -> {
     * System.out.println(type + ": " + status.isAvailable());
     * });
     * }
     */
    public Map<String, ProcessorStatus> getProcessorStatus() {
        return processorFactory.getAllProcessorStatus();
    }

    /**
     * 获取指定处理器的状态
     *
     * @param type 处理器类型
     * @return 处理器状态，如果处理器不存在返回null
     */
    public ProcessorStatus getProcessorStatus(String type) {
        try {
            AsyncProcessor processor = processorFactory.getProcessor(type);
            return processor.getStatus();
        } catch (Exception e) {
            log.warn("获取处理器状态失败: {}", type, e);
            return null;
        }
    }

    /**
     * 获取指定异步类型的处理器状态
     *
     * @param asyncType 异步类型
     * @return 处理器状态，如果处理器不存在返回null
     */
    public ProcessorStatus getProcessorStatus(AsyncType asyncType) {
        return getProcessorStatus(asyncType.name());
    }

    /**
     * 注册自定义处理器
     * 允许在运行时动态注册新的异步处理器
     *
     * @param processor 自定义处理器实例
     *                  <p>
     *                  使用示例：
     *                  {@code
     *                  AsyncProcessor customProcessor = new CustomAsyncProcessor();
     *                  asyncExecutor.registerProcessor(customProcessor);
     *                  }
     */
    public void registerProcessor(AsyncProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("处理器不能为null");
        }

        try {
            processorFactory.registerProcessor(processor);
            log.info("自定义处理器注册成功: {}", processor.getType());
        } catch (Exception e) {
            log.error("自定义处理器注册失败: {}", processor.getType(), e);
            throw new ExcelExportException("处理器注册失败: " + e.getMessage(), e);
        }
    }

    /**
     * 注销处理器
     * 从工厂中移除处理器并销毁它
     *
     * @param type 要注销的处理器类型
     * @return 如果成功注销返回true，如果处理器不存在返回false
     */
    public boolean unregisterProcessor(String type) {
        boolean result = processorFactory.unregisterProcessor(type);
        if (result) {
            log.info("处理器注销成功: {}", type);
        } else {
            log.warn("处理器注销失败或不存在: {}", type);
        }
        return result;
    }

    /**
     * 注销指定异步类型的处理器
     *
     * @param asyncType 异步类型
     * @return 如果成功注销返回true，如果处理器不存在返回false
     */
    public boolean unregisterProcessor(AsyncType asyncType) {
        return unregisterProcessor(asyncType.name());
    }

    /**
     * 检查处理器是否可用
     *
     * @param type 处理器类型
     * @return 如果处理器存在且可用返回true，否则返回false
     */
    public boolean isProcessorAvailable(String type) {
        return processorFactory.isProcessorAvailable(type);
    }

    /**
     * 检查指定异步类型的处理器是否可用
     *
     * @param asyncType 异步类型
     * @return 如果处理器存在且可用返回true，否则返回false
     */
    public boolean isProcessorAvailable(AsyncType asyncType) {
        return isProcessorAvailable(asyncType.name());
    }

    /**
     * 获取所有可用的处理器类型
     *
     * @return 可用处理器类型列表
     */
    public List<String> getAvailableTypes() {
        return processorFactory.getAvailableTypes();
    }

    /**
     * 获取执行器状态信息
     * 包含任务统计、处理器状态和执行器运行信息
     *
     * @return 执行器状态对象
     */
    public ExecutorStatus getExecutorStatus() {
        Map<String, ProcessorStatus> processorStatus = getProcessorStatus();

        return ExecutorStatus.builder()
                .totalSubmittedTasks(totalSubmittedTasks.get())
                .successSubmittedTasks(successSubmittedTasks.get())
                .fallbackTasks(fallbackTasks.get())
                .processorCount(processorStatus.size())
                .availableProcessorCount(getAvailableProcessorCount(processorStatus))
                .startTime(new java.util.Date(executorStartTime))
                .uptime(System.currentTimeMillis() - executorStartTime)
                .build();
    }

    /**
     * 销毁方法
     * 在Spring容器关闭时调用，清理资源
     */
    @PreDestroy
    public void destroy() {
        log.info("开始关闭异步执行器...");

        // 销毁处理器工厂
        processorFactory.destroy();

        log.info("异步执行器关闭完成，总提交任务: {}, 成功提交: {}, 降级处理: {}",
                totalSubmittedTasks.get(), successSubmittedTasks.get(), fallbackTasks.get());
    }


    /**
     * 确定异步处理类型
     * 优先使用参数指定的类型，其次使用请求配置的类型，最后使用全局默认类型
     *
     * @param asyncType 参数指定的异步类型
     * @return 最终确定的异步类型
     */
    private AsyncType determineAsyncType(AsyncType asyncType) {
        // 优先使用参数指定的类型
        if (asyncType != null) {
            return asyncType;
        }

        // 其次使用全局默认配置
        return properties.getAsync().getDefaultType();
    }

    /**
     * 获取可用处理器数量
     *
     * @param processorStatus 处理器状态映射
     * @return 可用处理器数量
     */
    private long getAvailableProcessorCount(Map<String, ProcessorStatus> processorStatus) {
        return processorStatus.values().stream()
                .filter(ProcessorStatus::isAvailable)
                .count();
    }

    @Override
    public void afterPropertiesSet() {
        this.init();
    }

    /**
     * 执行器状态内部类
     * 包含异步执行器的运行时统计信息
     */
    @Data
    @Builder
    public static class ExecutorStatus {
        /**
         * 总提交任务数
         */
        private long totalSubmittedTasks;

        /**
         * 成功提交任务数
         */
        private long successSubmittedTasks;

        /**
         * 降级处理任务数
         */
        private long fallbackTasks;

        /**
         * 处理器总数
         */
        private int processorCount;

        /**
         * 可用处理器数量
         */
        private long availableProcessorCount;

        /**
         * 执行器启动时间
         */
        private java.util.Date startTime;

        /**
         * 运行时长（毫秒）
         */
        private long uptime;

        /**
         * 计算提交成功率
         *
         * @return 提交成功率（0.0 - 1.0）
         */
        public double getSubmitSuccessRate() {
            return totalSubmittedTasks > 0 ?
                    (double) successSubmittedTasks / totalSubmittedTasks : 0.0;
        }

        /**
         * 计算降级率
         *
         * @return 降级率（0.0 - 1.0）
         */
        public double getFallbackRate() {
            return totalSubmittedTasks > 0 ?
                    (double) fallbackTasks / totalSubmittedTasks : 0.0;
        }

        /**
         * 计算处理器可用率
         *
         * @return 处理器可用率（0.0 - 1.0）
         */
        public double getProcessorAvailableRate() {
            return processorCount > 0 ?
                    (double) availableProcessorCount / processorCount : 0.0;
        }

        /**
         * 获取平均提交速率（任务/分钟）
         *
         * @return 平均提交速率
         */
        public double getAverageSubmitRate() {
            long minutes = uptime / (60 * 1000);
            return minutes > 0 ? (double) totalSubmittedTasks / minutes : 0.0;
        }

        /**
         * 获取状态描述信息
         *
         * @return 状态描述
         */
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
     * 获取执行器健康状态
     * 用于健康检查端点
     *
     * @return 健康状态描述
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
                .timestamp(new java.util.Date())
                .build();
    }

    /**
     * 健康状态内部类
     * 用于健康检查和监控
     */
    @Data
    @Builder
    public static class HealthStatus {
        /**
         * 健康等级
         */
        private HealthLevel level;

        /**
         * 健康状态描述
         */
        private String message;

        /**
         * 执行器状态
         */
        private ExecutorStatus executorStatus;

        /**
         * 检查时间
         */
        private java.util.Date timestamp;

        /**
         * 健康等级枚举
         */
        public enum HealthLevel {
            /**
             * 健康 - 所有组件运行正常
             */
            HEALTHY,

            /**
             * 降级 - 部分组件不可用，但核心功能正常
             */
            DEGRADED,

            /**
             * 不健康 - 核心功能受影响
             */
            UNHEALTHY,

            /**
             * 离线 - 服务不可用
             */
            OFFLINE
        }

        /**
         * 检查是否健康
         *
         * @return 如果健康返回true
         */
        public boolean isHealthy() {
            return level == HealthLevel.HEALTHY;
        }

        /**
         * 获取健康状态描述
         *
         * @return 健康状态描述
         */
        public String getHealthDescription() {
            return String.format("健康状态: %s - %s", level, message);
        }
    }
}
