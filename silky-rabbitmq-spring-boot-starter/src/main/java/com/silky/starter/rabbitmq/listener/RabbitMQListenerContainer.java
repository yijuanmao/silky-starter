//package com.silky.starter.rabbitmq.listener;
//
//import com.rabbitmq.client.Channel;
//import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
//import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
//import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * RabbitMQ 消息监听器容器（统一管理所有监听器）
// *
// * @author zy
// * @date 2025-10-16 10:40
// **/
//@Component
//public class RabbitMQListenerContainer {
//
//    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListenerContainer.class);
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
//    public RabbitMQListenerContainer(RabbitMqMessageSerializer messageSerializer, MessagePersistenceService persistenceService, RabbitTemplate rabbitTemplate) {
//        this.messageSerializer = messageSerializer;
//        this.persistenceService = persistenceService;
//        this.rabbitTemplate = rabbitTemplate;
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
//        logger.info("Registered RabbitMQ listener for queue: {}, message type: {}",
//                queueName, listener.getMessageType().getSimpleName());
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
////    @RabbitListener(queues = "#{@rabbitMQListenerContainer.getListenerQueueNames()}")
//    public void handleMessage(Message amqpMessage,
//                              Channel channel,
//                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
//                              @Header(AmqpHeaders.CONSUMER_QUEUE) String queueName) {
//
//        long startTime = System.currentTimeMillis();
//        String messageId = amqpMessage.getMessageProperties().getMessageId();
//
//        RabbitMQListener<?> listener = listenerMap.get(queueName);
//        if (listener == null) {
//            logger.warn("No listener found for queue: {}, messageId: {}", queueName, messageId);
//            // 没有监听器，拒绝消息并重新入队
//            basicReject(channel, deliveryTag, true, "No listener found");
//            return;
//        }
//
//        try {
//            // 反序列化消息
//            Object message = deserializeMessage(amqpMessage.getBody(), listener.getMessageType());
//
//            if (message != null) {
//                logger.debug("Received message from queue: {}, messageId: {}, type: {}, autoAck: {}",
//                        queueName, messageId, listener.getMessageType().getSimpleName(), listener.autoAck());
//
//                // 调用监听器的处理方法
//                invokeListener(listener, message, channel, amqpMessage);
//
//                long costTime = System.currentTimeMillis() - startTime;
//
//                // 记录消费成功
//                if (persistenceService != null) {
//                    persistenceService.recordMessageConsume(messageId, costTime);
//                }
//
//                // 手动确认消息（如果配置了手动确认）
//                if (!listener.autoAck()) {
//                    basicAck(channel, deliveryTag);
//                    logger.debug("Message manually acknowledged: queue={}, messageId={}, deliveryTag={}",
//                            queueName, messageId, deliveryTag);
//                }
//
//                logger.debug("Message processed successfully: queue={}, messageId={}, costTime={}ms",
//                        queueName, messageId, costTime);
//            } else {
//                logger.warn("Deserialized message is null, queue: {}, messageId: {}", queueName, messageId);
//                // 消息反序列化失败，拒绝消息并不重新入队
//                basicReject(channel, deliveryTag, false, "Message deserialization failed");
//            }
//
//        } catch (Exception e) {
//            long costTime = System.currentTimeMillis() - startTime;
//            logger.error("Failed to process message: queue={}, messageId={}", queueName, messageId, e);
//
//            // 记录消费失败
//            if (persistenceService != null) {
//                persistenceService.recordMessageConsumeFailure(messageId, e.getMessage(), costTime);
//            }
//
//            // 根据监听器配置决定是否重试
//            handleConsumeFailure(listener, channel, deliveryTag, amqpMessage, queueName, e);
//        }
//    }
//
//
//    /**
//     * 获取所有监听器的队列名称（用于 @RabbitListener 的队列配置）
//     */
//    public String[] getListenerQueueNames() {
//        return listenerMap.keySet().toArray(new String[0]);
//    }
//
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
//     * 处理消费失败
//     */
//    private void handleConsumeFailure(RabbitMQListener<?> listener, Channel channel, long deliveryTag,
//                                      Message amqpMessage, String queueName, Exception e) {
//
//        // 如果配置了自动确认，由 Spring 自动处理
//        if (listener.autoAck()) {
//            logger.debug("Auto ack mode, letting Spring handle the failure");
//            return;
//        }
//
//        // 手动确认模式下的失败处理
//        try {
//            int retryCount = getRetryCount(amqpMessage);
//            int maxRetries = listener.retryTimes();
//
//            if (retryCount < maxRetries) {
//                // 重新投递消息进行重试
//                logger.info("Message consume failed, requeue for retry {}/{}, queue: {}",
//                        retryCount + 1, maxRetries, queueName);
//                basicReject(channel, deliveryTag, true, "Retry " + (retryCount + 1));
//            } else {
//                // 重试次数用完，进入死信队列或直接拒绝
//                if (listener.enableDlx()) {
//                    logger.warn("Message consume failed after {} retries, sending to DLX, queue: {}",
//                            maxRetries, queueName);
//                    basicReject(channel, deliveryTag, false, "Max retries exceeded");
//                } else {
//                    logger.error("Message consume failed after {} retries, discarding, queue: {}",
//                            maxRetries, queueName);
//                    basicReject(channel, deliveryTag, false, "Max retries exceeded");
//                }
//            }
//        } catch (Exception ex) {
//            logger.error("Failed to handle consume failure, queue: {}", queueName, ex);
//            // 最后的手段：拒绝消息并不重新入队
//            basicReject(channel, deliveryTag, false, "Error handling failure: " + ex.getMessage());
//        }
//    }
//
//    /**
//     * 获取重试次数
//     */
//    private int getRetryCount(Message amqpMessage) {
//        Map<String, Object> headers = amqpMessage.getMessageProperties().getHeaders();
//        if (headers != null && headers.containsKey("x-retry-count")) {
//            Object retryCount = headers.get("x-retry-count");
//            if (retryCount instanceof Integer) {
//                return (Integer) retryCount;
//            }
//        }
//        return 0;
//    }
//
//    /**
//     * 安全的消息确认方法
//     */
//    private void basicAck(Channel channel, long deliveryTag) {
//        try {
//            channel.basicAck(deliveryTag, false);
//        } catch (IOException e) {
//            logger.error("Failed to acknowledge message, deliveryTag: {}", deliveryTag, e);
//            throw new RuntimeException("Message acknowledgement failed", e);
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
//            throw new RuntimeException("Message rejection failed", e);
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
//            logger.error("Failed to nack message, deliveryTag: {}, multiple: {}, requeue: {}",
//                    deliveryTag, multiple, requeue, e);
//            throw new RuntimeException("Message nack failed", e);
//        }
//    }
//}
