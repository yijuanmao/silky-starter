package com.silky.starter.rabbitmq.listener;

import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import com.silky.starter.rabbitmq.listener.registry.ListenerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 抽象消息监听器基类（自动注册到容器）
 *
 * @author zy
 * @date 2025-10-16 10:43
 **/
public abstract class AbstractRabbitMQListener<T extends BaseMassageSend> implements RabbitMQListener<T>, SmartLifecycle {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ListenerRegistry listenerRegistry;

    private final Class<T> messageType;
    private final String queueName;

    private boolean running = false;


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
    }

    /**
     * 启动时注册到注册表
     */
    @Override
    public void start() {
        if (!running) {
            listenerRegistry.registerListener(this);
            running = true;
            logger.info("Started and registered listener for queue: {}", queueName);
        }
    }

    @Override
    public void stop() {
        running = false;
        logger.info("Stopped listener for queue: {}", queueName);
    }

    @Override
    public boolean isRunning() {
        return running;
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
