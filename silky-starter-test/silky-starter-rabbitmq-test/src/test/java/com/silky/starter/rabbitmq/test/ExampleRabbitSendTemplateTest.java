package com.silky.starter.rabbitmq.test;

import cn.hutool.core.thread.ThreadUtil;
import com.silky.starter.rabbitmq.RabbitMqApplicationTest;
import com.silky.starter.rabbitmq.core.model.SendResult;
import com.silky.starter.rabbitmq.enums.SendMode;
import com.silky.starter.rabbitmq.template.RabbitSendTemplate;
import com.silky.starter.rabbitmq.test.config.RabbitMqBindConfig;
import com.silky.starter.rabbitmq.test.entity.TradeOrder;
import com.silky.starter.rabbitmq.test.service.TestSendCallback;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Rabbit消息发送模板测试类
 *
 * @author zy
 * @date 2025-10-15 15:07
 **/
public class ExampleRabbitSendTemplateTest extends RabbitMqApplicationTest {

    private static final String exchange = RabbitMqBindConfig.EXAMPLE_EXCHANGE;
    private static final String routingKey = RabbitMqBindConfig.EXAMPLE_ROUTING_KEY;

    private static final String delayExchange = RabbitMqBindConfig.EXAMPLE_DELAY_EXCHANGE;
    private static final String delayRoutingKey = RabbitMqBindConfig.EXAMPLE_DELAY_ROUTING_KEY;


    @Autowired
    private RabbitSendTemplate rabbitSendTemplate;


    /**
     * 普通发送消息测试方法
     */
    @Test
    public void testSend() {
        //普通发送消息
        TradeOrder order = new TradeOrder(3L, LocalDateTime.now(), "测试MQ发送3", BigDecimal.ONE);

        //普通发送消息
        SendResult send1 = rabbitSendTemplate.send(exchange, routingKey, order);
        log.info("普通发送消息测试方法发送结果：{}", send1);

        //普通发送消息，指定发送模式，支持SYNC、ASYNC、AUTO
        SendResult send2 = rabbitSendTemplate.send(exchange, routingKey, order, SendMode.ASYNC);
        log.info("普通发送消息，指定发送模式测试方法发送结果：{}", send2);

        //业务类型，比如订单、用户等
        String businessType = "TRADE";
        String description = "silky-测试描述";
        // 带业务类型发送消息
        SendResult send3 = rabbitSendTemplate.send(exchange, routingKey, order, businessType, description);
        log.info("带业务类型发送消息测试方法发送结果：{}", send3);

        // 带业务类型发送消息，指定发送模式，支持SYNC、ASYNC、AUTO
        SendResult send4 = rabbitSendTemplate.send(exchange, routingKey, order, businessType, description, SendMode.ASYNC);
        log.info("带业务类型发送消息，指定发送模式测试方法发送结果：{}", send4);

        ThreadUtil.sleep(200000);
    }

    /**
     * 发送延迟消息测试方法
     */
    @Test
    public void testSendDelay() {
        //业务类型，比如订单、用户等
        String businessType = "TRADE";
        String description = "silky-延迟-测试描述";

        //普通发送消息
        TradeOrder order = new TradeOrder(5L, LocalDateTime.now(), "测试MQ发送-延迟消息测试", BigDecimal.ONE);
        //普通发送消息
        SendResult send1 = rabbitSendTemplate.sendDelay(delayExchange, delayRoutingKey, order, 7000L, businessType, description);
        log.info("普通发送延迟消息测试方法发送结果：{}", send1);

        ThreadUtil.sleep(20300);
    }

    /**
     * 异步发送消息测试方法
     */
    @Test
    public void testSendAsyncAndCallback() {
        //普通发送消息
        TradeOrder order = new TradeOrder(5L, LocalDateTime.now(), "测试MQ发送-异步回调测试", BigDecimal.ONE);
        //普通发送消息
        rabbitSendTemplate.sendAsync(exchange, routingKey, order, new TestSendCallback());
        log.info("异步发送消息测试方法完成");

        ThreadUtil.sleep(20300);
    }
}
