package com.silky.starter.statemachine.core.persist.strategy.router;

import com.silky.starter.statemachine.core.persist.strategy.BusinessPersistenceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 持久化策略路由器
 *
 * @author zy
 * @date 2025-09-26 14:54
 **/
public class PersistenceStrategyRouter {

    private static final Logger log = LoggerFactory.getLogger(PersistenceStrategyRouter.class);

    private final Map<String, BusinessPersistenceStrategy> strategyMap = new ConcurrentHashMap<>();

    private final BusinessPersistenceStrategy defaultStrategy;

    public PersistenceStrategyRouter(List<BusinessPersistenceStrategy> strategies,
                                     BusinessPersistenceStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        initStrategyMap(strategies);
    }

    /**
     * 根据机器类型获取策略
     */
    public BusinessPersistenceStrategy getStrategy(String machineType) {
        BusinessPersistenceStrategy strategy = strategyMap.get(machineType);
        if (strategy == null) {
            log.debug("No specific strategy found for machine type: {}, using default strategy", machineType);
            return defaultStrategy;
        }
        return strategy;
    }

    /**
     * 获取所有支持的机器类型
     */
    public String[] getSupportedMachineTypes() {
        return strategyMap.keySet().toArray(new String[0]);
    }

    /**
     * 检查是否支持指定的机器类型
     */
    public boolean supports(String machineType) {
        return strategyMap.containsKey(machineType) || defaultStrategy.supports(machineType);
    }

    /**
     * 初始化策略映射
     */
    private void initStrategyMap(List<BusinessPersistenceStrategy> strategies) {
        for (BusinessPersistenceStrategy strategy : strategies) {
            for (String machineType : strategy.getSupportedMachineTypes()) {
                BusinessPersistenceStrategy existing = strategyMap.put(machineType, strategy);
                if (existing != null) {
                    log.warn("Multiple strategies found for machine type: {}. Using: {}", machineType, strategy.getStrategyName());
                } else {
                    log.info("Registered strategy {} for machine type: {}", strategy.getStrategyName(), machineType);
                }
            }
        }
    }
}
