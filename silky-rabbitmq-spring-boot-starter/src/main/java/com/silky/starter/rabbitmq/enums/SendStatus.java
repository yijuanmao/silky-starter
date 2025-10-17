package com.silky.starter.rabbitmq.enums;

/**
 * 消息发送状态
 *
 * @author zy
 * @date 2025-10-13 18:04
 **/
public enum SendStatus {

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

}
