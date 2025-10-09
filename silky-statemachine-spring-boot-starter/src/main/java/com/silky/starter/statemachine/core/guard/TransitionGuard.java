package com.silky.starter.statemachine.core.guard;


import com.silky.starter.statemachine.core.context.StateMachineContext;

/**
 * 状态转换守卫接口, 用于在状态转换前进行条件检查，只有条件满足时才允许状态转换
 *
 * @author zy
 * @date 2025-09-12 16:27
 **/
@FunctionalInterface
public interface TransitionGuard {

    /**
     * 评估是否允许状态转换
     *
     * @param context 状态机上下文
     * @return true 表示允许转换，false 表示不允许
     */
    boolean evaluate(StateMachineContext context);

    /**
     * 组合多个守卫条件（AND逻辑）
     */
    static TransitionGuard and(TransitionGuard... guards) {
        return context -> {
            for (TransitionGuard guard : guards) {
                if (!guard.evaluate(context)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * 组合多个守卫条件（OR逻辑）
     */
    static TransitionGuard or(TransitionGuard... guards) {
        return context -> {
            for (TransitionGuard guard : guards) {
                if (guard.evaluate(context)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * 取反守卫条件
     */
    static TransitionGuard not(TransitionGuard guard) {
        return context -> !guard.evaluate(context);
    }
}
