package com.silky.starter.rabbitmq.test.listener;

import com.alibaba.fastjson2.JSONObject;
import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.test.config.RabbitMqBindConfig;
import com.silky.starter.rabbitmq.test.entity.TradeOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 单条消息监听器
 *
 * @author zy
 * @date 2025-10-16 11:41
 **/
@Slf4j
@Component
public class RabbitMQListener {

    /**
     * 处理消息
     *
     * @param message 消息对象
     * @param channel RabbitMQ通道
     */
    @RabbitListener(queues = RabbitMqBindConfig.EXAMPLE_ORDER_QUEUE)
    public void onMessage(Message message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        String msg = new String(message.getBody());
        log.info("Processing order message: {}", msg);
        // 支付处理逻辑
        try {
            TradeOrder tradeOrder = JSONObject.parseObject(msg, TradeOrder.class);
            log.info("order processed successfully: {}", tradeOrder.getOrderId());
            // 手动确认消费
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process payment: {}", msg, e);
            throw new RuntimeException("order processing failed", e);
        }
    }

}
