package com.silky.starter.excel.core.async.factory;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.async.AsyncProcessor;
import com.silky.starter.excel.core.async.ExportAsyncProcessor;
import com.silky.starter.excel.core.async.ImportAsyncProcessor;
import com.silky.starter.excel.core.async.model.ProcessorStatus;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 异步处理器工厂，负责管理所有异步处理器实例，提供处理器的注册、查找和生命周期管理，支持动态注册自定义处理器
 *
 * @author zy
 * @date 2025-10-27 21:58
 **/
public class AsyncProcessorFactory implements ApplicationContextAware, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AsyncProcessorFactory.class);

    /**
     * 导出处理器映射表
     */
    private final Map<String, ExportAsyncProcessor<ExportResult>> exportProcessorMap = new ConcurrentHashMap<>();

    /**
     * 导入处理器映射表
     */
    private final Map<String, ImportAsyncProcessor<ImportResult>> importProcessorMap = new ConcurrentHashMap<>();

    /**
     * 通用处理器映射表
     */
    private final Map<String, AsyncProcessor> processorMap = new ConcurrentHashMap<>();

    /**
     * AsyncType枚举到导出处理器的映射
     */
    private final Map<AsyncType, ExportAsyncProcessor<ExportResult>> exportTypeProcessorMap = new ConcurrentHashMap<>();

    /**
     * AsyncType枚举到导入处理器的映射
     */
    private final Map<AsyncType, ImportAsyncProcessor<ImportResult>> importTypeProcessorMap = new ConcurrentHashMap<>();

    /**
     * 工厂初始化状态
     */
    @Getter
    private volatile boolean initialized = false;

    /**
     * Spring应用上下文
     */
    private ApplicationContext applicationContext;

    private final SilkyExcelProperties properties;

    public AsyncProcessorFactory(SilkyExcelProperties properties) {
        this.properties = properties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        this.init();
    }

    /**
     * 初始化方法
     */
    public void init() {
        log.info("开始初始化异步处理器工厂...");
        try {
            // 自动发现并注册所有处理器
            autoDiscoverProcessors();

            // 建立类型映射
            buildTypeMapping();

            // 初始化所有处理器
            initializeAllProcessors();

            initialized = true;

            log.info("异步处理器工厂初始化完成 - 导出处理器: {}个, 导入处理器: {}个",
                    exportProcessorMap.size(), importProcessorMap.size());
            // 打印可用处理器信息
            logAvailableProcessors();

        } catch (Exception e) {
            log.error("异步处理器工厂初始化失败", e);
            throw new ExcelExportException("异步处理器工厂初始化失败", e);
        }
    }

    /**
     * 自动发现并注册所有处理器
     */
    @SuppressWarnings("all")
    private void autoDiscoverProcessors() {
        log.info("开始自动发现异步处理器...");

        // 发现导出处理器
        Map<String, ExportAsyncProcessor> exportProcessors = applicationContext.getBeansOfType(ExportAsyncProcessor.class);
        exportProcessors.forEach((beanName, processor) -> {
            String type = processor.getType();
            exportProcessorMap.put(type, processor);
            processorMap.put(type, processor);
            log.info("注册导出异步处理器: {} -> {}", type, processor.getClass().getSimpleName());
        });

        // 发现导入处理器
        Map<String, ImportAsyncProcessor> importProcessors = applicationContext.getBeansOfType(ImportAsyncProcessor.class);
        importProcessors.forEach((beanName, processor) -> {
            String type = processor.getType();
            importProcessorMap.put(type, processor);
            processorMap.put(type, processor);
            log.info("注册导入异步处理器: {} -> {}", type, processor.getClass().getSimpleName());
        });
    }

    /**
     * 建立类型映射
     */
    private void buildTypeMapping() {

        // 建立导出处理器类型映射
        for (Map.Entry<String, ExportAsyncProcessor<ExportResult>> entry : exportProcessorMap.entrySet()) {
            String type = entry.getKey();
            ExportAsyncProcessor<ExportResult> processor = entry.getValue();

            try {
                AsyncType asyncType = AsyncType.valueOf(type);
                exportTypeProcessorMap.put(asyncType, processor);
                log.debug("建立导出处理器类型映射: {} -> {}", asyncType, processor.getClass().getSimpleName());
            } catch (IllegalArgumentException e) {
                log.warn("导出处理器类型 {} 无法映射到 AsyncType 枚举", type);
            }
        }

        // 建立导入处理器类型映射
        for (Map.Entry<String, ImportAsyncProcessor<ImportResult>> entry : importProcessorMap.entrySet()) {
            String type = entry.getKey();
            ImportAsyncProcessor<ImportResult> processor = entry.getValue();

            try {
                AsyncType asyncType = AsyncType.valueOf(type);
                importTypeProcessorMap.put(asyncType, processor);
                log.debug("建立导入处理器类型映射: {} -> {}", asyncType, processor.getClass().getSimpleName());
            } catch (IllegalArgumentException e) {
                log.warn("导入处理器类型 {} 无法映射到 AsyncType 枚举", type);
            }
        }

        log.info("类型映射建立完成 -  导出处理器: {}个, 导入处理器: {}个",
                exportTypeProcessorMap.size(), importTypeProcessorMap.size());
    }

    /**
     * 初始化所有处理器
     */
    private void initializeAllProcessors() {

        // 初始化导出处理器
        for (ExportAsyncProcessor<ExportResult> processor : exportProcessorMap.values()) {
            try {
                processor.init();
                log.debug("导出处理器初始化成功: {}", processor.getType());
            } catch (Exception e) {
                log.error("导出处理器初始化失败: {}", processor.getType(), e);
            }
        }

        // 初始化导入处理器
        for (ImportAsyncProcessor<ImportResult> processor : importProcessorMap.values()) {
            try {
                processor.init();
                log.debug("导入处理器初始化成功: {}", processor.getType());
            } catch (Exception e) {
                log.error("导入处理器初始化失败: {}", processor.getType(), e);
            }
        }

        // 初始化通用处理器
        for (AsyncProcessor processor : processorMap.values()) {
            if (!exportProcessorMap.containsKey(processor.getType()) &&
                    !importProcessorMap.containsKey(processor.getType())) {
                try {
                    processor.init();
                    log.debug("通用处理器初始化成功: {}", processor.getType());
                } catch (Exception e) {
                    log.error("通用处理器初始化失败: {}", processor.getType(), e);
                }
            }
        }
    }

    /**
     * 打印可用处理器信息
     */
    private void logAvailableProcessors() {
        log.info("=== 可用异步处理器列表 ===");
        log.info("导出处理器: {}", getAvailableExportTypes());
        log.info("导入处理器: {}", getAvailableImportTypes());
        log.info("默认异步类型: {}", properties.getAsync().getAsyncType());
        log.info("==========================");
    }


    /**
     * 根据处理器类型获取导出处理器
     */
    public ExportAsyncProcessor<ExportResult> getExportProcessor(String type) {
        validateInitialized();
        validateType(type);

        ExportAsyncProcessor<ExportResult> processor = exportProcessorMap.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的导出处理器类型: " + type +
                    "，可用类型: " + getAvailableExportTypes());
        }

        return processor;
    }

    /**
     * 根据AsyncType枚举获取导出处理器
     */
    public ExportAsyncProcessor<ExportResult> getExportProcessor(AsyncType asyncType) {
        validateInitialized();
        validateAsyncType(asyncType);

        ExportAsyncProcessor<ExportResult> processor = exportTypeProcessorMap.get(asyncType);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的导出异步类型: " + asyncType +
                    "，可用类型: " + exportTypeProcessorMap.keySet());
        }
        return processor;
    }

    /**
     * 根据处理器类型获取导入处理器
     */
    public ImportAsyncProcessor<ImportResult> getImportProcessor(String type) {
        validateInitialized();
        validateType(type);

        ImportAsyncProcessor<ImportResult> processor = importProcessorMap.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的导入处理器类型: " + type +
                    "，可用类型: " + getAvailableImportTypes());
        }

        return processor;
    }

    /**
     * 根据AsyncType枚举获取导入处理器
     */
    public ImportAsyncProcessor<ImportResult> getImportProcessor(AsyncType asyncType) {
        validateInitialized();
        validateAsyncType(asyncType);

        ImportAsyncProcessor<ImportResult> processor = importTypeProcessorMap.get(asyncType);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的导入异步类型: " + asyncType +
                    "，可用类型: " + importTypeProcessorMap.keySet());
        }
        return processor;
    }

    /**
     * 注册自定义导出处理器
     */
    public void registerExportProcessor(ExportAsyncProcessor<ExportResult> processor) {
        registerCustomProcessor(processor, exportProcessorMap, exportTypeProcessorMap, "导出");
    }

    /**
     * 注册自定义导入处理器
     */
    public void registerImportProcessor(ImportAsyncProcessor<ImportResult> processor) {
        registerCustomProcessor(processor, importProcessorMap, importTypeProcessorMap, "导入");
    }

    /**
     * 通用的自定义处理器注册逻辑
     */
    private <T> void registerCustomProcessor(T processor,
                                             Map<String, T> specificMap,
                                             Map<AsyncType, T> typeMap,
                                             String processorType) {
        validateInitialized();

        if (processor == null) {
            throw new IllegalArgumentException(processorType + "处理器不能为null");
        }

        String type = ((AsyncProcessor) processor).getType();
        if (specificMap.containsKey(type)) {
            throw new IllegalArgumentException(processorType + "处理器类型已存在: " + type);
        }

        try {
            // 初始化处理器
            ((AsyncProcessor) processor).init();

            // 注册到特定处理器表
            specificMap.put(type, processor);

            // 注册到通用处理器表
            processorMap.put(type, (AsyncProcessor) processor);

            // 建立类型映射
            try {
                AsyncType asyncType = AsyncType.valueOf(type);
                typeMap.put(asyncType, processor);
                log.debug("建立{}处理器类型映射: {} -> {}", processorType, asyncType,
                        processor.getClass().getSimpleName());
            } catch (IllegalArgumentException e) {
                log.warn("{}处理器类型 {} 无法映射到 AsyncType 枚举", processorType, type);
            }

            log.info("注册自定义{}处理器成功: {} -> {}", processorType, type,
                    processor.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("自定义{}处理器初始化失败: {}", processorType, type, e);
            throw new RuntimeException(processorType + "处理器注册失败: " + e.getMessage(), e);
        }
    }


    /**
     * 获取所有处理器状态
     */
    public Map<String, ProcessorStatus> getAllProcessorStatus() {
        Map<String, ProcessorStatus> statusMap = new LinkedHashMap<>();

        // 添加导出处理器状态
        exportProcessorMap.forEach((type, processor) -> {
            statusMap.put("EXPORT_" + type, processor.getStatus());
        });

        // 添加导入处理器状态
        importProcessorMap.forEach((type, processor) -> {
            statusMap.put("IMPORT_" + type, processor.getStatus());
        });

        // 添加通用处理器状态
        processorMap.forEach((type, processor) -> {
            if (!exportProcessorMap.containsKey(type) &&
                    !importProcessorMap.containsKey(type)) {
                statusMap.put(type, processor.getStatus());
            }
        });

        return statusMap;
    }

    /**
     * 获取可用的导出处理器类型
     */
    public List<String> getAvailableExportTypes() {
        return new ArrayList<>(exportProcessorMap.keySet());
    }

    /**
     * 获取可用的导入处理器类型
     */
    public List<String> getAvailableImportTypes() {
        return new ArrayList<>(importProcessorMap.keySet());
    }

    /**
     * 获取所有可用处理器类型（兼容老版本）
     */
    public List<String> getAvailableTypes() {
        return new ArrayList<>(processorMap.keySet());
    }


    /**
     * 注销处理器
     */
    public boolean unregisterProcessor(String type) {
        validateInitialized();

        boolean unregistered = false;

        // 尝试从各个映射表中移除
        ExportAsyncProcessor<ExportResult> exportProcessor = exportProcessorMap.remove(type);
        ImportAsyncProcessor<ImportResult> importProcessor = importProcessorMap.remove(type);
        AsyncProcessor generalProcessor = processorMap.remove(type);

        if (exportProcessor != null) {
            destroyProcessor(exportProcessor, type);
            exportTypeProcessorMap.values().removeIf(p -> p.getType().equals(type));
            unregistered = true;
        }

        if (importProcessor != null) {
            destroyProcessor(importProcessor, type);
            importTypeProcessorMap.values().removeIf(p -> p.getType().equals(type));
            unregistered = true;
        }

        if (generalProcessor != null && !unregistered) {
            destroyProcessor(generalProcessor, type);
            unregistered = true;
        }
        if (unregistered) {
            log.info("注销异步处理器成功: {}", type);
        } else {
            log.warn("尝试注销不存在的处理器: {}", type);
        }
        return unregistered;
    }


    /**
     * 销毁方法
     */
    @PreDestroy
    public void destroy() {
        log.info("开始销毁异步处理器工厂...");

        // 销毁所有处理器
        destroyAllProcessors();

        // 清空映射表
        exportProcessorMap.clear();
        importProcessorMap.clear();
        processorMap.clear();
        exportTypeProcessorMap.clear();
        importTypeProcessorMap.clear();

        initialized = false;

        log.info("异步处理器工厂已销毁");
    }

    /**
     * 销毁所有处理器
     */
    private void destroyAllProcessors() {

        // 销毁导出处理器
        for (ExportAsyncProcessor<ExportResult> processor : exportProcessorMap.values()) {
            destroyProcessor(processor, processor.getType());
        }

        // 销毁导入处理器
        for (ImportAsyncProcessor<ImportResult> processor : importProcessorMap.values()) {
            destroyProcessor(processor, processor.getType());
        }

        // 销毁通用处理器
        for (AsyncProcessor processor : processorMap.values()) {
            if (!exportProcessorMap.containsKey(processor.getType()) &&
                    !importProcessorMap.containsKey(processor.getType())) {
                destroyProcessor(processor, processor.getType());
            }
        }
    }

    /**
     * 销毁单个处理器
     */
    private void destroyProcessor(AsyncProcessor processor, String type) {
        try {
            processor.destroy();
            log.debug("处理器销毁成功: {}", type);
        } catch (Exception e) {
            log.error("处理器销毁失败: {}", type, e);
        }
    }


    /**
     * 验证工厂是否已初始化
     */
    private void validateInitialized() {
        if (!initialized) {
            throw new IllegalStateException("异步处理器工厂未初始化");
        }
    }

    /**
     * 验证类型参数
     */
    private void validateType(String type) {
        if (StrUtil.isBlank(type)) {
            throw new IllegalArgumentException("处理器类型不能为空");
        }
    }

    /**
     * 验证异步类型参数
     */
    private void validateAsyncType(AsyncType asyncType) {
        if (asyncType == null) {
            throw new IllegalArgumentException("异步类型不能为空");
        }
    }

    /**
     * 获取工厂状态信息
     */
    public String getFactoryStatus() {
        return String.format("异步处理器工厂[%s] -  导出处理器: %d个, 导入处理器: %d个, 总处理器: %d个",
                initialized ? "已初始化" : "未初始化",
                exportProcessorMap.size(),
                importProcessorMap.size(),
                processorMap.size());
    }

    /**
     * 获取工厂统计信息
     */
    public Map<String, Object> getFactoryStatistics() {
        Map<String, Object> stats = new HashMap<>(12);
        stats.put("initialized", initialized);
        stats.put("exportProcessorCount", exportProcessorMap.size());
        stats.put("importProcessorCount", importProcessorMap.size());
        stats.put("totalProcessorCount", processorMap.size());
        stats.put("availableExportTypes", getAvailableExportTypes());
        stats.put("availableImportTypes", getAvailableImportTypes());
        stats.put("defaultAsyncType", properties.getAsync().getAsyncType());
        return stats;
    }

}
