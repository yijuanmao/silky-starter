package com.silky.starter.rabbitmq.listener.util;

import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.exception.RabbitMessageSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;

import java.io.IOException;

/**
 * 手动确认工具类
 *
 * @author zy
 * @date 2025-10-16 11:26
 **/
public class ManualAckHelper {

    private static final Logger logger = LoggerFactory.getLogger(ManualAckHelper.class);

    /**
     * 确认消息处理成功
     */
    public static void ackSuccess(Channel channel, Message amqpMessage, String business) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String messageId = amqpMessage.getMessageProperties().getMessageId();

        try {
            channel.basicAck(deliveryTag, false);
            logger.debug("Message acknowledged successfully: messageId={}, deliveryTag={}, business={}",
                    messageId, deliveryTag, business);
        } catch (IOException e) {
            logger.error("Failed to acknowledge message: messageId={}, deliveryTag={}, business={}",
                    messageId, deliveryTag, business, e);
            throw new RabbitMessageSendException("Message acknowledgement failed", e);
        }
    }

    /**
     * 拒绝消息并重新入队（用于重试）
     */
    public static void rejectAndRequeue(Channel channel, Message amqpMessage, String reason) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String messageId = amqpMessage.getMessageProperties().getMessageId();

        try {
            channel.basicReject(deliveryTag, true);
            logger.warn("Message rejected and requeued: messageId={}, deliveryTag={}, reason={}",
                    messageId, deliveryTag, reason);
        } catch (IOException e) {
            logger.error("Failed to reject and requeue message: messageId={}, deliveryTag={}",
                    messageId, deliveryTag, e);
            throw new RabbitMessageSendException("Message rejection failed", e);
        }
    }

    /**
     * 拒绝消息并不重新入队（用于死信队列）
     */
    public static void rejectToDlx(Channel channel, Message amqpMessage, String reason) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        String messageId = amqpMessage.getMessageProperties().getMessageId();

        try {
            channel.basicReject(deliveryTag, false);
            logger.warn("Message rejected to DLX: messageId={}, deliveryTag={}, reason={}",
                    messageId, deliveryTag, reason);
        } catch (IOException e) {
            logger.error("Failed to reject message to DLX: messageId={}, deliveryTag={}",
                    messageId, deliveryTag, e);
            throw new RabbitMessageSendException("Message rejection to DLX failed", e);
        }
    }

    /**
     * 批量确认消息
     */
    public static void batchAck(Channel channel, long deliveryTag, boolean multiple) {
        try {
            channel.basicAck(deliveryTag, multiple);
            logger.debug("Batch messages acknowledged: deliveryTag={}, multiple={}", deliveryTag, multiple);
        } catch (IOException e) {
            logger.error("Failed to batch acknowledge messages: deliveryTag={}, multiple={}",
                    deliveryTag, multiple, e);
            throw new RabbitMessageSendException("Batch message acknowledgement failed", e);
        }
    }
}
