//package com.silky.starter.rabbitmq.listener.config;
//
//import com.silky.starter.rabbitmq.core.constant.SilkyRabbitMQConstants;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//
///**
// * 监听器配置
// *
// * @author zy
// * @date 2025-10-17 16:06
// **/
//@Getter
//@Builder
//@AllArgsConstructor
//public class ListenerConfig {
//
//    /**
//     * 是否自动确认
//     */
//    private final boolean autoAck;
//
//    /**
//     * 最大重试次数
//     */
//    private final int maxRetryCount;
//
//    /**
//     * 重试间隔（毫秒）
//     */
//    private final long retryInterval;
//
//    /**
//     * 是否启用死信队列
//     */
//    private final boolean enableDlx;
//
//    /**
//     * 死信队列交换机名称
//     */
//    private final String dlxExchange;
//
//    /**
//     * 死信队列路由键
//     */
//    private final String dlxRoutingKey;
//
//    /**
//     * 并发消费者数量
//     */
//    private final int concurrency;
//
//    /**
//     * 每次从队列中获取的消息数量
//     */
//    private final int prefetchCount;
//
//    public static ListenerConfig defaultConfig() {
//        return new ListenerConfig(false, 3, 5000L, true, SilkyRabbitMQConstants.SILKY_DEFAULT_DLX_EXCHANGE, SilkyRabbitMQConstants.SILKY_DEFAULT_DLX_ROUTING_KEY, 1, 1);
//    }
//}
