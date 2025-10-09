package com.silky.starter.statemachine.trade;

import com.silky.starter.statemachine.core.handler.StateChangeHandler;
import com.silky.starter.statemachine.event.StateChangeEvent;
import org.springframework.stereotype.Component;

/**
 * 交易订单状态变更处理器
 *
 * @author zy
 * @date 2025-09-26 16:58
 **/
@Component
public class TradeOrderStateChangeHandler implements StateChangeHandler {

    @Override
    public boolean supports(String s) {
        return "TRADE".equals(s);
    }

    @Override
    public void onStateChange(StateChangeEvent stateChangeEvent) {

        // 在这里处理交易订单的状态变更逻辑
        System.out.println("TradeOrderStateChangeHandler处理状态变更: " + stateChangeEvent);
    }
}
