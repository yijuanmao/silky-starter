package com.silky.starter.rabbitmq.properties;

import com.silky.starter.rabbitmq.core.constant.SilkyRabbitMQConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RabbitMQ 监听器配置属性
 *
 * @author zy
 * @date 2025-10-18 10:36
 **/
@Data
@ConfigurationProperties(prefix = SilkyRabbitListenerProperties.LISTENER_SIMPLE_PREFIX)
public class SilkyRabbitListenerProperties {

    public static final String LISTENER_SIMPLE_PREFIX = "spring.rabbitmq.listener.silky";

    /**
     * 是否启用死信队列
     */
    private boolean enableDlx = false;

    /**
     * 死信队列交换机名称
     */
    private String dlxExchange = SilkyRabbitMQConstants.SILKY_DEFAULT_DLX_EXCHANGE;

    /**
     * 死信队列路由键
     */
    private String dlxRoutingKey = SilkyRabbitMQConstants.SILKY_DEFAULT_DLX_ROUTING_KEY;

    /**
     * 死信队列消息过期时间，单位毫秒，默认120000毫秒（2分钟）
     */
    private long dlxMessageTtl = 0;
}
