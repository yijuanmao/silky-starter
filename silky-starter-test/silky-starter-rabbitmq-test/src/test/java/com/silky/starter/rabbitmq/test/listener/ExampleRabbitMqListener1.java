package com.silky.starter.rabbitmq.test.listener;

import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.test.config.RabbitMqBindConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 订单监听器
 *
 * @author zy
 * @date 2025-10-15 16:15
 **/
@Component
public class ExampleRabbitMqListener1 {

    @RabbitListener(queues = RabbitMqBindConfig.EXAMPLE_ORDER_QUEUE)
    public void fileExportListener(String obj, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        System.out.println("进入订单消息1,msg：" + obj + " tag:" + tag);
    }

}



