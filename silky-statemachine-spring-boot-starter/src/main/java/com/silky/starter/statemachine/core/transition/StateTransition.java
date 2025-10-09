package com.silky.starter.statemachine.core.transition;

import com.silky.starter.statemachine.core.action.TransitionAction;
import com.silky.starter.statemachine.core.guard.TransitionGuard;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * 状态转换
 *
 * @author zy
 * @date 2025-09-12 16:08
 **/
@Data
@ToString
public class StateTransition {

    /**
     * 当前状态
     */
    private String currentState;

    /**
     * 触发事件
     */
    private String event;

    /**
     * 下一个状态
     */
    private String nextState;

    /**
     * 触发条件
     */
    private TransitionGuard guard;

    /**
     * 触发动作
     */
    private List<TransitionAction> actions = new ArrayList<>();

    /**
     * 转换描述（可选）
     */
    private String description;

    public StateTransition() {
    }

    // 便捷构造函数
    public StateTransition(String currentState, String event, String nextState) {
        this.currentState = currentState;
        this.event = event;
        this.nextState = nextState;
    }

    /**
     * 验证转换是否有效
     */
    public boolean isValid() {
        return currentState != null && event != null && nextState != null;
    }
}
