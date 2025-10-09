package com.silky.starter.statemachine.trade;

import com.silky.starter.statemachine.core.config.StateMachineConfig;
import com.silky.starter.statemachine.core.transition.StateTransition;
import com.silky.starter.statemachine.trade.enums.OrderEventEnum;
import com.silky.starter.statemachine.trade.enums.OrderStatusEnum;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 交易订单状态机配置
 *
 * @author zy
 * @date 2025-09-26 15:55
 **/
@Component
public class TradeOrderStateMachineConfig implements StateMachineConfig {

    @Override
    public String getInitialState() {
        return OrderStatusEnum.NOT_PAY.name();
    }

    @Override
    public Map<String, StateTransition> getTransitions() {
        Map<String, StateTransition> transitions = new HashMap<>();

        // 创建订单 -> 待支付
        StateTransition createTransition = new StateTransition(
                OrderStatusEnum.NOT_PAY.name(),
                OrderEventEnum.CREATE.name(),
                OrderStatusEnum.PAYING.name()
        );
        createTransition.setDescription("创建订单并进入待支付状态");
        transitions.put(OrderEventEnum.CREATE.name(), createTransition);

        // 支付成功 -> 已支付
        StateTransition paySuccessTransition = new StateTransition(
                OrderStatusEnum.PAYING.name(),
                OrderEventEnum.PAY_SUCCESS.name(),
                OrderStatusEnum.SUCCESS.name()
        );
        paySuccessTransition.setDescription("支付成功，订单进入已支付状态");
        transitions.put(OrderEventEnum.PAY_SUCCESS.name(), paySuccessTransition);

        // 支付失败 -> 支付失败
        StateTransition payFailedTransition = new StateTransition(
                OrderStatusEnum.PAYING.name(),
                OrderEventEnum.PAY_FAILED.name(),
                OrderStatusEnum.FAIL.name()
        );
        payFailedTransition.setDescription("支付失败，订单进入支付失败状态");
        transitions.put(OrderEventEnum.PAY_FAILED.name(), payFailedTransition);

        return transitions;
    }

    @Override
    public String getMachineType() {
        return "TRADE";
    }

}
