package com.silky.starter.rabbitmq.template;

import com.silky.starter.rabbitmq.core.model.MassageSendParam;
import com.silky.starter.rabbitmq.core.model.SendResult;
import com.silky.starter.rabbitmq.enums.SendMode;
import com.silky.starter.rabbitmq.service.SendCallback;

/**
 * 消息发送模板接口
 *
 * @author zy
 * @date 2025-10-09 17:52
 **/
public interface SkRabbitMqTemplate {

    /**
     * 发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息参数
     */
    SendResult send(String exchange, String routingKey, Object message);


    /**
     * 发送消息（指定发送模式）
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     * @param sendMode   发送模式
     */
    SendResult send(String exchange, String routingKey, Object message, SendMode sendMode);

    /**
     * 发送消息
     *
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param message      消息体
     * @param businessType 业务类型
     * @param description  描述
     * @param sendMode     发送模式
     */
    SendResult send(String exchange, String routingKey, Object message, String businessType, String description, SendMode sendMode);

    /**
     * 发送延迟消息
     *
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param message      消息体
     * @param delayMillis  延迟时间
     * @param businessType 业务类型
     * @param description  描述
     */
    SendResult sendDelay(String exchange, String routingKey, Object message, long delayMillis, String businessType, String description);

    /**
     * 异步发送消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     */
    void sendAsync(String exchange, String routingKey, Object message);

    /**
     * 异步发送消息（带回调）
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息体
     * @param callback   回调
     */
    void sendAsync(String exchange, String routingKey, Object message, SendCallback callback);

    /**
     * 发送消息
     *
     * @param param 消息体
     */
    SendResult send(MassageSendParam param);
}
