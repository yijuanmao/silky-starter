package com.silky.starter.rabbitmq.service;

import com.silky.starter.rabbitmq.core.BaseMassageSend;
import com.silky.starter.rabbitmq.exception.RabbitMessageSendException;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.serialization.FastJson2MessageSerializer;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 消息接收器基类（使用Fastjson2反序列化）
 *
 * @param <T> 消息类型
 * @author zy
 * @date 2025-10-12 10:11
 **/
@Component
public abstract class AbstractMessageReceiver<T extends BaseMassageSend> {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMessageReceiver.class);

    @Getter
    private final Class<T> messageClass;
    @Autowired
    private FastJson2MessageSerializer messageSerializer;
    @Autowired(required = false)
    private MessagePersistenceService persistenceService;

    /**
     * 构造函数，通过反射获取泛型类型
     */
    @SuppressWarnings("unchecked")
    public AbstractMessageReceiver() {
        // 通过反射获取泛型类型
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                this.messageClass = (Class<T>) actualTypeArguments[0];
            } else {
                this.messageClass = (Class<T>) BaseMassageSend.class;
            }
        } else {
            this.messageClass = (Class<T>) BaseMassageSend.class;
        }
    }

    /**
     * 消息处理方法
     *
     * @param message 消息
     */
    protected abstract void handleMessage(T message);

    /**
     * 消息监听方法
     */
    @RabbitListener
    public void onMessage(Message amqpMessage) {
        long startTime = System.currentTimeMillis();
        String messageId = amqpMessage.getMessageProperties().getMessageId();

        try {
            byte[] body = amqpMessage.getBody();
            T message = messageSerializer.deserialize(body, messageClass);

            if (message != null) {
                logger.info("Received message: {}, messageId: {}", messageClass.getSimpleName(), messageId);

                // 处理消息
                this.handleMessage(message);

                long costTime = System.currentTimeMillis() - startTime;

                // 记录消费成功
                if (persistenceService != null) {
                    persistenceService.recordMessageConsume(messageId, costTime);
                }

                logger.debug("Message processed successfully: messageId={}, costTime={}ms",
                        messageId, costTime);
            } else {
                logger.warn("Deserialized message is null, messageId: {}", messageId);
            }
        } catch (Exception e) {
            long costTime = System.currentTimeMillis() - startTime;
            logger.error("Failed to process message: messageId={}", messageId, e);

            // 记录消费失败
            if (persistenceService != null) {
                persistenceService.recordMessageConsumeFailure(messageId, e.getMessage(), costTime);
            }
            // 可以根据业务需求决定是否抛出异常进行重试
            throw new RuntimeException("Message processing failed", e);
        }
    }
}
