package com.silky.starter.rabbitmq.test;

import com.silky.starter.rabbitmq.RabbitMqApplicationTest;
import com.silky.starter.rabbitmq.template.RabbitSendTemplate;
import com.silky.starter.rabbitmq.test.entity.TradeOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

/**
 * Rabbit消息发送模板测试类
 *
 * @author zy
 * @date 2025-10-15 15:07
 **/
public class RabbitSendTemplateTest extends RabbitMqApplicationTest {
    @Autowired
    private RabbitSendTemplate rabbitSendTemplate;

    private final static String exchange = "test-exchange";

    private final static String routingKey = "test-routingKey";

    /**
     * 测试发送消息
     */
    @Test
    public void testSend() {
        TradeOrder order = new TradeOrder(1L, LocalDateTime.now(), "测试MQ发送");
        rabbitSendTemplate.send(exchange, routingKey, order);
    }
}
