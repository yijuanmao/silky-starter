package com.silky.starter.rabbitmq.listener;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.Channel;
import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import com.silky.starter.rabbitmq.exception.RabbitMessageSendException;
import com.silky.starter.rabbitmq.listener.registry.ListenerRegistry;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.properties.SilkyRabbitListenerProperties;
import com.silky.starter.rabbitmq.properties.SilkyRabbitMQProperties;
import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.core.Ordered;
import org.springframework.messaging.handler.annotation.Header;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RabbitMQ 消息监听器容器（统一管理所有监听器
 *
 * @author zy
 * @date 2025-10-18 14:06
 **/
public class RabbitMQListenerContainer implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListenerContainer.class);

    /**
     * 是否自动确认
     */
    private final boolean autoAck;

    /**
     * 最大重试次数
     */
    private final int maxRetryCount;

    /**
     * 是否启用重试机制
     */
    private final boolean retryEnabled;

    /**
     * 是否启用死信队列
     */
    private final boolean enableDlx;

    private final RabbitMqMessageSerializer messageSerializer;

    private final MessagePersistenceService persistenceService;

    private final RabbitTemplate rabbitTemplate;

    private final SilkyRabbitListenerProperties listenerProperties;

    private final SilkyRabbitMQProperties.PersistenceProperties persistenceProperties;

    private final ListenerRegistry listenerRegistry;

    /**
     * 重试计数缓存：messageId -> retryCount
     */
    private final Map<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();


    public RabbitMQListenerContainer(RabbitMqMessageSerializer messageSerializer, MessagePersistenceService persistenceService,
                                     RabbitTemplate rabbitTemplate, RabbitProperties rabbitProperties,
                                     SilkyRabbitListenerProperties listenerProperties,
                                     SilkyRabbitMQProperties.PersistenceProperties persistenceProperties,
                                     ListenerRegistry listenerRegistry) {
        this.messageSerializer = messageSerializer;
        this.persistenceService = persistenceService;
        this.rabbitTemplate = rabbitTemplate;
        this.listenerProperties = listenerProperties;
        this.persistenceProperties = persistenceProperties;
        this.listenerRegistry = listenerRegistry;

        this.autoAck = rabbitProperties.getListener().getSimple().getAcknowledgeMode().isAutoAck();
        this.retryEnabled = rabbitProperties.getListener().getSimple().getRetry().isEnabled();
        this.maxRetryCount = rabbitProperties.getListener().getSimple().getRetry().getMaxAttempts();

        this.enableDlx = listenerProperties.isEnableDlx();
    }

    /**
     * 获取所有监听器的队列名称
     */
    public String[] getListenerQueueNames() {
        return listenerRegistry.getListenerQueueNames();
    }

    /**
     * 统一的消息处理方法
     */
    @RabbitListener(queues = "#{listenerRegistry.getListenerQueueNames()}")
    public void handleMessage(Message amqpMessage,
                              Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                              @Header(AmqpHeaders.CONSUMER_QUEUE) String queueName) {

        logger.info("Received message: queue={}, deliveryTag={}, messageId={}",
                queueName, deliveryTag, amqpMessage.getMessageProperties().getMessageId());
        ProcessingContext context = new ProcessingContext(amqpMessage, channel, deliveryTag, queueName);

        try {
            // 1. 验证监听器
            RabbitMQListener<?> listener = validateListener(context);
            if (listener == null) {
                logger.warn("Listener validation failed, messageId: {}, queue: {}",
                        context.messageId, context.queueName);
                return;
            }

            // 2. 反序列化消息
            Object message = deserializeMessage(context, listener);
            if (message == null) {
                logger.warn("Message deserialization failed, messageId: {}, queue: {}",
                        context.messageId, context.queueName);
                return;
            }
            // 3. 处理消息
            this.processMessage(context, listener, message);

            // 4. 确认消息
            this.acknowledgeMessage(context);

        } catch (Exception e) {
            handleProcessingException(context, e);
        }
    }

    /**
     * 验证监听器是否存在
     */
    private RabbitMQListener<?> validateListener(ProcessingContext context) {
        RabbitMQListener<?> listener = listenerRegistry.getListener(context.queueName);
        if (listener == null) {
            String errorMsg = StrUtil.format("No listener found for queue: {}, messageId: {}",
                    context.queueName, context.messageId);
            logger.warn(errorMsg);
            recordPersistenceFailure(context.messageId, errorMsg, context.startTime);
            safeReject(context.channel, context.deliveryTag, false, "No listener found");
            return null;
        }
        return listener;
    }

    /**
     * 反序列化消息
     */
    private Object deserializeMessage(ProcessingContext context, RabbitMQListener<?> listener) {
        try {
            Object message = messageSerializer.deserialize(context.amqpMessage.getBody(),
                    listener.getMessageType());
            if (message == null) {
                logger.warn("Deserialized message is null, queue: {}, messageId: {}",
                        context.queueName, context.messageId);
                recordPersistenceFailure(context.messageId, "Message deserialization failed", context.startTime);
                safeReject(context.channel, context.deliveryTag, false, "Deserialization failed");
            }
            return message;
        } catch (Exception e) {
            logger.error("Message deserialization failed, queue: {}, messageId: {}",
                    context.queueName, context.messageId, e);
            recordPersistenceFailure(context.messageId, "Deserialization error: " + e.getMessage(), context.startTime);
            safeReject(context.channel, context.deliveryTag, false, "Deserialization error");
            return null;
        }
    }

    /**
     * 处理消息业务逻辑
     */
    @SuppressWarnings("unchecked")
    private void processMessage(ProcessingContext context, RabbitMQListener<?> listener, Object message) {
        try {
            logger.debug("Processing message: queue={}, messageId={}, type={}",
                    context.queueName, context.messageId, listener.getMessageType().getSimpleName());

            // 类型检查
            if (!(message instanceof BaseMassageSend)) {
                logger.error("Message type mismatch: expected BaseMassageSend, actual {}", message.getClass().getName());
                throw new RabbitMessageSendException("Message type mismatch");
            }

            ((RabbitMQListener<BaseMassageSend>) listener).onMessage(
                    (BaseMassageSend) message, context.channel, context.amqpMessage);

            context.processed = true;

        } catch (Exception e) {
            logger.error("Message processing failed: queue={}, messageId={}",
                    context.queueName, context.messageId, e);
            throw new RabbitMessageSendException("Message processing failed", e);
        }
    }

    /**
     * 确认消息处理成功
     */
    private void acknowledgeMessage(ProcessingContext context) {
        // 清除重试计数
        retryCountMap.remove(context.messageId);

        if (autoAck) {
            logger.debug("Auto ack mode - Spring handles acknowledgment: queue={}, messageId={}",
                    context.queueName, context.messageId);
        } else {
            safeAck(context.channel, context.deliveryTag);
            logger.debug("Manual acknowledgment successful: queue={}, messageId={}, costTime={}ms",
                    context.queueName, context.messageId, context.getCostTime());
        }

        recordPersistenceSuccess(context.messageId, context.startTime);
    }

    /**
     * 处理消费异常
     */
    private void handleProcessingException(ProcessingContext context, Exception exception) {
        logger.error("Message processing exception: queue={}, messageId={}",
                context.queueName, context.messageId, exception);

        if (autoAck) {
            recordPersistenceFailure(context.messageId,
                    "Auto ack mode failure: " + exception.getMessage(),
                    context.startTime);
            return;
        }

        handleManualAckFailure(context, exception);
    }

    /**
     * 处理手动确认模式下的失败
     */
    private void handleManualAckFailure(ProcessingContext context, Exception exception) {
        try {
            int currentRetryCount = getCurrentRetryCount(context);

            if (shouldRetry(currentRetryCount)) {
                handleRetry(context, currentRetryCount, exception);
            } else {
                handleMaxRetriesExceeded(context, exception);
            }
        } catch (Exception e) {
            logger.error("Error handling message failure, queue: {}, messageId: {}",
                    context.queueName, context.messageId, e);
            recordPersistenceFailure(context.messageId,
                    "Failure handling error: " + e.getMessage(),
                    context.startTime);
            safeReject(context.channel, context.deliveryTag, false, "Error handling failure");
        }
    }

    /**
     * 处理重试逻辑
     */
    private void handleRetry(ProcessingContext context, int currentRetryCount, Exception exception) {
        int nextRetry = currentRetryCount + 1;
        incrementRetryCount(context.messageId);

        logger.info("Message consume failed, requeue for retry {}/{}, queue: {}, messageId: {}",
                nextRetry, maxRetryCount, context.queueName, context.messageId);

        recordPersistenceFailure(context.messageId,
                String.format("Retry %d/%d: %s", nextRetry, maxRetryCount, exception.getMessage()),
                context.startTime);

        safeReject(context.channel, context.deliveryTag, true,
                String.format("Retry %d/%d", nextRetry, maxRetryCount));
    }

    /**
     * 处理重试次数用尽的情况
     */
    private void handleMaxRetriesExceeded(ProcessingContext context, Exception exception) {
        if (enableDlx) {
            handleDeadLetterQueue(context, exception);
            safeReject(context.channel, context.deliveryTag, false, "Max retries exceeded - Sent to DLX");
        } else {
            logger.error("Message consume failed after {} retries, discarding, queue: {}, messageId: {}",
                    maxRetryCount, context.queueName, context.messageId);
            recordPersistenceFailure(context.messageId,
                    "Max retries exceeded, message discarded: " + exception.getMessage(),
                    context.startTime);
            safeReject(context.channel, context.deliveryTag, false, "Max retries exceeded - Discarded");
        }
    }

    /**
     * 处理死信队列
     */
    private void handleDeadLetterQueue(ProcessingContext context, Exception exception) {
        logger.warn("Message consume failed after {} retries, sending to DLX, queue: {}, messageId: {}",
                maxRetryCount, context.queueName, context.messageId);

        try {
            // 记录死信队列信息
            recordDeadLetterMessage(context.messageId, context.queueName,
                    String.format("Consume failed after %d retries: %s",
                            maxRetryCount, exception.getMessage()));

            // 发送到死信交换机
            sendToDeadLetterExchange(context, exception);

            logger.info("Successfully sent message to DLX: messageId={}, queue={}",
                    context.messageId, context.queueName);

        } catch (Exception e) {
            logger.error("Failed to send message to DLX: messageId={}, queue={}",
                    context.messageId, context.queueName, e);
            recordDLQSendFailure(context.messageId, context.queueName, e.getMessage());
            throw new RabbitMessageSendException("Failed to send message to dead letter exchange", e);
        }
    }

    /**
     * 发送消息到死信交换机
     */
    private void sendToDeadLetterExchange(ProcessingContext context, Exception exception) {
        String dlxExchange = listenerProperties.getDlxExchange();
        String dlxRoutingKey = listenerProperties.getDlxRoutingKey();

        if (StrUtil.isBlank(dlxExchange) || StrUtil.isBlank(dlxRoutingKey)) {
            logger.warn("DLX enabled but no DLX exchange or routing key configured for queue: {}",
                    context.queueName);
            return;
        }

        Message dlqMessage = prepareDeadLetterMessage(context.amqpMessage, context.queueName, exception);

        rabbitTemplate.convertAndSend(dlxExchange, dlxRoutingKey, dlqMessage.getBody(), message -> {
            MessageProperties properties = message.getMessageProperties();
            enhanceDeadLetterMessageProperties(properties, context, exception);
            return message;
        });

        recordDLQSendSuccess(context.messageId, context.queueName, dlxExchange, dlxRoutingKey);
    }

    /**
     * 增强死信消息属性
     *
     * @param properties 死信消息属性
     * @param context    处理上下文
     * @param exception  异常信息
     */
    private void enhanceDeadLetterMessageProperties(MessageProperties properties,
                                                    ProcessingContext context,
                                                    Exception exception) {
        // 复制原始消息属性
        MessageProperties originalProps = context.amqpMessage.getMessageProperties();
        if (originalProps != null) {
            properties.setContentType(originalProps.getContentType());
            properties.setContentEncoding(originalProps.getContentEncoding());
            properties.setPriority(originalProps.getPriority());
            properties.setDeliveryMode(originalProps.getDeliveryMode());
        }

        // 设置死信消息头
        Map<String, Object> headers = new HashMap<>();
        if (MapUtil.isNotEmpty(properties.getHeaders())) {
            headers.putAll(properties.getHeaders());
        }

        headers.putAll(createDeadLetterHeaders(context, exception));
        properties.setHeaders(headers);

        properties.setMessageId(context.messageId + "_DLQ");
        properties.setTimestamp(new Date());
    }

    /**
     * 创建死信消息头
     */
    private Map<String, Object> createDeadLetterHeaders(ProcessingContext context, Exception exception) {
        Map<String, Object> headers = new HashMap<>();

        headers.put("x-death", createDeathHeader(context.queueName, listenerProperties.getDlxExchange()));
        headers.put("x-original-queue", context.queueName);
        headers.put("x-original-exchange", context.amqpMessage.getMessageProperties().getReceivedExchange());
        headers.put("x-original-routing-key", context.amqpMessage.getMessageProperties().getReceivedRoutingKey());
        headers.put("x-failure-reason", exception.getMessage());
        headers.put("x-failure-timestamp", new Date());
        headers.put("x-retry-count", maxRetryCount);
        headers.put("x-dead-letter-reason", "max_retries_exceeded");

        if (listenerProperties.getDlxMessageTtl() > 0) {
            headers.put("x-expiration", String.valueOf(listenerProperties.getDlxMessageTtl()));
        }

        return headers;
    }

    @Override
    public int getOrder() {
        return 2;
    }


    // ========== 工具方法 ==========

    /**
     * 消息处理上下文
     */
    private static class ProcessingContext {
        /**
         * AMQP 消息
         */
        final Message amqpMessage;

        /**
         * AMQP 频道
         */
        final Channel channel;

        /**
         * 消息投递标签
         */
        final long deliveryTag;

        /**
         * 队列名称
         */
        final String queueName;

        /**
         * 消息ID
         */
        final String messageId;

        /**
         * 消息关联ID
         */
        final String correlationId;

        /**
         * 处理开始时间
         */
        final long startTime;

        /**
         * 是否已处理
         */
        boolean processed = false;

        ProcessingContext(Message amqpMessage, Channel channel, long deliveryTag, String queueName) {
            this.amqpMessage = amqpMessage;
            this.channel = channel;
            this.deliveryTag = deliveryTag;
            this.queueName = queueName;
            this.messageId = amqpMessage.getMessageProperties().getMessageId();
            this.correlationId = amqpMessage.getMessageProperties().getCorrelationId();
            this.startTime = System.currentTimeMillis();
        }

        long getCostTime() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * 获取当前重试次数
     *
     * @param context 处理上下文
     * @return 当前重试次数
     */
    private int getCurrentRetryCount(ProcessingContext context) {
        Map<String, Object> headers = context.amqpMessage.getMessageProperties().getHeaders();
        int headerRetryCount = 0;
        if (MapUtil.isNotEmpty(headers) && headers.containsKey("x-retry-count")) {
            Object retryCount = headers.get("x-retry-count");
            if (retryCount instanceof Integer) {
                headerRetryCount = (Integer) retryCount;
            }
        }
        return retryCountMap.getOrDefault(context.messageId, new AtomicInteger(headerRetryCount)).get();
    }


    /**
     * 是否应该重试
     *
     * @param currentRetryCount 当前重试次数
     * @return 是否应该重试
     */
    private boolean shouldRetry(int currentRetryCount) {
        return retryEnabled && currentRetryCount < maxRetryCount;
    }


    /**
     * 增加重试计数
     *
     * @param messageId 消息ID
     */
    private void incrementRetryCount(String messageId) {
        retryCountMap.computeIfAbsent(messageId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    // ========== 安全操作方法 ==========

    /**
     * 安全确认消息
     *
     * @param channel     AMQP 频道
     * @param deliveryTag 消息投递标签
     */
    private void safeAck(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            logger.error("Failed to acknowledge message, deliveryTag: {}", deliveryTag, e);
            throw new RabbitMessageSendException("Message acknowledgement failed", e);
        }
    }


    /**
     * 安全拒绝消息
     *
     * @param channel     AMQP 频道
     * @param deliveryTag 消息投递标签
     * @param requeue     是否重新入队
     * @param reason      拒绝原因
     */
    private void safeReject(Channel channel, long deliveryTag, boolean requeue, String reason) {
        try {
            channel.basicReject(deliveryTag, requeue);
            logger.debug("Message rejected: deliveryTag={}, requeue={}, reason={}", deliveryTag, requeue, reason);
        } catch (IOException e) {
            logger.error("Failed to reject message, deliveryTag: {}, requeue: {}", deliveryTag, requeue, e);
            throw new RabbitMessageSendException("Message rejection failed", e);
        }
    }


    /**
     * 记录持久化成功
     *
     * @param messageId 消息ID
     * @param startTime 消息处理开始时间
     */
    private void recordPersistenceSuccess(String messageId, long startTime) {
        if (!isPersistenceEnabled()) {
            return;
        }
        long costTime = System.currentTimeMillis() - startTime;
        try {
            persistenceService.consumeSuccess(messageId, costTime);
        } catch (Exception e) {
            logger.error("Failed to record consumer success for messageId={}, costTime={}", messageId, costTime, e);
        }
    }

    /**
     * 记录持久化失败
     *
     * @param messageId 消息ID
     * @param error     错误信息
     * @param startTime 消息处理开始时间
     */
    private void recordPersistenceFailure(String messageId, String error, long startTime) {
        if (!isPersistenceEnabled()) return;

        long costTime = System.currentTimeMillis() - startTime;
        try {
            persistenceService.consumeFailure(messageId, error, costTime);
        } catch (Exception e) {
            logger.error("Failed to record consumer failure for messageId={}, costTime={}", messageId, costTime, e);
        }
    }


    /**
     * 记录发送到死信队列的消息
     *
     * @param messageId 消息ID
     * @param queueName 队列名称
     * @param error     错误信息
     */
    private void recordDeadLetterMessage(String messageId, String queueName, String error) {
        if (!isPersistenceEnabled()) {
            return;
        }
        try {
            persistenceService.recordMessageSendToDLQ(messageId, queueName, error);
        } catch (Exception e) {
            logger.error("Failed to record DLQ message for messageId={}, queue={}", messageId, queueName, e);
        }
    }

    /**
     * 记录死信队列发送成功
     *
     * @param messageId     消息ID
     * @param queueName     队列名称
     * @param dlxExchange   死信交换机
     * @param dlxRoutingKey 死信路由键
     */
    private void recordDLQSendSuccess(String messageId, String queueName, String dlxExchange, String dlxRoutingKey) {
        if (!isPersistenceEnabled()) return;

        try {
            persistenceService.recordDLQSendSuccess(messageId, queueName, dlxExchange, dlxRoutingKey);
        } catch (Exception e) {
            logger.error("Failed to record DLQ send success for messageId={}, queue={}", messageId, queueName, e);
        }
    }

    /**
     * 记录死信队列发送失败
     *
     * @param messageId 消息ID
     * @param queueName 队列名称
     * @param error     错误信息
     */
    private void recordDLQSendFailure(String messageId, String queueName, String error) {
        if (!isPersistenceEnabled()) return;

        try {
            persistenceService.recordDLQSendFailure(messageId, queueName, error);
        } catch (Exception e) {
            logger.error("Failed to record DLQ send failure for messageId={}, queue={}", messageId, queueName, e);
        }
    }

    /**
     * 检查持久化服务是否启用
     *
     * @return 是否启用
     */
    private boolean isPersistenceEnabled() {
        return persistenceService != null && persistenceProperties.isEnabled();
    }

    /**
     * 准备死信消息
     *
     * @param originalMessage 原始消息
     * @param queueName       队列名称
     * @param exception       异常信息
     * @return 死信消息
     */
    private Message prepareDeadLetterMessage(Message originalMessage, String queueName, Exception exception) {
        // 实现保持不变
        try {
            return new Message(originalMessage.getBody(), originalMessage.getMessageProperties());
        } catch (Exception e) {
            logger.error("Failed to prepare dead letter message for queue: {}", queueName, e);
            return originalMessage;
        }
    }

    /**
     * 创建 x-death 头信息
     *
     * @param queueName   队列名称
     * @param dlxExchange 死信交换机
     * @return x-death 头信息列表
     */
    private List<Map<String, Object>> createDeathHeader(String queueName, String dlxExchange) {
        // 实现保持不变
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
