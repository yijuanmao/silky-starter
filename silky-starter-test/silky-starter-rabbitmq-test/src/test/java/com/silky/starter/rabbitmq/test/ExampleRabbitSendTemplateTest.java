package com.silky.starter.rabbitmq.test;

import com.silky.starter.rabbitmq.RabbitMqApplicationTest;
import com.silky.starter.rabbitmq.core.SendResult;
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
public class ExampleRabbitSendTemplateTest extends RabbitMqApplicationTest {
    @Autowired
    private RabbitSendTemplate rabbitSendTemplate;

    private final static String exchange = "test-exchange";

    private final static String routingKey = "test-routingKey";

    /**
     * 普通发送消息测试方法
     */
    @Test
    public void testSend() {
        //普通发送消息
        TradeOrder order = new TradeOrder(1L, LocalDateTime.now(), "测试MQ发送");
        SendResult send = rabbitSendTemplate.send(exchange, routingKey, order);
        log.info("普通发送消息测试方法发送结果：{}", send);

        //普通发送消息，指定发送模式，支持SYNC、ASYNC、AUTO
//        rabbitSendTemplate.send(exchange, routingKey, order, SendMode.ASYNC);

        //业务类型，比如订单、用户等
        String businessType = "TRADE";
        String description = "silky-测试描述";
        // 带业务类型发送消息
//        rabbitSendTemplate.send(exchange, routingKey, order, businessType, description);

        // 带业务类型发送消息，指定发送模式，支持SYNC、ASYNC、AUTO
//        rabbitSendTemplate.send(exchange, routingKey, order, businessType, description, SendMode.ASYNC);
    }
}
