package com.silky.starter.statemachine.trade;

import com.silky.starter.statemachine.core.context.StateMachineContext;
import com.silky.starter.statemachine.core.persist.strategy.BusinessPersistenceStrategy;
import com.silky.starter.statemachine.trade.entity.TradeOrder;
import com.silky.starter.statemachine.trade.service.TradeOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 交易订单持久化策略
 *
 * @author zy
 * @date 2025-09-26 16:03
 **/
@Slf4j
@Component
public class TradeOrderPersistenceStrategy implements BusinessPersistenceStrategy {

    private static final int orderYears = 202509;

    @Autowired
    private TradeOrderService tradeOrderService;

    @Override
    public boolean supports(String machineType) {
        return "TRADE".equals(machineType);
    }

    @Override
    public String readState(StateMachineContext stateMachineContext) {
        TradeOrder order = tradeOrderService.selectTradeOrderById(Long.valueOf(stateMachineContext.getBusinessId()));
        return order.getOrderStatus();
    }

    @Override
    public void writeState(String s, StateMachineContext stateMachineContext) {

        TradeOrder order = tradeOrderService.selectTradeOrderById(Long.valueOf(stateMachineContext.getBusinessId()));
        if (order == null) {
            throw new RuntimeException("Payment not found: " + stateMachineContext.getBusinessId());
        }
        String orderStatus = order.getOrderStatus();
        if (!orderStatus.equals(s)) {
            order.setOrderStatus(s);
            tradeOrderService.updateTradeOrder(order);
            log.info("TradeOrder state updated: id={}, from {} to {}", order.getId(), orderStatus, s);
        } else {
            // 状态相同，不更新
            log.info("TradeOrder state unchanged: id={}, state={}", order.getId(), orderStatus);
        }
    }

    @Override
    public String[] getSupportedMachineTypes() {
        return new String[]{"TRADE", "TRADE_V2"};
    }
}
