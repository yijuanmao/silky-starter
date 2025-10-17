package com.silky.starter.rabbitmq.listener;

import cn.hutool.core.map.MapUtil;
import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import com.silky.starter.rabbitmq.enums.ConsumeStatus;
import com.silky.starter.rabbitmq.exception.RabbitMessageSendException;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ 消息监听器容器（统一管理所有监听器）
 *
 * @author zy
 * @date 2025-10-16 10:40
 **/
public class RabbitMQListenerContainer {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListenerContainer.class);

    /**
     * 监听器缓存：queueName -> listener
     */
    private final Map<String, RabbitMQListener<?>> listenerMap = new ConcurrentHashMap<>();

    private final RabbitMqMessageSerializer messageSerializer;

    private final MessagePersistenceService persistenceService;

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQListenerContainer(RabbitMqMessageSerializer messageSerializer, MessagePersistenceService persistenceService, RabbitTemplate rabbitTemplate) {
        this.messageSerializer = messageSerializer;
        this.persistenceService = persistenceService;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 注册监听器
     *
     * @param listener 消息监听器
     */
    public void registerListener(RabbitMQListener<?> listener) {
        String queueName = listener.getQueueName();
        listenerMap.put(queueName, listener);
        logger.info("Registered RabbitMQ listener for queue: {}, message type: {}", queueName, listener.getMessageType().getSimpleName());
    }

    /**
     * 获取所有监听器的队列名称（用于 @RabbitListener 的队列配置）
     */
    public String[] getListenerQueueNames() {
        return listenerMap.keySet().toArray(new String[0]);
    }

    /**
     * 统一的消息处理方法（支持手动确认）
     *
     * @param amqpMessage AMQP消息
     * @param channel     RabbitMQ通道
     * @param deliveryTag 消息投递标签
     * @param queueName   队列名称
     */
    @RabbitListener(queues = "#{@rabbitMQListenerContainer.getListenerQueueNames()}")
    public void handleMessage(Message amqpMessage,
                              Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                              @Header(AmqpHeaders.CONSUMER_QUEUE) String queueName) {

        long startTime = System.currentTimeMillis();
        String messageId = amqpMessage.getMessageProperties().getMessageId();
        String correlationId = amqpMessage.getMessageProperties().getCorrelationId();

        logger.debug("Starting message processing: queue={}, messageId={}, correlationId={}", queueName, messageId, correlationId);

        RabbitMQListener<?> listener = listenerMap.get(queueName);
        if (listener == null) {
            logger.warn("No listener found for queue: {}, messageId: {}", queueName, messageId);

            // 更新数据库状态：无监听器
            this.updateMessageStatus(messageId, ConsumeStatus.CONSUME_FAILED, "No listener registered for queue: " + queueName, null);
            // 没有监听器，拒绝消息并重新入队
            basicReject(channel, deliveryTag, true, "No listener found");
            return;
        }
        try {
            // 反序列化消息
            Object message = deserializeMessage(amqpMessage.getBody(), listener.getMessageType());

            if (message == null) {
                logger.warn("Deserialized message is null, queue: {}, messageId: {}", queueName, messageId);
                // 更新数据库状态：反序列化失败
                this.updateMessageStatus(messageId, ConsumeStatus.CONSUME_FAILED, "Message deserialization failed", startTime);
                // 消息反序列化失败，拒绝消息并不重新入队
                basicReject(channel, deliveryTag, false, "Message deserialization failed");
                return;
            }

            logger.debug("Received message from queue: {}, messageId: {}, type: {}, autoAck: {}",
                    queueName, messageId, listener.getMessageType().getSimpleName(), listener.getConfig().isAutoAck());

            // 调用监听器的处理方法
            invokeListener(listener, message, channel, amqpMessage);

            long costTime = System.currentTimeMillis() - startTime;

            // 处理消息确认
            this.handleMessageAcknowledgment(listener, channel, deliveryTag, queueName, messageId, costTime);

            logger.info("Message processed successfully: queue={}, messageId={}, costTime={}ms",
                    queueName, messageId, costTime);

        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            logger.error("Failed to process message: queue={}, messageId={}", queueName, messageId, e);

            // 更新数据库状态：处理失败
            this.updateMessageStatus(messageId, ConsumeStatus.CONSUME_FAILED, "Message consumption failed: " + e.getMessage(), costTime);

            // 根据监听器配置决定是否重试
            this.handleConsumeFailure(listener, channel, deliveryTag, amqpMessage, queueName, messageId, e, costTime);
        }
    }

    /**
     * 处理消息确认（支持手动确认和自动确认）
     *
     * @param listener    监听器
     * @param channel     消息通道
     * @param deliveryTag 消息投递标签
     * @param queueName   队列名称
     * @param costTime    消费耗时
     * @param messageId   消息ID
     */
    private void handleMessageAcknowledgment(RabbitMQListener<?> listener, Channel channel,
                                             long deliveryTag, String queueName, String messageId, long costTime) {
        if (listener.getConfig().isAutoAck()) {
            // 自动确认模式，Spring会自动处理确认
            logger.debug("Auto ack mode, Spring will handle acknowledgment: queue={}, messageId={}", queueName, messageId);
            // 更新数据库状态：自动确认
            this.updateMessageStatus(messageId, ConsumeStatus.CONSUMED,
                    "Message auto acknowledged after " + costTime + "ms", costTime);
        } else {
            // 手动确认消息（如果配置了手动确认）
            try {
                basicAck(channel, deliveryTag);
                logger.debug("Message manually acknowledged: queue={}, messageId={}, deliveryTag={}, costTime={}ms",
                        queueName, messageId, deliveryTag, costTime);

                // 更新数据库状态：手动确认完成
                this.updateMessageStatus(messageId, ConsumeStatus.CONSUMED,
                        "Message manually acknowledged after " + costTime + "ms", System.currentTimeMillis());
            } catch (Exception e) {
                logger.error("Failed to manually acknowledge message: queue={}, messageId={}", queueName, messageId, e);
                // 更新数据库状态：确认失败
                this.updateMessageStatus(messageId, ConsumeStatus.CONSUME_FAILED,
                        "Manual acknowledgment failed: " + e.getMessage(), costTime);
                throw new RabbitMessageSendException("Message manual acknowledgment failed", e);
            }

        }
    }

    /**
     * 调用监听器处理方法
     */
    @SuppressWarnings("unchecked")
    private <T extends BaseMassageSend> void invokeListener(RabbitMQListener<T> listener, Object message,
                                                            Channel channel, Message amqpMessage) {
        listener.onMessage((T) message, channel, amqpMessage);
    }


    /**
     * 反序列化消息
     */
    @SuppressWarnings("unchecked")
    private <T> T deserializeMessage(byte[] body, Class<?> messageType) {
        if (body == null || body.length == 0) {
            return null;
        }
        return (T) messageSerializer.deserialize(body, messageType);
    }

    /**
     * 处理消费失败
     *
     * @param listener    监听器
     * @param channel     消息通道
     * @param deliveryTag 消息投递标签
     * @param amqpMessage AMQP消息
     * @param queueName   队列名称
     * @param messageId   消息ID
     * @param e           消费异常
     * @param costTime    消费耗时
     */
    private void handleConsumeFailure(RabbitMQListener<?> listener, Channel channel, long deliveryTag,
                                      Message amqpMessage, String queueName, String messageId,
                                      Exception e, long costTime) {

        // 如果配置了自动确认，由 Spring 自动处理
        if (listener.getConfig().isAutoAck()) {
            logger.debug("Auto ack mode, letting Spring handle the failure: queue={}, messageId={}", queueName, messageId);
            // 更新数据库状态：自动确认模式下的失败
            updateMessageStatus(messageId, ConsumeStatus.CONSUME_FAILED, "Failure in auto ack mode: " + e.getMessage(), costTime);
            return;
        }

        // 手动确认模式下的失败处理
        try {
            int retryCount = getRetryCount(amqpMessage);
            int maxRetries = listener.getConfig().getMaxRetryCount();

            if (retryCount < maxRetries) {
                // 重新投递消息进行重试
                int nextRetry = retryCount + 1;
                logger.info("Message consume failed, requeue for retry {}/{}, queue: {}, messageId: {}",
                        nextRetry, maxRetries, queueName, messageId);

                // 更新数据库状态：重试中
                updateMessageStatus(messageId, ConsumeStatus.CONSUME_FAILED, "Retry " + nextRetry + "/" + maxRetries + ": " + e.getMessage(),
                        System.currentTimeMillis());

                basicReject(channel, deliveryTag, true, "Retry " + nextRetry);

            } else {
                // 重试次数用完，进入死信队列或直接拒绝
                if (listener.getConfig().isEnableDlx()) {
                    logger.warn("Message consume failed after {} retries, sending to DLX, queue: {}, messageId: {}",
                            maxRetries, queueName, messageId);

                    // 更新数据库状态：进入死信队列
                    this.updateMessageStatus(messageId, ConsumeStatus.CONSUME_FAILED, "Max retries exceeded, sent to dead letter exchange", costTime);

                    // 记录死信队列 - 这是核心调用
                    if (persistenceService != null) {
                        String errorMsg = String.format("Consume failed after %d retries: %s",
                                maxRetries, e.getMessage());
                        persistenceService.recordMessageSendToDLQ(messageId, queueName, errorMsg);
                    }

                    // 处理死信队列逻辑
                    this.handleDeadLetterQueue(listener, amqpMessage, queueName, messageId, e);

                    basicReject(channel, deliveryTag, false, "Max retries exceeded - Sent to DLX");

                } else {
                    logger.error("Message consume failed after {} retries, discarding, queue: {}, messageId: {}",
                            maxRetries, queueName, messageId);

                    // 更新数据库状态：消费失败（最终）
                    updateMessageStatus(messageId, ConsumeStatus.CONSUME_FAILED,
                            "Max retries exceeded, message discarded: " + e.getMessage(),
                            System.currentTimeMillis());

                    basicReject(channel, deliveryTag, false, "Max retries exceeded - Discarded");
                }
            }
        } catch (Exception ex) {
            logger.error("Failed to handle consume failure, queue: {}, messageId: {}", queueName, messageId, ex);
            // 最后的手段：拒绝消息并不重新入队
            updateMessageStatus(messageId, ConsumeStatus.CONSUME_FAILED,
                    "Error handling failure: " + ex.getMessage(), System.currentTimeMillis());
            basicReject(channel, deliveryTag, false, "Error handling failure: " + ex.getMessage());
        }
    }

    /**
     * 更新消息状态到数据库
     *
     * @param messageId 消息ID
     * @param status    状态
     * @param remark    备注
     * @param costTime  消费耗时
     */
    private void updateMessageStatus(String messageId, ConsumeStatus status, String remark, Long costTime) {
        try {
            if (persistenceService != null) {
                if (ConsumeStatus.CONSUMED.equals(status)) {
                    persistenceService.recordMessageConsume(messageId, costTime);
                } else if (ConsumeStatus.CONSUME_FAILED.equals(status)) {
                    persistenceService.recordMessageConsumeFailure(messageId, remark, costTime);
                } else {
                    logger.warn("Unknown consume status for messageId={}, status={}", messageId, status.name());
                }
            }
        } catch (Exception e) {
            //暂时不抛出异常，避免影响主要业务流程
            logger.error("Failed to update message status in database: messageId={}, status={}", messageId, status.name(), e);
        }
    }

    /**
     * 获取重试次数
     */
    private int getRetryCount(Message amqpMessage) {
        Map<String, Object> headers = amqpMessage.getMessageProperties().getHeaders();
        if (headers != null && headers.containsKey("x-retry-count")) {
            Object retryCount = headers.get("x-retry-count");
            if (retryCount instanceof Integer) {
                return (Integer) retryCount;
            }
        }
        return 0;
    }

    /**
     * 安全的消息确认方法
     */
    private void basicAck(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            logger.error("Silky RabbitMQ Failed to acknowledge message, deliveryTag: {}", deliveryTag, e);
            throw new RabbitMessageSendException("Silky RabbitMQ Message acknowledgement failed", e);
        }
    }

    /**
     * 安全的消息拒绝方法
     */
    private void basicReject(Channel channel, long deliveryTag, boolean requeue, String reason) {
        try {
            channel.basicReject(deliveryTag, requeue);
            logger.debug("Message rejected: deliveryTag={}, requeue={}, reason={}",
                    deliveryTag, requeue, reason);
        } catch (IOException e) {
            logger.error("Failed to reject message, deliveryTag: {}, requeue: {}", deliveryTag, requeue, e);
            throw new RabbitMessageSendException("Silky RabbitMQ Message rejection failed", e);
        }
    }

    /**
     * 安全的消息否定确认方法（用于多个消息）
     */
    private void basicNack(Channel channel, long deliveryTag, boolean multiple, boolean requeue) {
        try {
            channel.basicNack(deliveryTag, multiple, requeue);
        } catch (IOException e) {
            logger.error("Silky RabbitMQ Failed to nack message, deliveryTag: {}, multiple: {}, requeue: {}",
                    deliveryTag, multiple, requeue, e);
            throw new RabbitMessageSendException("Silky RabbitMQ Message nack failed", e);
        }
    }

    /**
     * 处理死信队列逻辑 - 实际发送消息到死信交换机
     */
    private void handleDeadLetterQueue(RabbitMQListener<?> listener, Message amqpMessage,
                                       String queueName, String messageId, Exception originalException) {
        try {
            // 获取死信队列配置
            String dlxExchange = listener.getConfig().getDlxExchange();
            String dlxRoutingKey = listener.getConfig().getDlxRoutingKey();

            if (dlxExchange == null || dlxRoutingKey == null) {
                logger.warn("DLX enabled but no DLX exchange or routing key configured for queue: {}", queueName);
                return;
            }

            // 准备死信消息
            Message dlqMessage = prepareDeadLetterMessage(amqpMessage, queueName, originalException);

            // 实际发送到死信交换机
            rabbitTemplate.convertAndSend(dlxExchange, dlxRoutingKey, dlqMessage.getBody(), message -> {
                // 设置消息属性
                MessageProperties properties = message.getMessageProperties();

                // 复制原始消息的属性
                MessageProperties originalProperties = amqpMessage.getMessageProperties();
                if (originalProperties != null) {
                    properties.setContentType(originalProperties.getContentType());
                    properties.setContentEncoding(originalProperties.getContentEncoding());
                    properties.setPriority(originalProperties.getPriority());
                    properties.setDeliveryMode(originalProperties.getDeliveryMode());
                }

                // 设置死信消息的特殊头信息
                Map<String, Object> headers = new HashMap<>();
                if (MapUtil.isNotEmpty(properties.getHeaders())) {
                    headers.putAll(properties.getHeaders());
                }
                // 添加死信相关的头信息
                headers.put("x-death", createDeathHeader(queueName, dlxExchange));
                headers.put("x-original-queue", queueName);
                headers.put("x-original-exchange", amqpMessage.getMessageProperties().getReceivedExchange());
                headers.put("x-original-routing-key", amqpMessage.getMessageProperties().getReceivedRoutingKey());
                headers.put("x-failure-reason", originalException.getMessage());
                headers.put("x-failure-timestamp", new Date());
                headers.put("x-retry-count", listener.getConfig().getMaxRetryCount());
                headers.put("x-dead-letter-reason", "max_retries_exceeded");

                properties.setHeaders(headers);
                properties.setMessageId(messageId + "_DLQ");
                properties.setTimestamp(new Date());

                return message;
            });

            logger.info("Successfully sent message to DLX: messageId={}, queue={}, dlxExchange={}, dlxRoutingKey={}",
                    messageId, queueName, dlxExchange, dlxRoutingKey);

            // 记录死信发送成功
            if (persistenceService != null) {
                persistenceService.recordDLQSendSuccess(messageId, queueName, dlxExchange, dlxRoutingKey);
            }

        } catch (Exception e) {
            logger.error("Failed to send message to DLX: messageId={}, queue={}", messageId, queueName, e);

            // 记录死信发送失败
            if (persistenceService != null) {
                persistenceService.recordDLQSendFailure(messageId, queueName, e.getMessage());
            }
            throw new RabbitMessageSendException("Failed to send message to dead letter exchange", e);
        }
    }

    /**
     * 准备死信消息
     */
    private Message prepareDeadLetterMessage(Message originalMessage, String queueName, Exception exception) {
        try {
            // 创建死信消息（使用原始消息体）
            // 可以在这里修改消息体，比如添加错误信息等
            // 如果需要修改消息体，可以在这里进行序列化/反序列化操作
            return new Message(originalMessage.getBody(), originalMessage.getMessageProperties());

        } catch (Exception e) {
            logger.error("Failed to prepare dead letter message for queue: {}", queueName, e);
            // 如果准备失败，返回原始消息
            return originalMessage;
        }
    }

    /**
     * 创建 x-death 头信息（RabbitMQ 标准的死信头格式）
     */
    private List<Map<String, Object>> createDeathHeader(String queueName, String dlxExchange) {
        Map<String, Object> death = new HashMap<>(8);
        death.put("count", 1L);
        death.put("reason", "rejected");
        death.put("queue", queueName);
        death.put("time", new Date());
        death.put("exchange", dlxExchange);
        death.put("routing-keys", Collections.singletonList(queueName));
        return Collections.singletonList(death);
    }

}
