package com.silky.starter.rabbitmq.test.listener;

import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.exception.RabbitMessageSendException;
import com.silky.starter.rabbitmq.listener.AbstractRabbitMQListener;
import com.silky.starter.rabbitmq.listener.util.ManualAckHelper;
import com.silky.starter.rabbitmq.test.config.RabbitMqBindConfig;
import com.silky.starter.rabbitmq.test.entity.TradeOrder;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

/**
 * 订单消息监听器（手动确认模式）
 *
 * @author zy
 * @date 2025-10-15 16:15
 **/
@Component
public class OrderMessageListenerTest extends AbstractRabbitMQListener<TradeOrder> {

    public OrderMessageListenerTest() {
        super(RabbitMqBindConfig.EXAMPLE_ORDER_QUEUE);
    }

    /**
     * 处理消息
     *
     * @param message     消息对象
     * @param channel     RabbitMQ通道
     * @param amqpMessage 原始AMQP消息
     */
    @Override
    public void onMessage(TradeOrder message, Channel channel, Message amqpMessage) {
        logger.info("Processing order message: orderId={}, orderName={}, amount={}",
                message.getOrderId(), message.getOrderName(), message.getAmount());

        try {
            // 处理订单逻辑

            // 手动确认消息处理成功
            ManualAckHelper.ackSuccess(channel, amqpMessage, "ORDER_PROCESS_SUCCESS");

            logger.info("Order processed successfully: {}", message.getOrderId());

            logger.info("Order processed successfully: {}", message.getOrderId());
        } catch (RabbitMessageSendException e) {
            // 比如：RabbitMessageSendException 是业务异常，重新入队重试
            logger.warn("Business exception processing order: {}, will retry", message.getOrderId(), e);
            ManualAckHelper.rejectAndRequeue(channel, amqpMessage, "BUSINESS_EXCEPTION: " + e.getMessage());
        } catch (Exception e) {
            // 系统异常，进入死信队列
            logger.error("System exception processing order: {}", message.getOrderId(), e);
            ManualAckHelper.rejectToDlx(channel, amqpMessage, "SYSTEM_EXCEPTION: " + e.getMessage());
        }
    }

    /**
     * 获取消息类型
     *
     * @return 消息类类型
     */
    @Override
    public boolean autoAck() {
        return false; // 手动确认模式
    }

    /**
     * 消费失败时的重试次数
     *
     * @return 重试次数，默认3次
     */
    @Override
    public int retryTimes() {
        return 3; // 最大重试3次
    }

    /**
     * 消费失败时的重试间隔（毫秒）
     *
     * @return 重试间隔，默认1000ms
     */
    @Override
    public long retryInterval() {
        return 5000L; // 重试间隔5秒
    }
}



