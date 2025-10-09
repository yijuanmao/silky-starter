package com.silky.starter.statemachine.core.persist.strategy;


import com.silky.starter.statemachine.core.context.StateMachineContext;

/**
 * 业务持久化策略
 *
 * @author zy
 * @date 2025-09-26 14:33
 **/
public interface BusinessPersistenceStrategy {

    /**
     * 是否支持指定的业务类型
     */
    boolean supports(String machineType);

    /**
     * 读取状态
     */
    String readState(StateMachineContext context);

    /**
     * 写入状态
     */
    void writeState(String state, StateMachineContext context);

    /**
     * 读取扩展数据
     */
    default String readExtendedState(StateMachineContext context) {
        return null;
    }

    /**
     * 写入扩展数据
     */
    default void writeExtendedState(String extendedState, StateMachineContext context) {
        // 默认空实现
    }

    /**
     * 获取策略名称
     */
    default String getStrategyName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取支持的机器类型列表
     */
    String[] getSupportedMachineTypes();
}
