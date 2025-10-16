//package com.silky.starter.rabbitmq.test.listener;
//
//import com.alibaba.fastjson2.JSONObject;
//import com.rabbitmq.client.Channel;
//import com.silky.starter.rabbitmq.test.config.RabbitMqBindConfig;
//import com.silky.starter.rabbitmq.test.entity.TradeOrder;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
///**
// * 订单监听器
// *
// * @author zy
// * @date 2025-10-15 16:15
// **/
//@Slf4j
//@Component
//public class ExampleRabbitMqListener1 {
//
//    @RabbitListener(queues = RabbitMqBindConfig.EXAMPLE_ORDER_QUEUE)
//    public void fileExportListener(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
//        log.info("进入订单消息1,msg：" + message + " tag:" + tag);
//
//        TradeOrder tradeOrder = JSONObject.parseObject(message, TradeOrder.class);
//        log.info("订单消息1内容：" + tradeOrder);
//        // 消费确认
//        channel.basicAck(tag, false);
//    }
//
//}
//
//
//
