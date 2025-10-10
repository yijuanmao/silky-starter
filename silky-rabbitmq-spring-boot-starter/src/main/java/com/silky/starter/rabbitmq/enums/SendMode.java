package com.silky.starter.rabbitmq.enums;

/**
 * 发送模式
 *
 * @author zy
 * @date 2025-10-09 17:49
 **/
public enum SendMode {

    /**
     * 同步发送
     */
    SYNC,

    /**
     * 异步发送
     */
    ASYNC,

    /**
     * 根据配置自动选择
     */
    AUTO
}
