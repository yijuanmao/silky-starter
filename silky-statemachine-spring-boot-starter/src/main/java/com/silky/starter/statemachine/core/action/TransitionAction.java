package com.silky.starter.statemachine.core.action;

import com.silky.starter.statemachine.core.context.StateMachineContext;

/**
 * 状态转换动作接口，用于在状态转换过程中执行具体的业务逻辑
 *
 * @author zy
 * @date 2025-09-12 16:19
 **/
@FunctionalInterface
public interface TransitionAction {

    /**
     * 执行转换动作
     *
     * @param fromState 转换前的状态
     * @param event     触发转换的事件
     * @param context   状态机上下文
     */
    void execute(String fromState, String event, StateMachineContext context);

    /**
     * 组合动作（顺序执行）
     */
    static TransitionAction sequence(TransitionAction... actions) {
        return (fromState, event, context) -> {
            for (TransitionAction action : actions) {
                action.execute(fromState, event, context);
            }
        };
    }

    /**
     * 空动作
     */
    static TransitionAction noop() {
        return (fromState, event, context) -> {
            // 什么都不做
        };
    }
}
