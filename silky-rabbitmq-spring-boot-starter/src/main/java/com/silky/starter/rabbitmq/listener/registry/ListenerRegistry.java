package com.silky.starter.rabbitmq.listener.registry;

import com.silky.starter.rabbitmq.listener.RabbitMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监听器注册表
 *
 * @author zy
 * @date 2025-10-18 15:42
 **/
public class ListenerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ListenerRegistry.class);

    /**
     * 监听器缓存：queueName -> listener
     */
    private final Map<String, RabbitMQListener<?>> listenerMap = new ConcurrentHashMap<>();

    /**
     * 注册监听器
     */
    public void registerListener(RabbitMQListener<?> listener) {
        String queueName = listener.getQueueName();
        listenerMap.put(queueName, listener);
        logger.info("Registered RabbitMQ listener for queue: {}, message type: {}",
                queueName, listener.getMessageType().getSimpleName());
    }

    /**
     * 获取监听器
     */
    public RabbitMQListener<?> getListener(String queueName) {
        return listenerMap.get(queueName);
    }

    /**
     * 获取所有监听器的队列名称
     */
    public String[] getListenerQueueNames() {
        return listenerMap.keySet().toArray(new String[0]);
    }

    /**
     * 检查是否有监听器
     */
    public boolean hasListener(String queueName) {
        return listenerMap.containsKey(queueName);
    }

    /**
     * 获取监听器数量
     */
    public int getListenerCount() {
        return listenerMap.size();
    }
}
