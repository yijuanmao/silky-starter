//package com.silky.starter.rabbitmq.config;
//
//import com.silky.starter.rabbitmq.core.constant.SilkyRabbitMQConstants;
//import org.springframework.amqp.core.*;
//import org.springframework.boot.autoconfigure.AutoConfiguration;
//import org.springframework.context.annotation.Bean;
//
///**
// * RabbitMQ队列配置，后期留作扩展
// *
// * @author zy
// * @date 2025-10-16 11:44
// **/
//@AutoConfiguration
//public class SilkyRabbitMQQueueConfig {
//
//    /**
//     * 创建死信队列
//     *
//     * @return Queue
//     */
//    @Bean
//    public Queue orderDlxQueue() {
//        return QueueBuilder.durable(SilkyRabbitMQConstants.SILKY_DEFAULT_DLX_QUEUE).build();
//    }
//
//    /**
//     * 创建死信交换机
//     *
//     * @return DirectExchange
//     */
//    @Bean
//    public DirectExchange orderDlxExchange() {
//        return new DirectExchange(SilkyRabbitMQConstants.SILKY_DEFAULT_DLX_EXCHANGE);
//    }
//
//    /**
//     * 绑定死信队列和死信交换机
//     *
//     * @return Binding
//     */
//    @Bean
//    public Binding orderDlxBinding() {
//        return BindingBuilder.bind(orderDlxQueue())
//                .to(orderDlxExchange())
//                .with(SilkyRabbitMQConstants.SILKY_DEFAULT_DLX_ROUTING_KEY);
//    }
//
//}
