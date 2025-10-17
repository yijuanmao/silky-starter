package com.silky.starter.rabbitmq.enums;

/**
 * 消息消费状态
 *
 * @author zy
 * @date 2025-10-17 17:47
 **/
public enum ConsumeStatus {

    /**
     * 已消费
     */
    CONSUMED,

    /**
     * 消费失败
     */
    CONSUME_FAILED
}
