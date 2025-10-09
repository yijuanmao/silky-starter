package com.silky.starter.statemachine.trade;

import com.silky.starter.statemachine.StatemachineApplicationTest;
import com.silky.starter.statemachine.core.context.StateMachineContext;
import com.silky.starter.statemachine.core.generic.GenericStateMachine;
import com.silky.starter.statemachine.factory.StateMachineFactory;
import com.silky.starter.statemachine.trade.entity.TradeOrder;
import com.silky.starter.statemachine.trade.enums.OrderEventEnum;
import com.silky.starter.statemachine.trade.enums.OrderStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 交易订单状态机测试类
 *
 * @author zy
 * @date 2025-10-09 14:59
 **/
public class TradeOrderStateMachineTest extends StatemachineApplicationTest {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    /**
     * 状态机测试
     */
    @Test
    public void testStatemachine() {
        TradeOrder order = new TradeOrder(1L, OrderStatusEnum.PAYING.name());

        // 创建状态机上下文
        StateMachineContext context = new StateMachineContext()
                .setMachineType("TRADE")
                .setBusinessId(order.getId().toString());

        GenericStateMachine stateMachine = stateMachineFactory.create("TRADE");
        StateMachineContext orderContext = stateMachine.trigger(OrderEventEnum.PAY_SUCCESS.name(), context);
        log.info("============= 状态机状态操作 ============={}", orderContext);
    }
}
