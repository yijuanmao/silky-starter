package com.silky.starter.statemachine.core.generic;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.statemachine.core.action.TransitionAction;
import com.silky.starter.statemachine.core.config.StateMachineConfig;
import com.silky.starter.statemachine.core.context.StateMachineContext;
import com.silky.starter.statemachine.core.persist.StateMachinePersist;
import com.silky.starter.statemachine.core.transition.StateTransition;
import com.silky.starter.statemachine.event.StateChangeEvent;
import com.silky.starter.statemachine.exception.StateMachineException;
import lombok.Getter;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 通用状态机
 *
 * @author zy
 * @date 2025-09-12 16:09
 **/
public class GenericStateMachine {

    @Getter
    private final StateMachineConfig config;

    @Getter
    private final StateMachinePersist persist;

    private final ApplicationEventPublisher eventPublisher;

    public GenericStateMachine(StateMachineConfig config,
                               StateMachinePersist persist,
                               ApplicationEventPublisher eventPublisher) {
        this.config = config;
        this.persist = persist;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 触发事件
     *
     * @param event   事件
     * @param context 上下文
     * @return 上下文
     */
    public StateMachineContext trigger(String event, StateMachineContext context) {
        String currentState = persist.readState(context);
        if (StrUtil.isBlank(currentState)) {
            currentState = config.getInitialState();
        }
        // 获取转换定义
        StateTransition transition = config.getTransition(event);
        if (transition == null) {
            throw new StateMachineException("No transition defined for event: " + event);
        }
        // 验证当前状态
        if (!transition.getCurrentState().equals(currentState)) {
            throw new StateMachineException(String.format("Invalid transition from state '%s' with event '%s'. Expected state: '%s'", currentState, event, transition.getCurrentState()));
        }
        // 检查守卫条件
        if (transition.getGuard() != null && !transition.getGuard().evaluate(context)) {
            throw new StateMachineException("Guard condition not satisfied for event: " + event);
        }
        // 执行转换前动作
        if (CollUtil.isNotEmpty(transition.getActions())) {
            for (TransitionAction action : transition.getActions()) {
                action.execute(currentState, event, context);
            }
        }
        // 更新状态
        String nextState = transition.getNextState();
        persist.writeState(nextState, context);
        // 发布状态变更事件
        publishStateChangeEvent(currentState, nextState, event, context);

        return context;
    }

    /**
     * 获取当前状态
     *
     * @param context 上下文
     */
    public String getCurrentState(StateMachineContext context) {
        String state = persist.readState(context);
        return StrUtil.isBlank(state) ? config.getInitialState() : state;
    }

    /**
     * 发布状态变更事件
     */
    private void publishStateChangeEvent(String fromState, String toState,
                                         String event, StateMachineContext context) {
        StateChangeEvent stateChangeEvent = new StateChangeEvent(
                this, fromState, toState, event, context, config.getMachineType());
        eventPublisher.publishEvent(stateChangeEvent);
    }
}
