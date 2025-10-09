package com.silky.starter.statemachine.core.listener;

import com.silky.starter.statemachine.core.handler.StateChangeHandler;
import com.silky.starter.statemachine.event.StateChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

import java.util.List;

/**
 * 状态改变事件监听器
 *
 * @author zy
 * @date 2025-09-12 16:23
 **/
@Slf4j
public class StateChangeEventListener {

    private final List<StateChangeHandler> handlers;

    public StateChangeEventListener(List<StateChangeHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * 处理状态变更事件
     *
     * @param event 状态变更事件
     */
    @EventListener
    public void handleStateChange(StateChangeEvent event) {
        log.info("State changed: {} -> {} by event {} on machine type {}",
                event.getSourceState(), event.getTargetState(),
                event.getEvent(), event.getMachineType());

        // 调用所有支持该类型状态机的处理器
        handlers.stream()
                .filter(handler -> handler.supports(event.getMachineType()))
                .forEach(handler -> {
                    try {
                        handler.onStateChange(event);
                    } catch (Exception e) {
                        log.error("Error handling state change event", e);
                    }
                });
    }
}
