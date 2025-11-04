package com.silky.starter.excel.core.async.factory;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.async.*;
import com.silky.starter.excel.core.async.model.ProcessorStatus;
import com.silky.starter.excel.core.exception.ExcelExportException;
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
    private final Map<String, ExportAsyncProcessor> exportProcessorMap = new ConcurrentHashMap<>();

    /**
     * 导入处理器映射表
     */
    private final Map<String, ImportAsyncProcessor> importProcessorMap = new ConcurrentHashMap<>();

    /**
     * 统一处理器映射表
     */
    private final Map<String, UnifiedAsyncProcessor> unifiedProcessorMap = new ConcurrentHashMap<>();

    /**
     * 通用处理器映射表（兼容老版本）
     */
    private final Map<String, AsyncProcessor> processorMap = new ConcurrentHashMap<>();

    /**
     * AsyncType枚举到导出处理器的映射
     */
    private final Map<AsyncType, ExportAsyncProcessor> exportTypeProcessorMap = new ConcurrentHashMap<>();

    /**
     * AsyncType枚举到导入处理器的映射
     */
    private final Map<AsyncType, ImportAsyncProcessor> importTypeProcessorMap = new ConcurrentHashMap<>();

    /**
     * AsyncType枚举到统一处理器的映射
     */
    private final Map<AsyncType, UnifiedAsyncProcessor> unifiedTypeProcessorMap = new ConcurrentHashMap<>();

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

            log.info("异步处理器工厂初始化完成 - 导出处理器: {}个, 导入处理器: {}个, 统一处理器: {}个",
                    exportProcessorMap.size(), importProcessorMap.size(), unifiedProcessorMap.size());
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
    private void autoDiscoverProcessors() {
        log.info("开始自动发现异步处理器...");

        // 发现统一处理器（优先）
//        Map<String, UnifiedAsyncProcessor> unifiedProcessors = applicationContext.getBeansOfType(UnifiedAsyncProcessor.class);
//        unifiedProcessors.forEach((beanName, processor) -> {
//            String type = processor.getType();
//            unifiedProcessorMap.put(type, processor);
//            processorMap.put(type, processor);
//            log.info("注册统一异步处理器: {} -> {}", type, processor.getClass().getSimpleName());
//        });
        // 发现导出处理器
        Map<String, ExportAsyncProcessor> exportProcessors = applicationContext.getBeansOfType(ExportAsyncProcessor.class);
        exportProcessors.forEach((beanName, processor) -> {
            String type = processor.getType();
            // 如果统一处理器已经注册了相同类型，则跳过
            if (!unifiedProcessorMap.containsKey(type)) {
                exportProcessorMap.put(type, processor);
                processorMap.put(type, processor);
                log.info("注册导出异步处理器: {} -> {}", type, processor.getClass().getSimpleName());
            } else {
                log.warn("跳过导出处理器 {}，因为统一处理器已注册相同类型", type);
            }
        });

        // 发现导入处理器
        Map<String, ImportAsyncProcessor> importProcessors =
                applicationContext.getBeansOfType(ImportAsyncProcessor.class);
        importProcessors.forEach((beanName, processor) -> {
            String type = processor.getType();
            // 如果统一处理器已经注册了相同类型，则跳过
            if (!unifiedProcessorMap.containsKey(type)) {
                importProcessorMap.put(type, processor);
                processorMap.put(type, processor);
                log.info("注册导入异步处理器: {} -> {}", type, processor.getClass().getSimpleName());
            } else {
                log.warn("跳过导入处理器 {}，因为统一处理器已注册相同类型", type);
            }
        });

        // 发现通用处理器（兼容老版本）
        Map<String, AsyncProcessor> generalProcessors =
                applicationContext.getBeansOfType(AsyncProcessor.class);
        generalProcessors.forEach((beanName, processor) -> {
            // 跳过已经注册的处理器
            if (!(processor instanceof ExportAsyncProcessor) &&
                    !(processor instanceof ImportAsyncProcessor) &&
                    !(processor instanceof UnifiedAsyncProcessor)) {
                String type = processor.getType();
                processorMap.put(type, processor);
                log.info("注册通用异步处理器: {} -> {}", type, processor.getClass().getSimpleName());
            }
        });
    }

    /**
     * 建立类型映射
     */
    private void buildTypeMapping() {
        // 建立统一处理器类型映射
        for (Map.Entry<String, UnifiedAsyncProcessor> entry : unifiedProcessorMap.entrySet()) {
            String type = entry.getKey();
            UnifiedAsyncProcessor processor = entry.getValue();

            try {
                AsyncType asyncType = AsyncType.valueOf(type);
                unifiedTypeProcessorMap.put(asyncType, processor);
                log.debug("建立统一处理器类型映射: {} -> {}", asyncType, processor.getClass().getSimpleName());
            } catch (IllegalArgumentException e) {
                log.warn("统一处理器类型 {} 无法映射到 AsyncType 枚举", type);
            }
        }

        // 建立导出处理器类型映射
        for (Map.Entry<String, ExportAsyncProcessor> entry : exportProcessorMap.entrySet()) {
            String type = entry.getKey();
            ExportAsyncProcessor processor = entry.getValue();

            try {
                AsyncType asyncType = AsyncType.valueOf(type);
                exportTypeProcessorMap.put(asyncType, processor);
                log.debug("建立导出处理器类型映射: {} -> {}", asyncType, processor.getClass().getSimpleName());
            } catch (IllegalArgumentException e) {
                log.warn("导出处理器类型 {} 无法映射到 AsyncType 枚举", type);
            }
        }

        // 建立导入处理器类型映射
        for (Map.Entry<String, ImportAsyncProcessor> entry : importProcessorMap.entrySet()) {
            String type = entry.getKey();
            ImportAsyncProcessor processor = entry.getValue();

            try {
                AsyncType asyncType = AsyncType.valueOf(type);
                importTypeProcessorMap.put(asyncType, processor);
                log.debug("建立导入处理器类型映射: {} -> {}", asyncType, processor.getClass().getSimpleName());
            } catch (IllegalArgumentException e) {
                log.warn("导入处理器类型 {} 无法映射到 AsyncType 枚举", type);
            }
        }

        log.info("类型映射建立完成 - 统一处理器: {}个, 导出处理器: {}个, 导入处理器: {}个",
                unifiedTypeProcessorMap.size(), exportTypeProcessorMap.size(), importTypeProcessorMap.size());
    }

    /**
     * 初始化所有处理器
     */
    private void initializeAllProcessors() {
        // 初始化统一处理器
        for (UnifiedAsyncProcessor processor : unifiedProcessorMap.values()) {
            try {
                processor.init();
                log.debug("统一处理器初始化成功: {}", processor.getType());
            } catch (Exception e) {
                log.error("统一处理器初始化失败: {}", processor.getType(), e);
            }
        }

        // 初始化导出处理器
        for (ExportAsyncProcessor processor : exportProcessorMap.values()) {
            try {
                processor.init();
                log.debug("导出处理器初始化成功: {}", processor.getType());
            } catch (Exception e) {
                log.error("导出处理器初始化失败: {}", processor.getType(), e);
            }
        }

        // 初始化导入处理器
        for (ImportAsyncProcessor processor : importProcessorMap.values()) {
            try {
                processor.init();
                log.debug("导入处理器初始化成功: {}", processor.getType());
            } catch (Exception e) {
                log.error("导入处理器初始化失败: {}", processor.getType(), e);
            }
        }

        // 初始化通用处理器
        for (AsyncProcessor processor : processorMap.values()) {
            if (!unifiedProcessorMap.containsKey(processor.getType()) &&
                    !exportProcessorMap.containsKey(processor.getType()) &&
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
        log.info("统一处理器: {}", getAvailableUnifiedTypes());
        log.info("导出处理器: {}", getAvailableExportTypes());
        log.info("导入处理器: {}", getAvailableImportTypes());
        log.info("默认异步类型: {}", properties.getAsync().getAsyncType());
        log.info("==========================");
    }

    // ==================== 统一处理器相关方法 ====================

    /**
     * 根据处理器类型获取统一处理器
     */
    public UnifiedAsyncProcessor getUnifiedProcessor(String type) {
        validateInitialized();
        validateType(type);

        UnifiedAsyncProcessor processor = unifiedProcessorMap.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的统一处理器类型: " + type +
                    "，可用类型: " + getAvailableUnifiedTypes());
        }

        return processor;
    }

    /**
     * 根据AsyncType枚举获取统一处理器
     */
    public UnifiedAsyncProcessor getUnifiedProcessor(AsyncType asyncType) {
        validateInitialized();
        validateAsyncType(asyncType);

        UnifiedAsyncProcessor processor = unifiedTypeProcessorMap.get(asyncType);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的统一异步类型: " + asyncType +
                    "，可用类型: " + unifiedTypeProcessorMap.keySet());
        }

        return processor;
    }

    /**
     * 获取默认统一处理器
     */
    public UnifiedAsyncProcessor getDefaultUnifiedProcessor() {
        AsyncType defaultType = properties.getAsync().getAsyncType();
        return getUnifiedProcessor(defaultType);
    }

    // ==================== 导出处理器相关方法 ====================

    /**
     * 根据处理器类型获取导出处理器
     */
    public ExportAsyncProcessor getExportProcessor(String type) {
        validateInitialized();
        validateType(type);

        ExportAsyncProcessor processor = exportProcessorMap.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的导出处理器类型: " + type +
                    "，可用类型: " + getAvailableExportTypes());
        }

        return processor;
    }

    /**
     * 根据AsyncType枚举获取导出处理器
     */
    public ExportAsyncProcessor getExportProcessor(AsyncType asyncType) {
        validateInitialized();
        validateAsyncType(asyncType);

        ExportAsyncProcessor processor = exportTypeProcessorMap.get(asyncType);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的导出异步类型: " + asyncType +
                    "，可用类型: " + exportTypeProcessorMap.keySet());
        }

        return processor;
    }

    /**
     * 获取默认导出处理器
     */
    public ExportAsyncProcessor getDefaultExportProcessor() {
        AsyncType defaultType = properties.getAsync().getAsyncType();
        return getExportProcessor(defaultType);
    }

    // ==================== 导入处理器相关方法 ====================

    /**
     * 根据处理器类型获取导入处理器
     */
    public ImportAsyncProcessor getImportProcessor(String type) {
        validateInitialized();
        validateType(type);

        ImportAsyncProcessor processor = importProcessorMap.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的导入处理器类型: " + type +
                    "，可用类型: " + getAvailableImportTypes());
        }

        return processor;
    }

    /**
     * 根据AsyncType枚举获取导入处理器
     */
    public ImportAsyncProcessor getImportProcessor(AsyncType asyncType) {
        validateInitialized();
        validateAsyncType(asyncType);

        ImportAsyncProcessor processor = importTypeProcessorMap.get(asyncType);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的导入异步类型: " + asyncType +
                    "，可用类型: " + importTypeProcessorMap.keySet());
        }

        return processor;
    }

    /**
     * 获取默认导入处理器
     */
    public ImportAsyncProcessor getDefaultImportProcessor() {
        AsyncType defaultType = properties.getAsync().getAsyncType();
        return getImportProcessor(defaultType);
    }

    // ==================== 通用处理器相关方法（兼容老版本） ====================

    /**
     * 通用获取处理器方法（兼容老版本）
     */
    @Deprecated
    public AsyncProcessor getProcessor(String type) {
        validateInitialized();
        validateType(type);

        AsyncProcessor processor = processorMap.get(type);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的处理器类型: " + type +
                    "，可用类型: " + getAvailableTypes());
        }

        return processor;
    }

    /**
     * 通用获取处理器方法（兼容老版本）
     */
    @Deprecated
    public AsyncProcessor getProcessor(AsyncType asyncType) {
        return getProcessor(asyncType.name());
    }

    // ==================== 处理器注册方法 ====================

    /**
     * 注册自定义统一处理器
     */
    public void registerUnifiedProcessor(UnifiedAsyncProcessor processor) {
        registerCustomProcessor(processor, unifiedProcessorMap, unifiedTypeProcessorMap, "统一");
    }

    /**
     * 注册自定义导出处理器
     */
    public void registerExportProcessor(ExportAsyncProcessor processor) {
        registerCustomProcessor(processor, exportProcessorMap, exportTypeProcessorMap, "导出");
    }

    /**
     * 注册自定义导入处理器
     */
    public void registerImportProcessor(ImportAsyncProcessor processor) {
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

    // ==================== 状态和监控方法 ====================

    /**
     * 获取所有处理器状态
     */
    public Map<String, ProcessorStatus> getAllProcessorStatus() {
        Map<String, ProcessorStatus> statusMap = new LinkedHashMap<>();

        // 添加统一处理器状态
        unifiedProcessorMap.forEach((type, processor) -> {
            statusMap.put("UNIFIED_" + type, processor.getStatus());
        });

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
            if (!unifiedProcessorMap.containsKey(type) &&
                    !exportProcessorMap.containsKey(type) &&
                    !importProcessorMap.containsKey(type)) {
                statusMap.put(type, processor.getStatus());
            }
        });

        return statusMap;
    }

    /**
     * 获取指定处理器的状态
     */
    public ProcessorStatus getProcessorStatus(String type) {
        AsyncProcessor processor = processorMap.get(type);
        return processor != null ? processor.getStatus() : null;
    }

    // ==================== 可用性检查方法 ====================

    /**
     * 检查统一处理器是否可用
     */
    public boolean isUnifiedProcessorAvailable(String type) {
        try {
            UnifiedAsyncProcessor processor = getUnifiedProcessor(type);
            return processor.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查导出处理器是否可用
     */
    public boolean isExportProcessorAvailable(String type) {
        try {
            ExportAsyncProcessor processor = getExportProcessor(type);
            return processor.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查导入处理器是否可用
     */
    public boolean isImportProcessorAvailable(String type) {
        try {
            ImportAsyncProcessor processor = getImportProcessor(type);
            return processor.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查处理器是否可用（兼容老版本）
     */
    public boolean isProcessorAvailable(String type) {
        try {
            AsyncProcessor processor = getProcessor(type);
            return processor.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 可用类型获取方法 ====================

    /**
     * 获取可用的统一处理器类型
     */
    public List<String> getAvailableUnifiedTypes() {
        return new ArrayList<>(unifiedProcessorMap.keySet());
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
     * 获取所有可用的异步类型枚举
     */
    public List<AsyncType> getAvailableAsyncTypes() {
        Set<AsyncType> asyncTypes = new HashSet<>();
        asyncTypes.addAll(unifiedTypeProcessorMap.keySet());
        asyncTypes.addAll(exportTypeProcessorMap.keySet());
        asyncTypes.addAll(importTypeProcessorMap.keySet());
        return new ArrayList<>(asyncTypes);
    }

    // ==================== 注销方法 ====================

    /**
     * 注销处理器
     */
    public boolean unregisterProcessor(String type) {
        validateInitialized();

        boolean unregistered = false;

        // 尝试从各个映射表中移除
        UnifiedAsyncProcessor unifiedProcessor = unifiedProcessorMap.remove(type);
        ExportAsyncProcessor exportProcessor = exportProcessorMap.remove(type);
        ImportAsyncProcessor importProcessor = importProcessorMap.remove(type);
        AsyncProcessor generalProcessor = processorMap.remove(type);

        if (unifiedProcessor != null) {
            destroyProcessor(unifiedProcessor, type);
            unifiedTypeProcessorMap.values().removeIf(p -> p.getType().equals(type));
            unregistered = true;
        }

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
     * 注销指定异步类型的处理器
     */
    public boolean unregisterProcessor(AsyncType asyncType) {
        return unregisterProcessor(asyncType.name());
    }

    // ==================== 销毁方法 ====================

    /**
     * 销毁方法
     */
    @PreDestroy
    public void destroy() {
        log.info("开始销毁异步处理器工厂...");

        // 销毁所有处理器
        destroyAllProcessors();

        // 清空映射表
        unifiedProcessorMap.clear();
        exportProcessorMap.clear();
        importProcessorMap.clear();
        processorMap.clear();
        unifiedTypeProcessorMap.clear();
        exportTypeProcessorMap.clear();
        importTypeProcessorMap.clear();

        initialized = false;

        log.info("异步处理器工厂已销毁");
    }

    /**
     * 销毁所有处理器
     */
    private void destroyAllProcessors() {
        // 销毁统一处理器
        for (UnifiedAsyncProcessor processor : unifiedProcessorMap.values()) {
            destroyProcessor(processor, processor.getType());
        }

        // 销毁导出处理器
        for (ExportAsyncProcessor processor : exportProcessorMap.values()) {
            destroyProcessor(processor, processor.getType());
        }

        // 销毁导入处理器
        for (ImportAsyncProcessor processor : importProcessorMap.values()) {
            destroyProcessor(processor, processor.getType());
        }

        // 销毁通用处理器
        for (AsyncProcessor processor : processorMap.values()) {
            if (!unifiedProcessorMap.containsKey(processor.getType()) &&
                    !exportProcessorMap.containsKey(processor.getType()) &&
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

    // ==================== 验证方法 ====================

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
        return String.format("异步处理器工厂[%s] - 统一处理器: %d个, 导出处理器: %d个, 导入处理器: %d个, 总处理器: %d个",
                initialized ? "已初始化" : "未初始化",
                unifiedProcessorMap.size(),
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
        stats.put("unifiedProcessorCount", unifiedProcessorMap.size());
        stats.put("exportProcessorCount", exportProcessorMap.size());
        stats.put("importProcessorCount", importProcessorMap.size());
        stats.put("totalProcessorCount", processorMap.size());
        stats.put("availableUnifiedTypes", getAvailableUnifiedTypes());
        stats.put("availableExportTypes", getAvailableExportTypes());
        stats.put("availableImportTypes", getAvailableImportTypes());
        stats.put("defaultAsyncType", properties.getAsync().getAsyncType());
        return stats;
    }

}
