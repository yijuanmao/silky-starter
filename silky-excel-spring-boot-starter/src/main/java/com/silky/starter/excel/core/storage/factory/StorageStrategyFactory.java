package com.silky.starter.excel.core.storage.factory;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.storage.StorageStrategy;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储策略工厂。
 *
 * @author zy
 * @date 2025-10-24 13:50
 **/
@Slf4j
public class StorageStrategyFactory implements InitializingBean {

    private final List<StorageStrategy> storageStrategies;

    private final SilkyExcelProperties properties;

    private final Map<String, StorageStrategy> strategyMap = new ConcurrentHashMap<>();

    public StorageStrategyFactory(List<StorageStrategy> storageStrategies, SilkyExcelProperties properties) {
        this.storageStrategies = storageStrategies;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (CollectionUtil.isEmpty(storageStrategies)) {
            return;
        }
        for (StorageStrategy strategy : storageStrategies) {
            registerStrategy(strategy);
        }
    }

    public StorageStrategy getStrategy(String storageType) {
        String type = normalizeType(StrUtil.blankToDefault(storageType, properties.getStorage().getType()));
        StorageStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的存储类型: " + storageType);
        }
        return strategy;
    }

    public StorageStrategy getDefaultStrategy() {
        return getStrategy(properties.getStorage().getType());
    }

    public void registerStrategy(StorageStrategy strategy) {
        if (strategy == null || StrUtil.isBlank(strategy.getType())) {
            throw new IllegalArgumentException("StorageStrategy type must not be blank");
        }
        String type = normalizeType(strategy.getType());
        strategyMap.put(type, strategy);
        log.info("注册 Excel 存储策略: {}", type);
    }

    private String normalizeType(String storageType) {
        return storageType.trim().toLowerCase(Locale.ROOT);
    }
}
