package com.silky.starter.statemachine.registry;


import com.silky.starter.statemachine.core.config.StateMachineConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 状态机配置注册中心
 *
 * @author zy
 * @date 2025-09-12 18:10
 **/
public class StateMachineRegistry {

    private final Map<String, StateMachineConfig> configMap = new ConcurrentHashMap<>(10);

    /**
     * 注册适配器
     *
     * @param machineType 状态机类型
     * @param adapter     适配器
     */
    public void register(String machineType, StateMachineConfig adapter) {
        configMap.put(machineType, adapter);
    }

    /**
     * 获取适配器
     *
     * @param machineType 状态机类型
     * @return 适配器
     */
    public StateMachineConfig getConfig(String machineType) {
        return configMap.get(machineType);
    }

    /**
     * 获取所有适配器
     *
     * @return 适配器映射
     */
    public Map<String, StateMachineConfig> getAllConfigs() {
        return this.configMap;
    }
}
