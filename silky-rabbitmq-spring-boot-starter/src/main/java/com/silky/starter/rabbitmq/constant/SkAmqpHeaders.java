package com.silky.starter.rabbitmq.constant;

import org.springframework.amqp.support.AmqpHeaders;

/**
 * SkRabbitMQ-AmqpHeaders 类用于定义 RabbitMQ 相关的常量
 *
 * @author: zy
 * @date: 2026-03-16
 */
public class SkAmqpHeaders extends AmqpHeaders {

    /**
     * 业务类型
     */
    public static final String BUSINESS_TYPE = "businessType";

    /**
     * 业务描述
     */
    public static final String DESCRIPTION = "description";

    /**
     * 消息来源
     */
    public static final String SOURCE = "source";

    /**
     * 消息扩展数据
     */
    public static final String EXT_DATA = "extData";
}
