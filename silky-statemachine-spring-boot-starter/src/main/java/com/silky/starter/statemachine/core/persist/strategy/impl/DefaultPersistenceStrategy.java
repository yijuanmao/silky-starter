package com.silky.starter.statemachine.core.persist.strategy.impl;

import com.silky.starter.statemachine.core.context.StateMachineContext;
import com.silky.starter.statemachine.core.persist.strategy.BusinessPersistenceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认业务持久化策略
 *
 * @author zy
 * @date 2025-09-26 15:05
 **/
public class DefaultPersistenceStrategy implements BusinessPersistenceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPersistenceStrategy.class);

    // 内存存储，用于测试或简单业务
    private final Map<String, String> stateStore = new ConcurrentHashMap<>();
    private final Map<String, String> extendedStateStore = new ConcurrentHashMap<>();

    @Override
    public boolean supports(String machineType) {
        // 默认策略支持所有类型，但优先级最低
        return true;
    }

    @Override
    public String readState(StateMachineContext context) {
        String key = buildStateKey(context);
        String state = stateStore.get(key);

        logger.debug("Reading state from default storage for key: {}, state: {}", key, state);

        return state;
    }

    @Override
    public void writeState(String state, StateMachineContext context) {
        String key = buildStateKey(context);

        logger.debug("Writing state to default storage for key: {}, state: {}", key, state);

        stateStore.put(key, state);

        logger.info("Successfully wrote state to default storage for key: {}, state: {}", key, state);
    }

    @Override
    public String readExtendedState(StateMachineContext context) {
        String key = buildExtendedStateKey(context);
        return extendedStateStore.get(key);
    }

    @Override
    public void writeExtendedState(String extendedState, StateMachineContext context) {
        String key = buildExtendedStateKey(context);
        extendedStateStore.put(key, extendedState);
    }

    @Override
    public String[] getSupportedMachineTypes() {
        // 默认策略声称支持所有类型，但实际只在没有专用策略时使用
        return new String[]{"*"};
    }

    /**
     * 构建状态存储键
     */
    private String buildStateKey(StateMachineContext context) {
        return String.format("default:%s:%s:state", context.getMachineType(), context.getBusinessId());
    }

    /**
     * 构建扩展状态存储键
     */
    private String buildExtendedStateKey(StateMachineContext context) {
        return String.format("default:%s:%s:extended", context.getMachineType(), context.getBusinessId());
    }
}
