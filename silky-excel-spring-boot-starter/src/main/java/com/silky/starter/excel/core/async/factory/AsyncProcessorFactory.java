package com.silky.starter.excel.core.async.factory;

import com.silky.starter.excel.core.async.AsyncProcessor;
import com.silky.starter.excel.core.async.ProcessorStatus;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步处理器工厂，负责管理所有异步处理器实例，提供处理器的注册、查找和生命周期管理，支持动态注册自定义处理器
 *
 * @author zy
 * @date 2025-10-24 16:30
 **/
public class AsyncProcessorFactory {

    private static final Logger log = LoggerFactory.getLogger(AsyncProcessorFactory.class);

    /**
     * 处理器类型映射表
     * key: 处理器类型字符串，value: 处理器实例
     */
    private final Map<String, AsyncProcessor> processorMap = new ConcurrentHashMap<>();

    /**
     * AsyncType枚举到处理器的映射
     * key: AsyncType枚举，value: 处理器实例
     */
    private final Map<AsyncType, AsyncProcessor> typeProcessorMap = new ConcurrentHashMap<>();

    /**
     * 自定义处理器注册表
     * 用于存储动态注册的自定义处理器
     */
    private final Map<String, AsyncProcessor> customProcessorMap = new ConcurrentHashMap<>();

    /**
     * 工厂初始化状态 检查工厂是否已初始化
     */
    @Getter
    private volatile boolean initialized = false;

    private final SilkyExcelProperties properties;


    /**
     * 所有异步处理器列表 由Spring自动注入
     */
    private final List<AsyncProcessor> processors;

    public AsyncProcessorFactory(SilkyExcelProperties properties, List<AsyncProcessor> processors) {
        this.properties = properties;
        this.processors = processors;
    }

    /**
     * 初始化方法
     * 注册所有处理器并建立类型映射
     *
     * @throws Exception 如果初始化失败
     */
    @PostConstruct
    public void init() throws Exception {
        log.info("开始初始化异步处理器工厂...");

        // 注册Spring管理的处理器
        registerSpringProcessors();

        // 注册自定义处理器（如果有配置）
        registerCustomProcessors();

        // 建立类型映射
        buildTypeMapping();

        // 初始化所有处理器
        initializeAllProcessors();

        initialized = true;

        log.info("异步处理器工厂初始化完成，共注册 {} 个处理器: {}",
                processorMap.size(), processorMap.keySet());
    }

    /**
     * 销毁方法
     * 销毁所有处理器实例
     */
    @PreDestroy
    public void destroy() {
        log.info("开始销毁异步处理器工厂...");

        // 销毁所有处理器
        destroyAllProcessors();

        // 清空映射表
        processorMap.clear();
        typeProcessorMap.clear();
        customProcessorMap.clear();

        initialized = false;

        log.info("异步处理器工厂已销毁");
    }

    /**
     * 注册Spring管理的处理器
     */
    private void registerSpringProcessors() {
        if (processors == null || processors.isEmpty()) {
            log.warn("未找到任何Spring管理的异步处理器");
            return;
        }

        for (AsyncProcessor processor : processors) {
            String type = processor.getType();

            if (processorMap.containsKey(type)) {
                log.warn("处理器类型冲突: {}，已存在的处理器: {}，新的处理器: {}",
                        type, processorMap.get(type).getClass().getSimpleName(),
                        processor.getClass().getSimpleName());
            }

            processorMap.put(type, processor);
            log.info("注册Spring异步处理器: {} -> {}", type, processor.getClass().getSimpleName());
        }
    }

    /**
     * 注册自定义处理器
     */
    private void registerCustomProcessors() {
        // 检查是否启用自定义处理器扫描
        if (!properties.getAsync().getCustom().isEnabled()) {
            log.debug("自定义处理器扫描未启用");
            return;
        }

        String scanPackages = properties.getAsync().getCustom().getScanPackages();
        if (scanPackages == null || scanPackages.trim().isEmpty()) {
            log.debug("未配置自定义处理器扫描包路径");
            return;
        }

        log.info("开始扫描自定义异步处理器，包路径: {}", scanPackages);

        // 这里可以实现包扫描逻辑来发现自定义处理器
        // 实际实现中可以使用Spring的扫描机制或自定义扫描
        // 由于复杂度较高，这里只记录日志
        log.info("自定义处理器扫描功能待实现，包路径: {}", scanPackages);
    }

    /**
     * 建立类型映射
     * 将AsyncType枚举映射到对应的处理器
     */
    private void buildTypeMapping() {
        for (Map.Entry<String, AsyncProcessor> entry : processorMap.entrySet()) {
            String type = entry.getKey();
            AsyncProcessor processor = entry.getValue();

            try {
                AsyncType asyncType = AsyncType.valueOf(type);
                typeProcessorMap.put(asyncType, processor);
                log.debug("建立类型映射: {} -> {}", asyncType, processor.getClass().getSimpleName());
            } catch (IllegalArgumentException e) {
                log.warn("处理器类型 {} 无法映射到 AsyncType 枚举，将无法通过AsyncType获取", type);
            }
        }

        log.info("类型映射建立完成，共映射 {} 个处理器类型", typeProcessorMap.size());
    }

    /**
     * 初始化所有处理器
     */
    private void initializeAllProcessors() {
        for (AsyncProcessor processor : processorMap.values()) {
            try {
                processor.init();
                log.debug("处理器初始化成功: {}", processor.getType());
            } catch (Exception e) {
                log.error("处理器初始化失败: {}", processor.getType(), e);
            }
        }
    }

    /**
     * 销毁所有处理器
     */
    private void destroyAllProcessors() {
        for (AsyncProcessor processor : processorMap.values()) {
            try {
                processor.destroy();
                log.debug("处理器销毁成功: {}", processor.getType());
            } catch (Exception e) {
                log.error("处理器销毁失败: {}", processor.getType(), e);
            }
        }

        // 同时销毁自定义处理器
        for (AsyncProcessor processor : customProcessorMap.values()) {
            try {
                processor.destroy();
                log.debug("自定义处理器销毁成功: {}", processor.getType());
            } catch (Exception e) {
                log.error("自定义处理器销毁失败: {}", processor.getType(), e);
            }
        }
    }

    /**
     * 根据处理器类型获取处理器实例
     *
     * @param type 处理器类型字符串
     * @return 对应的处理器实例
     * @throws IllegalArgumentException 如果找不到对应的处理器
     */
    public AsyncProcessor getProcessor(String type) {
        if (!initialized) {
            throw new IllegalStateException("异步处理器工厂未初始化");
        }

        AsyncProcessor processor = processorMap.get(type);
        if (processor == null) {
            processor = customProcessorMap.get(type);
        }

        if (processor == null) {
            throw new IllegalArgumentException("不支持的处理器类型: " + type + "，可用类型: " + getAvailableTypes());
        }

        return processor;
    }

    /**
     * 根据AsyncType枚举获取处理器实例
     *
     * @param asyncType 异步类型枚举
     * @return 对应的处理器实例
     * @throws IllegalArgumentException 如果找不到对应的处理器
     */
    public AsyncProcessor getProcessor(AsyncType asyncType) {
        if (!initialized) {
            throw new IllegalStateException("异步处理器工厂未初始化");
        }

        AsyncProcessor processor = typeProcessorMap.get(asyncType);
        if (processor == null) {
            throw new IllegalArgumentException("不支持的异步类型: " + asyncType + "，可用类型: " + typeProcessorMap.keySet());
        }

        return processor;
    }

    /**
     * 获取默认处理器
     * 使用配置文件中指定的默认异步类型
     *
     * @return 默认处理器实例
     */
    public AsyncProcessor getDefaultProcessor() {
        AsyncType defaultType = properties.getAsync().getDefaultType();
        return getProcessor(defaultType);
    }

    /**
     * 注册自定义处理器
     * 允许在运行时动态注册新的处理器
     *
     * @param processor 自定义处理器实例
     * @throws IllegalArgumentException 如果处理器类型已存在
     */
    public void registerProcessor(AsyncProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("处理器不能为null");
        }

        String type = processor.getType();
        if (processorMap.containsKey(type) || customProcessorMap.containsKey(type)) {
            throw new IllegalArgumentException("处理器类型已存在: " + type);
        }

        try {
            // 初始化处理器
            processor.init();

            // 注册到自定义处理器表
            customProcessorMap.put(type, processor);

            // 尝试建立类型映射
            try {
                AsyncType asyncType = AsyncType.valueOf(type);
                typeProcessorMap.put(asyncType, processor);
                log.debug("建立自定义处理器类型映射: {} -> {}", asyncType, processor.getClass().getSimpleName());
            } catch (IllegalArgumentException e) {
                log.warn("自定义处理器类型 {} 无法映射到 AsyncType 枚举", type);
            }

            log.info("注册自定义异步处理器成功: {} -> {}", type, processor.getClass().getSimpleName());

        } catch (Exception e) {
            log.error("自定义处理器初始化失败: {}", type, e);
            throw new RuntimeException("处理器注册失败: " + e.getMessage(), e);
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
        AsyncProcessor processor = customProcessorMap.remove(type);
        if (processor == null) {
            processor = processorMap.get(type);
            if (processor != null) {
                log.warn("尝试注销Spring管理的处理器: {}，这将影响其他依赖此处理器的组件", type);
                processorMap.remove(type);
            }
        }

        if (processor != null) {
            try {
                processor.destroy();

                // 移除类型映射
                typeProcessorMap.values().removeIf(p -> p.getType().equals(type));

                log.info("注销异步处理器成功: {}", type);
                return true;

            } catch (Exception e) {
                log.error("处理器销毁失败: {}", type, e);
                return false;
            }
        }

        log.warn("尝试注销不存在的处理器: {}", type);
        return false;
    }

    /**
     * 获取所有处理器状态
     *
     * @return 处理器类型到状态的映射表
     */
    public Map<String, ProcessorStatus> getAllProcessorStatus() {
        Map<String, ProcessorStatus> statusMap = new LinkedHashMap<>();

        // 添加Spring管理的处理器状态
        processorMap.forEach((type, processor) -> {
            statusMap.put(type, processor.getStatus());
        });

        // 添加自定义处理器状态
        customProcessorMap.forEach((type, processor) -> {
            statusMap.put(type, processor.getStatus());
        });

        return statusMap;
    }

    /**
     * 获取指定处理器的状态
     *
     * @param type 处理器类型
     * @return 处理器状态，如果处理器不存在返回null
     */
    public ProcessorStatus getProcessorStatus(String type) {
        AsyncProcessor processor = processorMap.get(type);
        if (processor == null) {
            processor = customProcessorMap.get(type);
        }

        return processor != null ? processor.getStatus() : null;
    }

    /**
     * 检查处理器是否可用
     *
     * @param type 处理器类型
     * @return 如果处理器存在且可用返回true，否则返回false
     */
    public boolean isProcessorAvailable(String type) {
        try {
            AsyncProcessor processor = getProcessor(type);
            return processor.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取所有可用的处理器类型
     *
     * @return 可用处理器类型列表
     */
    public List<String> getAvailableTypes() {
        List<String> types = new ArrayList<>(processorMap.keySet());
        types.addAll(customProcessorMap.keySet());
        return types;
    }

    /**
     * 获取工厂状态
     *
     * @return 工厂状态描述
     */
    public String getFactoryStatus() {
        return String.format("异步处理器工厂[%s]，已注册处理器: %d个，自定义处理器: %d个",
                initialized ? "已初始化" : "未初始化",
                processorMap.size(),
                customProcessorMap.size());
    }

}
