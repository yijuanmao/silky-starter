package com.silky.starter.statemachine.core.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.statemachine.core.transition.StateTransition;

import java.util.Map;

/**
 * 状态机配置
 *
 * @author zy
 * @date 2025-09-12 16:06
 **/
public interface StateMachineConfig {

    /**
     * 获取初始状态
     */
    String getInitialState();

    /**
     * 获取所有可能的转换
     */
    Map<String, StateTransition> getTransitions();

    /**
     * 获取状态机类型
     */
    String getMachineType();

    /**
     * 获取持久化Bean名称
     */
    default String getPersistBeanName() {
        return "stateMachinePersist";
    }

    /**
     * 根据事件获取转换定义
     */
    default StateTransition getTransition(String event) {
        return getTransitions().get(event);
    }

    /**
     * 验证配置有效性
     */
    default boolean isValid() {
        if (StrUtil.isBlank(getInitialState()) || MapUtil.isEmpty(getTransitions())) {
            return false;
        }
        return getTransitions().values().stream().allMatch(StateTransition::isValid);
    }
}
