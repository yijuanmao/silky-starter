package com.silky.starter.statemachine.core.persist;


import com.silky.starter.statemachine.core.context.StateMachineContext;

/**
 * 状态持久化接口
 *
 * @author zy
 * @date 2025-09-12 16:10
 **/
public interface StateMachinePersist {

    /**
     * 读取状态
     *
     * @param context 状态机上下文
     * @return 状态值
     */
    String readState(StateMachineContext context);

    /**
     * 写入状态
     *
     * @param state   状态值
     * @param context 状态机上下文
     */
    void writeState(String state, StateMachineContext context);

    /**
     * 读取扩展数据
     *
     * @param context 状态机上下文
     * @return 扩展数据JSON字符串
     */
    default String readExtendedState(StateMachineContext context) {
        return null;
    }

    /**
     * 写入扩展数据
     *
     * @param extendedState 扩展数据JSON字符串
     * @param context       状态机上下文
     */
    default void writeExtendedState(String extendedState, StateMachineContext context) {
        // 默认空实现
    }

    /**
     * 判断是否支持指定的状态机类型
     */
    default boolean supports(String machineType) {
        return true;
    }
}
