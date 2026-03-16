package com.silky.starter.rabbitmq.test;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import com.silky.starter.rabbitmq.RabbitMqApplicationTest;
import com.silky.starter.rabbitmq.core.model.MassageSendParam;
import com.silky.starter.rabbitmq.core.model.SendResult;
import com.silky.starter.rabbitmq.enums.SendMode;
import com.silky.starter.rabbitmq.template.SkRabbitMqTemplate;
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
public class ExampleSkRabbitMqTemplateTest extends RabbitMqApplicationTest {

    private static final String exchange = RabbitMqBindConfig.EXAMPLE_EXCHANGE;
    private static final String routingKey = RabbitMqBindConfig.EXAMPLE_ROUTING_KEY;

    private static final String delayExchange = RabbitMqBindConfig.EXAMPLE_DELAY_EXCHANGE;
    private static final String delayRoutingKey = RabbitMqBindConfig.EXAMPLE_DELAY_ROUTING_KEY;


    private static final String RETRY_EXCHANGE = RabbitMqBindConfig.EXAMPLE_RETRY_EXCHANGE;
    private static final String RETRY_ROUTING_KEY = RabbitMqBindConfig.EXAMPLE_RETRY_ROUTING_KEY;

    @Autowired
    private SkRabbitMqTemplate skRabbitMqTemplate;
    @Autowired
    private TestSendCallback testSendCallback;

    /**
     * 普通发送消息测试方法
     */
    @Test
    public void testSend() {

        TradeOrder order = new TradeOrder(3L, LocalDateTime.now(), "测试MQ发送3", BigDecimal.ONE);

        // 1.普通发送消息
        SendResult send1 = skRabbitMqTemplate.send(exchange, routingKey, order);
        log.info("普通发送消息测试方法发送结果：{}", send1);

        // 2.普通发送消息，指定发送模式，支持SYNC、ASYNC、AUTO
        SendResult send2 = skRabbitMqTemplate.send(exchange, routingKey, order, SendMode.ASYNC);
        log.info("普通发送消息，指定发送模式测试方法发送结果：{}", send2);

        // 3.业务类型，比如订单、用户等
        String businessType = "TRADE";
        String description = "silky-测试描述";
        // 带业务类型发送消息，指定发送模式，支持SYNC、ASYNC、AUTO
        SendResult send3 = skRabbitMqTemplate.send(exchange, routingKey, order, businessType, description, SendMode.ASYNC);
        log.info("带业务类型发送消息，指定发送模式测试方法发送结果：{}", send3);

        // 4.带参数发送消息，使用对象MassageSendParam参数
        String messageId = IdUtil.fastSimpleUUID();
        MassageSendParam param = MassageSendParam.builder()
                .body(order)
                //指定消息id，不给的话，底层默认会生成
                .messageId(messageId)
                .exchange(exchange)
                .routingKey(routingKey)
                .sendDelay(false)
                //同步发送
                .sendMode(SendMode.SYNC)
                .businessType(businessType)
                .description(description)
                .build();
        SendResult send4 = skRabbitMqTemplate.send(param);
        log.info("带参数发送消息，指定发送模式测试方法发送结果：{}", send4);
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
        SendResult send1 = skRabbitMqTemplate.sendDelay(delayExchange, delayRoutingKey, order, 7000L, businessType, description);
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
        skRabbitMqTemplate.sendAsync(exchange, routingKey, order, testSendCallback);
        log.info("异步发送消息测试方法完成");

        ThreadUtil.sleep(20300);
    }

    /**
     * 发送并保存到数据库测试方法
     */
    @Test
    public void testSendSaveDb() {
        //普通发送消息
        TradeOrder order = new TradeOrder(6L, LocalDateTime.now(), "测试MQ发送-保存数据库", BigDecimal.ONE);

        //普通发送消息
        SendResult send1 = skRabbitMqTemplate.send(exchange, routingKey, order);
        log.info("发送并保存到数据库测试方法发送结果：{}", send1);

        ThreadUtil.sleep(20300);
    }

    /**
     * 发送重试消息测试方法
     */
    @Test
    public void testRetry() {
        //普通发送消息
        TradeOrder order = new TradeOrder(System.currentTimeMillis(), LocalDateTime.now(), "测试MQ发送-重试消息", BigDecimal.ONE);

        //普通发送消息
        SendResult send1 = skRabbitMqTemplate.send(RETRY_EXCHANGE, RETRY_ROUTING_KEY, order);
        log.info("发送重试消息测试方法发送结果：{}", send1);

//        ThreadUtil.sleep(20300);
    }
}
