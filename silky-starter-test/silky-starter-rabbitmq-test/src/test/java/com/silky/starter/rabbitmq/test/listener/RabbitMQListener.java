package com.silky.starter.rabbitmq.test.listener;

import com.alibaba.fastjson2.JSONObject;
import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.constant.SkAmqpHeaders;
import com.silky.starter.rabbitmq.test.config.RabbitMqBindConfig;
import com.silky.starter.rabbitmq.test.entity.TradeOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 单条消息监听器,根据业务自己选择
 *
 * @author zy
 * @date 2025-10-16 11:41
 **/
@Slf4j
@Component
public class RabbitMQListener {

    /**
     * 处理消息,监听接受消息处理方式一，Message message
     *
     * @param message 消息对象
     * @param channel RabbitMQ通道
     */
    @RabbitListener(queues = RabbitMqBindConfig.EXAMPLE_ORDER_QUEUE)
    public void onMessage(Message message,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        String msg = new String(message.getBody());
        log.info("Processing order message: {}", msg);
        // 支付处理逻辑
        try {
            TradeOrder tradeOrder = JSONObject.parseObject(msg, TradeOrder.class);
            log.info("order processed successfully: {}", tradeOrder.getOrderId());
            // 手动确认消费
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Failed to process payment: {}", msg, e);
            throw new RuntimeException("order processing failed", e);
        }
    }

    /**
     * 处理消息,监听接受消息处理方式二，TradeOrder message
     *
     * @param message      订单对象
     * @param businessType 业务类型，发送消息时，此参数不给，就需要添加此参数
     * @param description  业务类型描述，发送消息时,添加此参数，则此参数会自动添加到消息头中
     * @param extData      业务扩展数据，发送消息时,添加此参数，则此参数会自动添加到消息头中
     * @param source       消息来源，发送消息时,添加此参数，则此参数会自动添加到消息头中
     * @param channel      RabbitMQ通道
     */
    @RabbitListener(queues = RabbitMqBindConfig.EXAMPLE_ORDER_QUEUE)
    public void exampleOrderQueueTest(TradeOrder message,
                                      Channel channel,
                                      @Header(SkAmqpHeaders.DELIVERY_TAG) long tag,
                                      @Header(value = SkAmqpHeaders.BUSINESS_TYPE, required = false) String businessType,
                                      @Header(value = SkAmqpHeaders.DESCRIPTION, required = false) String description,
                                      @Header(value = SkAmqpHeaders.SOURCE, required = false) String source,
                                      @Header(value = SkAmqpHeaders.EXT_DATA, required = false) Map<String, Object> extData) throws IOException {
        log.info("进入demo消息队列，消息内容为:{}, 业务类型:{}, 业务描述:{} , 消息来源:{}, 业务扩展数据为:{}", message, businessType, description, source, extData);
        channel.basicAck(tag, false);
    }

    /**
     * 处理消息,监听接受消息处理方式三，String message
     *
     * @param message      订单对象
     * @param businessType 业务类型，发送消息时，此参数不给，就需要添加此参数
     * @param description  业务类型描述，发送消息时,添加此参数，则此参数会自动添加到消息头中
     * @param extData      业务扩展数据，发送消息时,添加此参数，则此参数会自动添加到消息头中
     * @param source       消息来源，发送消息时,添加此参数，则此参数会自动添加到消息头中
     * @param channel      RabbitMQ通道
     */
    @RabbitListener(queues = RabbitMqBindConfig.EXAMPLE_ORDER_QUEUE)
    public void exampleOrderQueueTest(String message, Channel channel,
                                      @Header(SkAmqpHeaders.DELIVERY_TAG) long tag,
                                      @Header(value = SkAmqpHeaders.BUSINESS_TYPE, required = false) String businessType,
                                      @Header(value = SkAmqpHeaders.DESCRIPTION, required = false) String description,
                                      @Header(value = SkAmqpHeaders.SOURCE, required = false) String source,
                                      @Header(value = SkAmqpHeaders.EXT_DATA, required = false) Map<String, Object> extData) throws IOException {
        log.info("进入demo消息队列，消息内容为:{}, 业务类型:{}, 业务描述:{} , 消息来源:{}, 业务扩展数据为:{}", message, businessType, description, source, extData);
        channel.basicAck(tag, false);
    }

}
