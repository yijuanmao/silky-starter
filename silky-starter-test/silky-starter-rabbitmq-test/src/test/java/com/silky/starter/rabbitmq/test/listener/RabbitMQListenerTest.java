package com.silky.starter.rabbitmq.test.listener;

import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.listener.RabbitMQListener;
import com.silky.starter.rabbitmq.test.config.RabbitMqBindConfig;
import com.silky.starter.rabbitmq.test.entity.TradeOrder;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

/**
 * 单条消息监听器
 *
 * @author zy
 * @date 2025-10-16 11:41
 **/
@Component
public class RabbitMQListenerTest implements RabbitMQListener<TradeOrder> {

    /**
     * 处理消息
     *
     * @param message     消息对象
     * @param channel     RabbitMQ通道
     * @param amqpMessage 原始AMQP消息
     */
    @Override
    public void onMessage(TradeOrder message, Channel channel, Message amqpMessage) {

    }

    /**
     * 获取消息类型
     *
     * @return 消息类类型
     */
    @Override
    public Class<TradeOrder> getMessageType() {
        return TradeOrder.class;
    }

    /**
     * 获取监听的队列名称
     *
     * @return 队列名称
     */
    @Override
    public String getQueueName() {
        return RabbitMqBindConfig.EXAMPLE_ORDER_QUEUE;
    }

    /**
     * 是否自动确认消息
     *
     * @return true: 自动确认, false: 手动确认
     */
    @Override
    public boolean autoAck() {
        return RabbitMQListener.super.autoAck();
    }

    /**
     * 消费失败时的重试次数
     *
     * @return 重试次数，默认3次
     */
    @Override
    public int retryTimes() {
        return RabbitMQListener.super.retryTimes();
    }

    /**
     * 消费失败时的重试间隔（毫秒）
     *
     * @return 重试间隔，默认1000ms
     */
    @Override
    public long retryInterval() {
        return RabbitMQListener.super.retryInterval();
    }

    /**
     * 是否启用死信队列
     *
     * @return true: 启用, false: 不启用
     */
    @Override
    public boolean enableDlx() {
        return false;
    }
}
