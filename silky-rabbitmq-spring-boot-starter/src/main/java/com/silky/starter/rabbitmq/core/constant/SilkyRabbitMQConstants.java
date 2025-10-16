package com.silky.starter.rabbitmq.core.constant;

/**
 * RabbitMQ常量
 *
 * @author zy
 * @date 2025-10-16 17:20
 **/
public class SilkyRabbitMQConstants {

    /**
     * 死信队列
     */
    public static final String SILKY_DEFAULT_DLX_QUEUE = "silky.default.dlx.queue";

    /**
     * 死信交换机
     */
    public static final String SILKY_DEFAULT_DLX_EXCHANGE = "silky.default.dlx.exchange";

    /**
     * 死信路由键
     */
    public final static String SILKY_DEFAULT_DLX_ROUTING_KEY = "silky.default.dlx.routingKey";
}
