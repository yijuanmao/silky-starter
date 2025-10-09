package com.silky.starter.statemachine.core.handler;


import com.silky.starter.statemachine.event.StateChangeEvent;

/**
 * 状态变更处理器
 *
 * @author zy
 * @date 2025-09-12 16:23
 **/
public interface StateChangeHandler {

    /**
     * 是否支持处理指定类型的状态机事件
     *
     * @param machineType 状态机类型
     */
    boolean supports(String machineType);

    /**
     * 处理状态变更事件
     *
     * @param event 状态变更事件
     */
    void onStateChange(StateChangeEvent event);
}
