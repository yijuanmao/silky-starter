//package com.silky.starter.rabbitmq.listener;
//
//import cn.hutool.core.map.MapUtil;
//import cn.hutool.core.util.StrUtil;
//import com.rabbitmq.client.Channel;
//import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
//import com.silky.starter.rabbitmq.exception.RabbitMessageSendException;
//import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
//import com.silky.starter.rabbitmq.properties.SilkyRabbitListenerProperties;
//import com.silky.starter.rabbitmq.properties.SilkyRabbitMQProperties;
//import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.core.MessageProperties;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
//import org.springframework.messaging.handler.annotation.Header;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * RabbitMQ 消息监听器容器（统一管理所有监听器）
// *
// * @author zy
// * @date 2025-10-16 10:40
// **/
//public class RabbitMQListenerContainer {
//
//    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListenerContainer.class);
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
//     * 是否启用重试机制
//     */
//    private final boolean retryEnabled;
//
//    /**
//     * 是否启用死信队列
//     */
//    private final boolean enableDlx;
//
//    private final Map<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();
//
//    /**
//     * 监听器缓存：queueName -> listener
//     */
//    private final Map<String, RabbitMQListener<?>> listenerMap = new ConcurrentHashMap<>();
//
//    private final RabbitMqMessageSerializer messageSerializer;
//
//    private final MessagePersistenceService persistenceService;
//
//    private final RabbitTemplate rabbitTemplate;
//
//    private final RabbitProperties rabbitProperties;
//
//    private final SilkyRabbitListenerProperties skListenerProperties;
//    private final SilkyRabbitMQProperties.PersistenceProperties persistenceProperties;
//
//    public RabbitMQListenerContainer(RabbitMqMessageSerializer messageSerializer, MessagePersistenceService persistenceService,
//                                     RabbitTemplate rabbitTemplate, RabbitProperties rabbitProperties,
//                                     SilkyRabbitListenerProperties skListenerProperties,
//                                     SilkyRabbitMQProperties.PersistenceProperties persistenceProperties) {
//        this.messageSerializer = messageSerializer;
//        this.persistenceService = persistenceService;
//        this.rabbitTemplate = rabbitTemplate;
//        this.rabbitProperties = rabbitProperties;
//        this.skListenerProperties = skListenerProperties;
//        this.persistenceProperties = persistenceProperties;
//
//        this.autoAck = rabbitProperties.getListener().getSimple().getAcknowledgeMode().isAutoAck();
//        this.retryEnabled = rabbitProperties.getListener().getSimple().getRetry().isEnabled();
//        this.maxRetryCount = rabbitProperties.getListener().getSimple().getRetry().getMaxAttempts();
//
//        this.enableDlx = skListenerProperties.isEnableDlx();
//    }
//
//    /**
//     * 注册监听器
//     *
//     * @param listener 消息监听器
//     */
//    public void registerListener(RabbitMQListener<?> listener) {
//        String queueName = listener.getQueueName();
//        listenerMap.put(queueName, listener);
//        logger.info("Registered RabbitMQ listener for queue: {}, message type: {}", queueName, listener.getMessageType().getSimpleName());
//    }
//
//    /**
//     * 获取所有监听器的队列名称（用于 @RabbitListener 的队列配置）
//     */
//    public String[] getListenerQueueNames() {
//        return listenerMap.keySet().toArray(new String[0]);
//    }
//
//    /**
//     * 统一的消息处理方法（支持手动确认）
//     *
//     * @param amqpMessage AMQP消息
//     * @param channel     RabbitMQ通道
//     * @param deliveryTag 消息投递标签
//     * @param queueName   队列名称
//     */
//    @RabbitListener(queues = "#{@rabbitMQListenerContainer.getListenerQueueNames()}")
//    public void handleMessage(Message amqpMessage,
//                              Channel channel,
//                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
//                              @Header(AmqpHeaders.CONSUMER_QUEUE) String queueName) {
//
//        long startTime = System.currentTimeMillis();
//        String messageId = amqpMessage.getMessageProperties().getMessageId();
//        String correlationId = amqpMessage.getMessageProperties().getCorrelationId();
//
//        logger.debug("Starting message processing: queue={}, messageId={}, correlationId={}", queueName, messageId, correlationId);
//
//        RabbitMQListener<?> listener = listenerMap.get(queueName);
//        if (listener == null) {
//            String errorMessage = StrUtil.format("No listener found for queue: {}, messageId: {}", queueName, messageId);
//            logger.warn(errorMessage);
//            // 更新数据库状态：无监听器
//            this.handlePersistenceFailure(messageId, errorMessage, startTime);
//            // 没有监听器，拒绝消息并重新入队
//            basicReject(channel, deliveryTag, true, "No listener found");
//            return;
//        }
//        try {
//            // 反序列化消息
//            Object message = deserializeMessage(amqpMessage.getBody(), listener.getMessageType());
//
//            if (message == null) {
//                logger.warn("Deserialized message is null, queue: {}, messageId: {}", queueName, messageId);
//                // 更新数据库状态：反序列化失败
//                this.handlePersistenceFailure(messageId, "Message deserialization failed", startTime);
//                // 消息反序列化失败，拒绝消息并不重新入队
//                basicReject(channel, deliveryTag, false, "Message deserialization failed");
//                return;
//            }
//
//            logger.debug("Received message from queue: {}, messageId: {}, type: {}, autoAck: {}",
//                    queueName, messageId, listener.getMessageType().getSimpleName(), autoAck);
//
//            // 调用监听器的处理方法
//            invokeListener(listener, message, channel, amqpMessage);
//
//            // 处理消息确认
//            this.handleMessageAcknowledgment(listener, channel, deliveryTag, queueName, messageId, startTime);
//
//        } catch (Exception e) {
//            logger.error("Failed to process message: queue={}, messageId={}", queueName, messageId, e);
//            // 根据监听器配置决定是否重试
//            this.handleConsumeFailure(listener, channel, deliveryTag, amqpMessage, queueName, messageId, e, startTime);
//        }
//    }
//
//    /**
//     * 处理消费失败
//     *
//     * @param listener    监听器
//     * @param channel     消息通道
//     * @param deliveryTag 消息投递标签
//     * @param amqpMessage AMQP消息
//     * @param queueName   队列名称
//     * @param messageId   消息ID
//     * @param e           消费异常
//     * @param startTime   开始时间
//     */
//    private void handleConsumeFailure(RabbitMQListener<?> listener, Channel channel, long deliveryTag,
//                                      Message amqpMessage, String queueName, String messageId,
//                                      Exception e, long startTime) {
//
//        // 如果配置了自动确认，由 Spring 自动处理
//        if (autoAck) {
//            logger.debug("Auto ack mode, letting Spring handle the failure: queue={}, messageId={}", queueName, messageId);
//            // 更新数据库状态：自动确认模式下的失败
//            handlePersistenceFailure(messageId, "Failure in auto ack mode: " + e.getMessage(), startTime);
//            return;
//        }
//        // 手动确认模式下的失败处理
//        try {
//
//            int retryCount = this.getRetryCount(amqpMessage, messageId);
//
//            if (this.retryEnabled && retryCount < maxRetryCount) {
//                int nextRetry = retryCount + 1;
//                // 更新重试计数
//                incrementRetryCount(messageId);
//                logger.info("Message consume failed, requeue for retry {}/{}, queue: {}, messageId: {}",
//                        retryCount, maxRetryCount, queueName, messageId);
//
//                // 更新数据库状态：重试中
//                this.handlePersistenceFailure(messageId, "Retry " + nextRetry + "/" + maxRetryCount + ": " + e.getMessage(),
//                        System.currentTimeMillis());
//
//                basicReject(channel, deliveryTag, true, "Retry " + nextRetry);
//
//            } else {
//                // 重试次数用完，进入死信队列或直接拒绝
//                if (this.enableDlx) {
//                    logger.warn("Message consume failed after {} retries, sending to DLX, queue: {}, messageId: {}",
//                            maxRetryCount, queueName, messageId);
//
//                    // 更新数据库状态：进入死信队列
//                    this.handlePersistenceFailure(messageId, "Max retries exceeded, sent to dead letter exchange", startTime);
//
//                    // 记录死信队列 - 这是核心调用
//                    if (persistenceService != null) {
//                        String errorMsg = String.format("Consume failed after %d retries: %s", maxRetryCount, e.getMessage());
//                        persistenceService.recordMessageSendToDLQ(messageId, queueName, errorMsg);
//                    }
//
//                    // 处理死信队列逻辑
//                    this.handleDeadLetterQueue(listener, amqpMessage, queueName, messageId, e);
//
//                    basicReject(channel, deliveryTag, false, "Max retries exceeded - Sent to DLX");
//
//                } else {
//                    logger.error("Message consume failed after {} retries, discarding, queue: {}, messageId: {}",
//                            maxRetryCount, queueName, messageId);
//
//                    // 更新数据库状态：消费失败（最终）
//                    handlePersistenceFailure(messageId, "Max retries exceeded, message discarded: " + e.getMessage(), startTime);
//
//                    basicReject(channel, deliveryTag, false, "Max retries exceeded - Discarded");
//                }
//            }
//        } catch (Exception ex) {
//            logger.error("Failed to handle consume failure, queue: {}, messageId: {}", queueName, messageId, ex);
//            // 最后的手段：拒绝消息并不重新入队
//            handlePersistenceFailure(messageId, "Error handling failure: " + ex.getMessage(), startTime);
//            basicReject(channel, deliveryTag, false, "Error handling failure: " + ex.getMessage());
//        }
//    }
//
//
//    /**
//     * 处理消息确认（支持手动确认和自动确认）
//     *
//     * @param listener    监听器
//     * @param channel     消息通道
//     * @param deliveryTag 消息投递标签
//     * @param queueName   队列名称
//     * @param startTime   开始时间
//     * @param messageId   消息ID
//     */
//    private void handleMessageAcknowledgment(RabbitMQListener<?> listener, Channel channel,
//                                             long deliveryTag, String queueName, String messageId, long startTime) {
//        // 清除重试计数
//        retryCountMap.remove(messageId);
//        if (autoAck) {
//            // 自动确认模式，Spring会自动处理确认
//            logger.debug("Auto ack mode, Spring will handle acknowledgment: queue={}, messageId={}", queueName, messageId);
//            // 更新数据库状态：自动确认
//            this.handlePersistenceSuccess(messageId, startTime);
//        } else {
//            // 手动确认消息（如果配置了手动确认）
//            try {
//                basicAck(channel, deliveryTag);
//                logger.debug("Message manually acknowledged: queue={}, messageId={}, deliveryTag={}, costTime={}ms",
//                        queueName, messageId, deliveryTag, startTime);
//
//                // 更新数据库状态：手动确认完成
//                this.handlePersistenceSuccess(messageId, startTime);
//            } catch (Exception e) {
//                logger.error("Failed to manually acknowledge message: queue={}, messageId={}", queueName, messageId, e);
//                // 更新数据库状态：确认失败
//                this.handlePersistenceFailure(messageId, "Manual acknowledgment failed: " + e.getMessage(), startTime);
//                throw new RabbitMessageSendException("Message manual acknowledgment failed", e);
//            }
//
//        }
//    }
//
//    /**
//     * 调用监听器处理方法
//     */
//    @SuppressWarnings("unchecked")
//    private <T extends BaseMassageSend> void invokeListener(RabbitMQListener<T> listener, Object message,
//                                                            Channel channel, Message amqpMessage) {
//        listener.onMessage((T) message, channel, amqpMessage);
//    }
//
//
//    /**
//     * 反序列化消息
//     */
//    @SuppressWarnings("unchecked")
//    private <T> T deserializeMessage(byte[] body, Class<?> messageType) {
//        if (body == null || body.length == 0) {
//            return null;
//        }
//        return (T) messageSerializer.deserialize(body, messageType);
//    }
//
//    /**
//     * 获取重试次数
//     *
//     * @param amqpMessage AMQP消息
//     * @param messageId   消息ID
//     */
//    private int getRetryCount(Message amqpMessage, String messageId) {
//        Map<String, Object> headers = amqpMessage.getMessageProperties().getHeaders();
//        int retryCountInt = 0;
//        if (MapUtil.isNotEmpty(headers) && headers.containsKey("x-retry-count")) {
//            Object retryCount = headers.get("x-retry-count");
//            if (retryCount instanceof Integer) {
//                retryCountInt = (Integer) retryCount;
//            }
//        }
//        return retryCountMap.getOrDefault(messageId, new AtomicInteger(retryCountInt)).get();
//    }
//
//    /**
//     * 增加重试次数
//     */
//    private void incrementRetryCount(String messageId) {
//        retryCountMap.computeIfAbsent(messageId, k -> new AtomicInteger(0)).incrementAndGet();
//    }
//
//    /**
//     * 安全的消息确认方法
//     */
//    private void basicAck(Channel channel, long deliveryTag) {
//        try {
//            channel.basicAck(deliveryTag, false);
//        } catch (IOException e) {
//            logger.error("Silky RabbitMQ Failed to acknowledge message, deliveryTag: {}", deliveryTag, e);
//            throw new RabbitMessageSendException("Silky RabbitMQ Message acknowledgement failed", e);
//        }
//    }
//
//    /**
//     * 安全的消息拒绝方法
//     */
//    private void basicReject(Channel channel, long deliveryTag, boolean requeue, String reason) {
//        try {
//            channel.basicReject(deliveryTag, requeue);
//            logger.debug("Message rejected: deliveryTag={}, requeue={}, reason={}",
//                    deliveryTag, requeue, reason);
//        } catch (IOException e) {
//            logger.error("Failed to reject message, deliveryTag: {}, requeue: {}", deliveryTag, requeue, e);
//            throw new RabbitMessageSendException("Silky RabbitMQ Message rejection failed", e);
//        }
//    }
//
//    /**
//     * 安全的消息否定确认方法（用于多个消息）
//     */
//    private void basicNack(Channel channel, long deliveryTag, boolean multiple, boolean requeue) {
//        try {
//            channel.basicNack(deliveryTag, multiple, requeue);
//        } catch (IOException e) {
//            logger.error("Silky RabbitMQ Failed to nack message, deliveryTag: {}, multiple: {}, requeue: {}",
//                    deliveryTag, multiple, requeue, e);
//            throw new RabbitMessageSendException("Silky RabbitMQ Message nack failed", e);
//        }
//    }
//
//    /**
//     * 处理死信队列逻辑 - 实际发送消息到死信交换机
//     */
//    private void handleDeadLetterQueue(RabbitMQListener<?> listener, Message amqpMessage,
//                                       String queueName, String messageId, Exception originalException) {
//        try {
//            // 获取死信队列配置
//            String dlxExchange = skListenerProperties.getDlxExchange();
//            String dlxRoutingKey = skListenerProperties.getDlxRoutingKey();
//
//            if (StrUtil.isBlank(dlxExchange) || StrUtil.isBlank(dlxRoutingKey)) {
//                logger.warn("DLX enabled but no DLX exchange or routing key configured for queue: {}", queueName);
//                return;
//            }
//
//            // 准备死信消息
//            Message dlqMessage = prepareDeadLetterMessage(amqpMessage, queueName, originalException);
//
//            // 实际发送到死信交换机
//            rabbitTemplate.convertAndSend(dlxExchange, dlxRoutingKey, dlqMessage.getBody(), message -> {
//                // 设置消息属性
//                MessageProperties properties = message.getMessageProperties();
//
//                // 复制原始消息的属性
//                MessageProperties originalProperties = amqpMessage.getMessageProperties();
//                if (originalProperties != null) {
//                    properties.setContentType(originalProperties.getContentType());
//                    properties.setContentEncoding(originalProperties.getContentEncoding());
//                    properties.setPriority(originalProperties.getPriority());
//                    properties.setDeliveryMode(originalProperties.getDeliveryMode());
//                }
//
//                // 设置死信消息的特殊头信息
//                Map<String, Object> headers = new HashMap<>();
//                if (MapUtil.isNotEmpty(properties.getHeaders())) {
//                    headers.putAll(properties.getHeaders());
//                }
//                // 添加死信相关的头信息
//                headers.put("x-death", createDeathHeader(queueName, dlxExchange));
//                headers.put("x-original-queue", queueName);
//                headers.put("x-original-exchange", amqpMessage.getMessageProperties().getReceivedExchange());
//                headers.put("x-original-routing-key", amqpMessage.getMessageProperties().getReceivedRoutingKey());
//                headers.put("x-failure-reason", originalException.getMessage());
//                headers.put("x-failure-timestamp", new Date());
//                headers.put("x-retry-count", maxRetryCount);
//                headers.put("x-dead-letter-reason", "max_retries_exceeded");
//                if (skListenerProperties.getDlxMessageTtl() > 0) {
//                    headers.put("expiration", String.valueOf(skListenerProperties.getDlxMessageTtl()));
//                }
//
//                properties.setHeaders(headers);
//                properties.setMessageId(messageId + "_DLQ");
//                properties.setTimestamp(new Date());
//
//                return message;
//            });
//
//            logger.info("Successfully sent message to DLX: messageId={}, queue={}, dlxExchange={}, dlxRoutingKey={}",
//                    messageId, queueName, dlxExchange, dlxRoutingKey);
//
//            // 记录死信发送成功
//            if (persistenceService != null) {
//                persistenceService.recordDLQSendSuccess(messageId, queueName, dlxExchange, dlxRoutingKey);
//            }
//
//        } catch (Exception e) {
//            logger.error("Failed to send message to DLX: messageId={}, queue={}", messageId, queueName, e);
//
//            // 记录死信发送失败
//            if (persistenceService != null) {
//                persistenceService.recordDLQSendFailure(messageId, queueName, e.getMessage());
//            }
//            throw new RabbitMessageSendException("Failed to send message to dead letter exchange", e);
//        }
//    }
//
//    /**
//     * 准备死信消息
//     */
//    private Message prepareDeadLetterMessage(Message originalMessage, String queueName, Exception exception) {
//        try {
//            // 创建死信消息（使用原始消息体）
//            // 可以在这里修改消息体，比如添加错误信息等
//            // 如果需要修改消息体，可以在这里进行序列化/反序列化操作
//            return new Message(originalMessage.getBody(), originalMessage.getMessageProperties());
//
//        } catch (Exception e) {
//            logger.error("Failed to prepare dead letter message for queue: {}", queueName, e);
//            // 如果准备失败，返回原始消息
//            return originalMessage;
//        }
//    }
//
//    /**
//     * 创建 x-death 头信息（RabbitMQ 标准的死信头格式）
//     */
//    private List<Map<String, Object>> createDeathHeader(String queueName, String dlxExchange) {
//        Map<String, Object> death = new HashMap<>(8);
//        death.put("count", 1L);
//        death.put("reason", "rejected");
//        death.put("queue", queueName);
//        death.put("time", new Date());
//        death.put("exchange", dlxExchange);
//        death.put("routing-keys", Collections.singletonList(queueName));
//        return Collections.singletonList(death);
//    }
//
//    /**
//     * 处理成功结果
//     *
//     * @param messageId 消息ID
//     * @param startTime 开始时间
//     */
//    private void handlePersistenceSuccess(String messageId, long startTime) {
//
//        long costTime = System.currentTimeMillis() - startTime;
//        try {
//            //更新消息状态
//            if (this.isPersistence()) {
//                persistenceService.consumeSuccess(messageId, costTime);
//            }
//        } catch (Exception e) {
//            logger.error("Failed to record consumer success for messageId={}, costTime={}", messageId, costTime, e);
//        }
//    }
//
//    /**
//     * 处理失败结果
//     */
//    private void handlePersistenceFailure(String messageId, String exception, long startTime) {
//        long costTime = System.currentTimeMillis() - startTime;
//        try {
//            //更新消息状态
//            if (this.isPersistence()) {
//                persistenceService.consumeFailure(messageId, exception, costTime);
//            }
//        } catch (Exception e) {
//            logger.error("Failed to record consumer failure for messageId={}, costTime={}", messageId, costTime, e);
//        }
//    }
//
//    /**
//     * 是否启用持久化
//     *
//     * @return boolean
//     */
//    private boolean isPersistence() {
//        return persistenceService != null && persistenceProperties.isEnabled();
//    }
//
//}
