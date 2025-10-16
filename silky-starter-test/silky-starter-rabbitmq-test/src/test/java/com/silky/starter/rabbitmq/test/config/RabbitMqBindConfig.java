package com.silky.starter.rabbitmq.test.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMq 绑定配置
 *
 * @author zy
 * @date 2025-10-15 17:53
 **/
@Configuration
public class RabbitMqBindConfig {

    public static final String EXAMPLE_ORDER_QUEUE = "example.order.queue";

    public final static String EXAMPLE_EXCHANGE = "example-exchange";

    public final static String EXAMPLE_ROUTING_KEY = "example-routingKey";

    public static final String EXAMPLE_ORDER_DELAY_QUEUE = "example.order.delay.queue";

    public final static String EXAMPLE_DELAY_EXCHANGE = "example-delay-exchange";

    public final static String EXAMPLE_DELAY_ROUTING_KEY = "example-delay-routingKey";


    /**
     * 测试订单队列
     *
     * @return Queue
     */
    @Bean("exampleOrderQueue")
    public Queue exampleOrderQueue() {
        return QueueBuilder
                .durable(EXAMPLE_ORDER_QUEUE)
                .build();
    }

    /**
     * 测试订单交换机,根据自己业务选择不同交换机
     *
     * @return TopicExchange
     */
    @Bean("exampleOrderExchange")
    public TopicExchange exampleOrderExchange() {
        return new TopicExchange(EXAMPLE_EXCHANGE, true, false);
    }

    /**
     * 将文件导出发送队列绑定到交换机
     *
     * @param queue    队列
     * @param exchange 交换机
     * @return
     */
    @Bean
    public Binding bindingExchangeMessageFileExport(@Qualifier("exampleOrderQueue") Queue queue, @Qualifier("exampleOrderExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(EXAMPLE_ROUTING_KEY);
    }

    /**
     * 订单延迟插件消息队列所绑定的交换机
     */
    @Bean
    CustomExchange orderPluginDirect() {
        //创建一个自定义交换机，可以发送延迟消息
        Map<String, Object> args = new HashMap<>(3);
        args.put("x-delayed-type", "direct");
        return new CustomExchange(EXAMPLE_DELAY_EXCHANGE, "x-delayed-message", true, false, args);
    }

    /**
     * 订单延迟插件队列
     */
    @Bean
    public Queue orderPluginQueue() {
        return QueueBuilder
                .durable(EXAMPLE_ORDER_DELAY_QUEUE)
                .build();
    }

    /**
     * 将订单延迟插件队列绑定到交换机
     */
    @Bean
    public Binding orderPluginBinding(CustomExchange orderPluginDirect, Queue orderPluginQueue) {
        return BindingBuilder
                .bind(orderPluginQueue)
                .to(orderPluginDirect)
                .with(EXAMPLE_DELAY_ROUTING_KEY)
                .noargs();
    }
}
