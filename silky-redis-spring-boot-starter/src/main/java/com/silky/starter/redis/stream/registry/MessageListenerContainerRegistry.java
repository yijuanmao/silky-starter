package com.silky.starter.redis.stream.registry;

import com.silky.starter.redis.stream.config.RedisStreamProperties;
import com.silky.starter.redis.stream.consumer.DefaultStreamMessageListenerContainer;
import com.silky.starter.redis.stream.core.MessageListener;
import com.silky.starter.redis.stream.core.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Redis Stream 消费者容器注册器
 *
 * @author zy
 * @date 2026-05-09
 */
public class MessageListenerContainerRegistry implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MessageListenerContainerRegistry.class);

    /**
     * key = streamKey:consumerName → 容器列表
     */
    private final Map<String, List<DefaultStreamMessageListenerContainer>> containerMap =
            new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;
    private final MessageRepository messageRepository;
    private final RedisStreamProperties properties;
    private final Executor executor;

    public MessageListenerContainerRegistry(StringRedisTemplate redisTemplate,
                                            MessageRepository messageRepository,
                                            RedisStreamProperties properties,
                                            Executor executor) {
        this.redisTemplate = redisTemplate;
        this.messageRepository = messageRepository;
        this.properties = properties;
        this.executor = executor;
    }

    /**
     * 注册并启动消费者容器
     *
     * @param streamKey   Stream Key
     * @param groupName   消费者组名
     * @param concurrency 并发数（会创建 concurrency 个消费者实例）
     * @param listener    消息处理逻辑
     */
    public void register(String streamKey, String groupName, int concurrency, MessageListener listener) {
        register(streamKey, groupName, resolveConsumerPrefix(streamKey), concurrency, listener);
    }

    /**
     * 注册并启动消费者容器（自定义消费者名前缀）
     *
     * @param streamKey      Stream Key
     * @param groupName      消费者组名
     * @param consumerPrefix 消费者名前缀（实际名称会追加 -N 序号后缀）
     * @param concurrency    并发数
     * @param listener       消息处理逻辑
     */
    public void register(String streamKey, String groupName, String consumerPrefix,
                         int concurrency, MessageListener listener) {
        String resolvedGroup = (groupName == null || groupName.isEmpty())
                ? properties.getDefaultGroup() : groupName;

        for (int i = 0; i < concurrency; i++) {
            String consumerName = consumerPrefix + "-" + i;
            DefaultStreamMessageListenerContainer container =
                    new DefaultStreamMessageListenerContainer(
                            redisTemplate, messageRepository, properties,
                            streamKey, resolvedGroup, consumerName, listener, executor);

            String mapKey = streamKey + ":" + consumerName;
            containerMap.computeIfAbsent(mapKey, k -> new ArrayList<>()).add(container);
            container.start();
        }

        log.info("[RedisStream] 注册消费者完成: stream={}, group={}, concurrency={}",
                streamKey, resolvedGroup, concurrency);
    }

    /**
     * 停止指定 Stream 的所有消费者容器
     */
    public void stop(String streamKey) {
        containerMap.entrySet().stream()
                .filter(e -> e.getKey().startsWith(streamKey + ":"))
                .flatMap(e -> e.getValue().stream())
                .forEach(DefaultStreamMessageListenerContainer::stop);
    }

    /**
     * 获取所有已注册的容器（只读视图）
     */
    public List<DefaultStreamMessageListenerContainer> getAllContainers() {
        List<DefaultStreamMessageListenerContainer> all = new ArrayList<>();
        containerMap.values().forEach(all::addAll);
        return all;
    }

    /**
     * Spring 容器销毁时优雅关闭所有消费者
     */
    @Override
    public void destroy() {
        log.info("[RedisStream] 开始关闭所有消费者容器...");
        containerMap.values().stream()
                .flatMap(List::stream)
                .forEach(container -> {
                    try {
                        container.stop();
                    } catch (Exception e) {
                        log.warn("[RedisStream] 容器关闭异常: stream={}, consumer={}",
                                container.getStreamKey(), container.getConsumerName(), e);
                    }
                });
        log.info("[RedisStream] 所有消费者容器已关闭，共 {} 个", containerMap.size());
        containerMap.clear();
    }

    // ========================= 工具方法 =========================

    /**
     * 生成消费者名称前缀（hostname + streamKey 简称）
     */
    private String resolveConsumerPrefix(String streamKey) {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            // 去除特殊字符，截取简短名称
            String cleanKey = streamKey.replaceAll("[^a-zA-Z0-9-]", "-");
            if (cleanKey.length() > 20) {
                cleanKey = cleanKey.substring(cleanKey.length() - 20);
            }
            return host + "-" + cleanKey;
        } catch (Exception e) {
            return "silky-consumer-" + streamKey.hashCode();
        }
    }
}
