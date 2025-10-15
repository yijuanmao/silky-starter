package com.silky.starter.rabbitmq.properties;

import com.silky.starter.rabbitmq.enums.SendMode;
import lombok.Data;

/**
 * 消息发送配置属性
 *
 * @author zy
 * @date 2025-10-09 18:07
 **/
@Data
public class SendProperties {

    /**
     * 是否启用消息发送功能 默认为true
     */
    private boolean enabled = true;

    /**
     * 默认发送模式
     */
    private SendMode defaultSendMode = SendMode.AUTO;

    /**
     * 同步发送超时时间 默认3秒
     */
    private long syncTimeout = 3000L;

    /**
     * 异步发送线程池大小 默认10
     */
    private int asyncThreadPoolSize = 10;

    /**
     * 是否启用重试机制 默认启用
     */
    private boolean enableRetry = true;

    /**
     * 最大重试次数 默认3次
     */
    private int maxRetryCount = 3;

    /**
     * 重试间隔时间（毫秒） 默认1000毫秒
     */
    private long retryInterval = 1000L;

    /**
     * 是否启用超时控制 默认启用
     */
    private boolean useTimeout = true;
}
