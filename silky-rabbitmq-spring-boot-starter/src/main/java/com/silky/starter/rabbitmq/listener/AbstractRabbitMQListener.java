package com.silky.starter.rabbitmq.listener;

import cn.hutool.core.thread.ThreadUtil;
import com.silky.starter.rabbitmq.core.BaseMassageSend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 抽象消息监听器基类（自动注册到容器）
 *
 * @author zy
 * @date 2025-10-16 10:43
 **/
public abstract class AbstractRabbitMQListener<T extends BaseMassageSend> implements RabbitMQListener<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RabbitMQListenerContainer listenerContainer;

    private final Class<T> messageType;
    private final String queueName;


    @SuppressWarnings("unchecked")
    public AbstractRabbitMQListener(String queueName) {
        this.queueName = queueName;

        // 通过反射获取泛型类型
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
                this.messageType = (Class<T>) actualTypeArguments[0];
            } else {
                throw new IllegalArgumentException("Cannot determine message type from generic parameters");
            }
        } else {
            throw new IllegalArgumentException("Subclass must specify generic type parameter");
        }
        // 自动注册到容器
        registerToContainer();
    }

    /**
     * 注册到监听器容器
     */
    private void registerToContainer() {
        // 使用后置初始化来确保 listenerContainer 已注入
        ThreadUtil.execute(() -> {
            try {
                // 等待 Spring 容器完全初始化
                Thread.sleep(100);
                if (listenerContainer != null) {
                    listenerContainer.registerListener(this);
                    logger.info("Auto-registered listener for queue: {}", queueName);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Failed to auto-register listener for queue: {}", queueName, e);
            }
        });
    }

    /**
     * 获取消息类型
     */
    @Override
    public Class<T> getMessageType() {
        return messageType;
    }

    /**
     * 获取监听的队列名称
     */
    @Override
    public String getQueueName() {
        return queueName;
    }
}
