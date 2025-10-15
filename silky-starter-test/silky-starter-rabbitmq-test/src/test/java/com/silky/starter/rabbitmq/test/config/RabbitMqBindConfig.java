package com.silky.starter.rabbitmq.test.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
     * 测试订单交换机
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
}
