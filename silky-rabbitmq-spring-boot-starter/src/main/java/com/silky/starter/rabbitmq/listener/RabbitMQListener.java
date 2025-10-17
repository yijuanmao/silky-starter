package com.silky.starter.rabbitmq.listener;

import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import com.silky.starter.rabbitmq.listener.config.ListenerConfig;
import org.springframework.amqp.core.Message;

/**
 * RabbitMQ 消息监听器接口
 *
 * @param <T> 消息类型，必须继承 BaseMassageSend
 * @author zy
 * @date 2025-10-16 10:39
 **/
public interface RabbitMQListener<T extends BaseMassageSend> {

    /**
     * 处理消息
     *
     * @param message     消息对象
     * @param channel     RabbitMQ通道
     * @param amqpMessage 原始AMQP消息
     */
    void onMessage(T message, Channel channel, Message amqpMessage);

    /**
     * 获取监听的队列名称
     *
     * @return 队列名称
     */
    String getQueueName();

    /**
     * 获取消息类型
     *
     * @return 消息类类型
     */
    Class<T> getMessageType();

    /**
     * 监听器配置
     */
    default ListenerConfig getConfig() {
        return ListenerConfig.defaultConfig();
    }

    /**
     * 是否启用此监听器
     */
    default boolean enabled() {
        return true;
    }
}
