package com.silky.starter.statemachine.factory;

import com.silky.starter.statemachine.core.config.StateMachineConfig;
import com.silky.starter.statemachine.core.generic.GenericStateMachine;
import com.silky.starter.statemachine.core.persist.StateMachinePersist;
import com.silky.starter.statemachine.registry.StateMachineRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

/**
 * 状态机工厂类
 *
 * @author zy
 * @date 2025-09-12 16:29
 **/
public class StateMachineFactory {

    private final Map<String, StateMachineConfig> configMap;

    private final ApplicationEventPublisher eventPublisher;

    private final ApplicationContext applicationContext;

    @Autowired
    public StateMachineFactory(StateMachineRegistry registry,
                               ApplicationEventPublisher eventPublisher,
                               ApplicationContext applicationContext) {
        this.configMap = registry.getAllConfigs();
        this.eventPublisher = eventPublisher;
        this.applicationContext = applicationContext;
    }

    /**
     * 创建状态机实例
     */
    public GenericStateMachine create(String machineType) {
        StateMachineConfig config = configMap.get(machineType);
        if (config == null) {
            throw new IllegalArgumentException("No state machine config found for type: " + machineType);
        }
        // 获取持久化Bean
        StateMachinePersist persist = applicationContext.getBean(config.getPersistBeanName(), StateMachinePersist.class);
        return new GenericStateMachine(config, persist, eventPublisher);
    }

    /**
     * 检查是否支持指定的状态机类型
     */
    public boolean supports(String machineType) {
        return configMap.containsKey(machineType);
    }

    /**
     * 获取所有支持的状态机类型
     */
    public String[] getSupportedMachineTypes() {
        return configMap.keySet().toArray(new String[0]);
    }
}
