package com.silky.starter.excel.core.storage.factory;

import com.silky.starter.excel.core.storage.StorageStrategy;
import com.silky.starter.excel.enums.StorageType;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储策略工厂
 *
 * @author zy
 * @date 2025-10-24 13:50
 **/
public class StorageStrategyFactory implements InitializingBean {

    private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StorageStrategyFactory.class);

    private final List<StorageStrategy> storageStrategies;

    public StorageStrategyFactory(List<StorageStrategy> storageStrategies) {
        this.storageStrategies = storageStrategies;
    }

    /**
     * 存储策略映射表
     */
    private final Map<StorageType, StorageStrategy> strategyMap = new ConcurrentHashMap<>();

    /**
     * 初始化方法，注册所有存储策略
     */
    @Override
    public void afterPropertiesSet() {
        this.init();
    }

    /**
     * 初始化方法，注册所有存储策略
     */
    public void init() {
        if (storageStrategies != null) {
            for (StorageStrategy strategy : storageStrategies) {
                strategyMap.put(strategy.getStorageType(), strategy);
                log.info("注册存储策略: {}", strategy.getStorageType());
            }
        }
    }

    /**
     * 根据存储类型获取存储策略
     *
     * @param storageType 存储类型
     * @return 存储策略实例
     * @throws IllegalArgumentException 如果存储类型不支持
     */
    public StorageStrategy getStrategy(StorageType storageType) {
        StorageStrategy strategy = strategyMap.get(storageType);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的存储类型: " + storageType);
        }
        return strategy;
    }

    /**
     * 获取默认存储策略（本地存储）
     *
     * @return 默认存储策略实例
     */
    public StorageStrategy getDefaultStrategy() {
        return getStrategy(StorageType.LOCAL);
    }

    /**
     * 注册自定义存储策略
     *
     * @param strategy 存储策略实例
     */
    public void registerStrategy(StorageStrategy strategy) {
        StorageType storageType = strategy.getStorageType();
        if (strategyMap.containsKey(storageType)) {
            log.warn("存储策略已存在: {}, 将被覆盖", storageType);
        }
        strategyMap.put(storageType, strategy);
        log.info("注册自定义存储策略: {}", storageType);
    }

    /**
     * 获取所有支持的存储类型
     *
     * @return 存储类型列表
     */
    public StorageType[] getSupportedStorageTypes() {
        return strategyMap.keySet().toArray(new StorageType[0]);
    }

}
