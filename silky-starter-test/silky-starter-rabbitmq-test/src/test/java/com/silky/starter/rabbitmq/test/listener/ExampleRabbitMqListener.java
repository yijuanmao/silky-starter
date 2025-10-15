package com.silky.starter.rabbitmq.test.listener;

import com.silky.starter.rabbitmq.service.AbstractMessageReceiver;
import com.silky.starter.rabbitmq.test.config.RabbitMqBindConfig;
import com.silky.starter.rabbitmq.test.entity.TradeOrder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单监听器
 *
 * @author zy
 * @date 2025-10-15 16:15
 **/
@Component
@RabbitListener(queues = RabbitMqBindConfig.EXAMPLE_ORDER_QUEUE)
public class ExampleRabbitMqListener extends AbstractMessageReceiver<TradeOrder> {

    /**
     * 处理消息
     * @param message 消息
     */
    @Override
    protected void handleMessage(TradeOrder message) {
        // 处理订单消息
        logger.info("处理订单消息: orderId={}, orderName={}",
                message.getOrderId(), message.getOrderName());

        System.out.println("处理订单消息: orderId=" + message.getOrderId() + ", orderName=" + message.getOrderName());

        // 业务逻辑...
    }

}



