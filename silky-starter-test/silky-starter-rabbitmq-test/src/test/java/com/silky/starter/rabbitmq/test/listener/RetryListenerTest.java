package com.silky.starter.rabbitmq.test.listener;

import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.listener.AbstractRabbitMQListener;
import com.silky.starter.rabbitmq.test.config.RabbitMqBindConfig;
import com.silky.starter.rabbitmq.test.entity.TradeOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

/**
 * 重试消息监听器
 *
 * @author zy
 * @date 2025-10-16 11:41
 **/
@Slf4j
@Component
public class RetryListenerTest extends AbstractRabbitMQListener<TradeOrder> {

    /**
     * 指定业务队列
     */
    public RetryListenerTest() {
        super(RabbitMqBindConfig.EXAMPLE_RETRY_ORDER_QUEUE);
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
        log.info("重试消息监听器接收到消息: {}", message);

        //开始处理业务
        this.handleBusiness(message);
    }


    /**
     * 处理业务
     *
     * @param message 消息对象
     */
    private void handleBusiness(TradeOrder message) {
        log.info("重试消息监听器接收到消息: {}", message);
        throw new RuntimeException("处理业务异常" + message);
    }
}
