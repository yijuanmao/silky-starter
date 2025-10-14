package com.silky.starter.rabbitmq.enums;

/**
 * 消息状态
 *
 * @author zy
 * @date 2025-10-13 18:04
 **/
public enum MessageStatus {

    /**
     * 待发送
     */
    PENDING,

    /**
     * 发送中
     */
    SENDING,

    /**
     * 发送成功
     */
    SENT,

    /**
     * 发送失败
     */
    FAILED,

    /**
     * 已消费
     */
    CONSUMED,

    /**
     * 消费失败
     */
    CONSUME_FAILED
}
