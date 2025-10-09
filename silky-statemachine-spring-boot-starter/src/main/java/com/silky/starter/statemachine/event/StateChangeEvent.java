package com.silky.starter.statemachine.event;

import com.silky.starter.statemachine.core.context.StateMachineContext;
import org.springframework.context.ApplicationEvent;

/**
 * 状态变更事件
 *
 * @author zy
 * @date 2025-09-12 16:16
 **/
public class StateChangeEvent extends ApplicationEvent {

    /**
     * 源状态
     */
    private final String sourceState;

    /**
     * 目标状态
     */
    private final String targetState;

    /**
     * 触发事件
     */
    private final String  event;

    /**
     * 上下文
     */
    private final StateMachineContext context;

    /**
     * 状态机类型
     */
    private final String machineType;

    public StateChangeEvent(Object source, String sourceState, String targetState,
                            String event, StateMachineContext context, String machineType) {
        super(source);
        this.sourceState = sourceState;
        this.targetState = targetState;
        this.event = event;
        this.context = context;
        this.machineType = machineType;
    }

    public String getSourceState() {
        return sourceState;
    }

    public String getTargetState() {
        return targetState;
    }

    public String getEvent() {
        return event;
    }

    public StateMachineContext getContext() {
        return context;
    }

    public String getMachineType() {
        return machineType;
    }

    @Override
    public String toString() {
        return "StateChangeEvent{" +
                "sourceState='" + sourceState + '\'' +
                ", targetState='" + targetState + '\'' +
                ", event='" + event + '\'' +
                ", context=" + context +
                ", machineType='" + machineType + '\'' +
                "} " + super.toString();
    }
}
