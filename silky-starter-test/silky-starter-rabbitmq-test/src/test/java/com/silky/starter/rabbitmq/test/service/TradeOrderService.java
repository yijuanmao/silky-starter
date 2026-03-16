package com.silky.starter.rabbitmq.test.service;

import com.silky.starter.rabbitmq.annotation.RabbitMessage;
import com.silky.starter.rabbitmq.annotation.RabbitPayload;
import com.silky.starter.rabbitmq.test.entity.TradeOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 交易订单测试方法
 *
 * @author: zy
 * @date: 2026-03-16
 */
@Slf4j
@Service
public class TradeOrderService {

    /**
     * 使用mq注解发送消息测试方法,参数必须使用@RabbitPayload注解，暂时只支持单个参数
     *
     * @param order 交易订单
     */
    @RabbitMessage(exchange = "example-exchange",
            routingKey = "example-routingKey",
            businessType = "测试",
            description = "描述")
    public void testSendMq(@RabbitPayload TradeOrder order) {
        log.info("使用mq注解发送消息测试方法接收到的消息：{}", order);
    }
}
