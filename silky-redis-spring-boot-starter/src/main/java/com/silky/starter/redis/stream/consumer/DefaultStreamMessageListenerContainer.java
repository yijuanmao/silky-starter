package com.silky.starter.redis.stream.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.silky.starter.redis.stream.config.RedisStreamProperties;
import com.silky.starter.redis.stream.core.MessageListener;
import com.silky.starter.redis.stream.core.MessageRepository;
import com.silky.starter.redis.stream.core.StreamMessage;
import com.silky.starter.redis.stream.support.StreamFieldConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis Stream 默认消费者容器
 * <p>
 * 实现了完整的消费生命周期：
 * <ol>
 *   <li>初始化：自动创建 Stream 和 Consumer Group（若不存在）</li>
 *   <li>主循环：XREADGROUP 拉取新消息 → 处理 → ACK / 重试 / 死信</li>
 *   <li>Pending 回收：定期 XPENDING + XCLAIM 处理超时的积压消息</li>
 *   <li>优雅关闭：等待当前批次处理完成后退出</li>
 * </ol>
 * </p>
 *
 * @author zy
 * @date 2026-05-09
 */
public class DefaultStreamMessageListenerContainer {

    private static final Logger log = LoggerFactory.getLogger(DefaultStreamMessageListenerContainer.class);

    private final StringRedisTemplate redisTemplate;
    private final MessageRepository repository;
    private final RedisStreamProperties properties;
    private final String streamKey;
    private final String groupName;
    private final String consumerName;
    private final MessageListener listener;
    private final Executor executor;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public DefaultStreamMessageListenerContainer(StringRedisTemplate redisTemplate,
                                                  MessageRepository repository,
                                                  RedisStreamProperties properties,
                                                  String streamKey,
                                                  String groupName,
                                                  String consumerName,
                                                  MessageListener listener,
                                                  Executor executor) {
        this.redisTemplate = redisTemplate;
        this.repository = repository;
        this.properties = properties;
        this.streamKey = streamKey;
        this.groupName = groupName;
        this.consumerName = consumerName;
        this.listener = listener;
        this.executor = executor;
    }

    /**
     * 启动消费者容器（提交到线程池异步执行）
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[RedisStream] 容器已在运行中: stream={}, consumer={}", streamKey, consumerName);
            return;
        }
        initStreamAndGroup();
        executor.execute(this::consumeLoop);
        log.info("[RedisStream] 消费者容器启动: stream={}, group={}, consumer={}",
                streamKey, groupName, consumerName);
    }

    /**
     * 停止消费者容器（设置标志位，等待当前批次完成后退出）
     */
    public void stop() {
        running.set(false);
        log.info("[RedisStream] 消费者容器停止信号已发出: stream={}, consumer={}", streamKey, consumerName);
    }

    public boolean isRunning() {
        return running.get();
    }

    public String getStreamKey() {
        return streamKey;
    }

    public String getConsumerName() {
        return consumerName;
    }

    // ========================= 初始化 =========================

    private void initStreamAndGroup() {
        try {
            // 若 Stream 不存在则先创建（通过 XADD 一条空消息再删除）
            Boolean exists = redisTemplate.hasKey(streamKey);
            if (Boolean.FALSE.equals(exists)) {
                log.debug("[RedisStream] Stream 不存在，尝试创建: {}", streamKey);
                RecordId initId = redisTemplate.opsForStream()
                        .add(StreamRecords.mapBacked(Collections.singletonMap("_init", "1"))
                                .withStreamKey(streamKey));
                if (initId != null) {
                    redisTemplate.opsForStream().delete(streamKey, initId.getValue());
                }
                log.info("[RedisStream] Stream 创建成功: {}", streamKey);
            }
        } catch (Exception e) {
            log.debug("[RedisStream] Stream 初始化检测异常（可忽略）: stream={}, error={}", streamKey, e.getMessage());
        }

        // 创建 Consumer Group（若不存在）
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), groupName);
            log.info("[RedisStream] Consumer Group 创建成功: stream={}, group={}", streamKey, groupName);
        } catch (Exception e) {
            // BUSYGROUP：Group 已存在，正常情况，忽略
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("[RedisStream] Consumer Group 已存在（正常）: stream={}, group={}", streamKey, groupName);
            } else {
                log.warn("[RedisStream] Consumer Group 创建异常: stream={}, group={}, error={}",
                        streamKey, groupName, e.getMessage());
            }
        }
    }

    // ========================= 消费主循环 =========================

    private void consumeLoop() {
        RedisStreamProperties.ConsumerConfig consumerConfig = properties.getConsumer();
        Duration pollTimeout = Duration.ofMillis(consumerConfig.getPollTimeoutMs());

        while (running.get()) {
            try {
                // 1. 优先处理 pending 超时消息（XCLAIM）
                processPendingMessages(consumerConfig);

                // 2. 拉取新消息（XREADGROUP，阻塞读）
                List<MapRecord<String, Object, Object>> records =
                        redisTemplate.opsForStream().read(
                                Consumer.from(groupName, consumerName),
                                StreamReadOptions.empty()
                                        .count(consumerConfig.getBatchSize())
                                        .block(pollTimeout),
                                StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                        );

                if (records == null || records.isEmpty()) {
                    continue;
                }

                // 3. 处理每条消息
                for (MapRecord<String, Object, Object> record : records) {
                    processRecord(record, consumerConfig);
                }

            } catch (Exception e) {
                if (running.get()) {
                    log.error("[RedisStream] 消费循环异常: stream={}, consumer={}", streamKey, consumerName, e);
                    // 短暂休眠避免 CPU 空转
                    sleep(1000);
                }
            }
        }
        log.info("[RedisStream] 消费者容器已退出循环: stream={}, consumer={}", streamKey, consumerName);
    }

    // ========================= 单条消息处理 =========================

    private void processRecord(MapRecord<String, Object, Object> record,
                               RedisStreamProperties.ConsumerConfig consumerConfig) {
        StreamMessage message = buildMessage(record);
        processSingleMessage(message, record, consumerConfig);
    }

    private void processSingleMessage(StreamMessage message,
                                      MapRecord<String, Object, Object> record,
                                      RedisStreamProperties.ConsumerConfig consumerConfig) {
        long startMs = System.currentTimeMillis();

        // 回调 onReceive
        try {
            repository.onReceive(message, consumerName);
        } catch (Exception e) {
            log.warn("[RedisStream] onReceive 回调异常（不影响消费）: stream={}, id={}", streamKey, message.getId(), e);
        }

        try {
            // 执行业务逻辑
            Object result = listener.handle(message);
            long elapsed = System.currentTimeMillis() - startMs;

            // ACK
            redisTemplate.opsForStream().acknowledge(streamKey, groupName, record.getId().getValue());

            // 慢消费告警
            if (properties.getMonitor().isEnabled()
                    && elapsed >= properties.getMonitor().getSlowThresholdMs()) {
                log.warn("[RedisStream][慢消费] stream={}, id={}, elapsed={}ms（阈值={}ms）",
                        streamKey, message.getId(), elapsed, properties.getMonitor().getSlowThresholdMs());
            } else {
                log.debug("[RedisStream] 消息处理成功: stream={}, id={}, elapsed={}ms",
                        streamKey, message.getId(), elapsed);
            }

            // 回调 onSuccess
            try {
                repository.onSuccess(message, result);
            } catch (Exception e) {
                log.warn("[RedisStream] onSuccess 回调异常: stream={}, id={}", streamKey, message.getId(), e);
            }

        } catch (Exception e) {
            handleFailure(message, e, consumerConfig);
        }
    }

    // ========================= 失败处理 =========================

    private void handleFailure(StreamMessage message, Exception exception,
                               RedisStreamProperties.ConsumerConfig consumerConfig) {
        int retryCount = (int) message.getDeliveryCount();
        log.warn("[RedisStream] 消息处理失败: stream={}, id={}, deliveryCount={}, error={}",
                streamKey, message.getId(), retryCount, exception.getMessage());

        boolean shouldRetry;
        try {
            shouldRetry = repository.onFailure(message, exception, retryCount);
        } catch (Exception e) {
            log.warn("[RedisStream] onFailure 回调异常，默认继续重试: stream={}, id={}", streamKey, message.getId(), e);
            shouldRetry = true;
        }

        if (shouldRetry && retryCount < consumerConfig.getMaxRetries()) {
            // 不 ACK，留在 PEL，等待下次 XCLAIM 重新投递
            log.info("[RedisStream] 消息将在 pending 超时后重试: stream={}, id={}, retryCount={}/{}",
                    streamKey, message.getId(), retryCount, consumerConfig.getMaxRetries());
        } else {
            // 超过最大重试次数或用户主动放弃 → 死信
            String reason = shouldRetry
                    ? "超过最大重试次数 " + consumerConfig.getMaxRetries()
                    : "用户 onFailure 返回 false，放弃重试";
            sendToDeadLetter(message, reason, exception);
        }
    }

    // ========================= 死信处理 =========================

    private void sendToDeadLetter(StreamMessage message, String reason, Exception exception) {
        if (!properties.getDeadLetter().isEnabled()) {
            // 死信未启用，直接 ACK 丢弃
            try {
                redisTemplate.opsForStream().acknowledge(streamKey, groupName, message.getId());
            } catch (Exception ackEx) {
                log.warn("[RedisStream] 死信 ACK 失败: stream={}, id={}", streamKey, message.getId(), ackEx);
            }
            log.warn("[RedisStream] 消息已丢弃（死信队列未启用）: stream={}, id={}, reason={}", streamKey, message.getId(), reason);
            return;
        }

        String deadStreamKey = streamKey + properties.getDeadLetter().getSuffix();
        message.putHeader("dead-reason", reason);
        message.putHeader("dead-exception", exception != null ? exception.getClass().getName() + ": " + exception.getMessage() : "unknown");
        message.putHeader("dead-time", String.valueOf(System.currentTimeMillis()));
        message.putHeader("original-stream", streamKey);

        try {
            Map<String, String> deadRecord = new HashMap<>(4);
            deadRecord.put(StreamFieldConstants.FIELD_PAYLOAD, message.getPayloadJson() != null ? message.getPayloadJson() : "");
            deadRecord.put(StreamFieldConstants.FIELD_TIMESTAMP, String.valueOf(message.getTimestamp()));
            deadRecord.put(StreamFieldConstants.FIELD_HEADERS, JSON.toJSONString(message.getHeaders()));

            redisTemplate.opsForStream().add(
                    StreamRecords.mapBacked(deadRecord).withStreamKey(deadStreamKey));

            // ACK 原消息
            redisTemplate.opsForStream().acknowledge(streamKey, groupName, message.getId());

            log.warn("[RedisStream] 消息已转入死信队列: originalStream={}, deadStream={}, id={}, reason={}",
                    streamKey, deadStreamKey, message.getId(), reason);

            // 回调 onDeadLetter
            try {
                repository.onDeadLetter(message, reason);
            } catch (Exception e) {
                log.warn("[RedisStream] onDeadLetter 回调异常: stream={}, id={}", streamKey, message.getId(), e);
            }

        } catch (Exception e) {
            log.error("[RedisStream] 死信写入失败: stream={}, id={}", streamKey, message.getId(), e);
        }
    }

    // ========================= Pending 消息回收 =========================

    private void processPendingMessages(RedisStreamProperties.ConsumerConfig consumerConfig) {
        try {
            // 查询超过 pendingTimeoutMs 的 pending 消息
            PendingMessagesSummary summary = redisTemplate.opsForStream()
                    .pending(streamKey, groupName);

            if (summary == null || summary.getTotalPendingMessages() == 0) {
                return;
            }

            // 获取 pending 消息详情
            PendingMessages pendingMessages = redisTemplate.opsForStream()
                    .pending(streamKey, Consumer.from(groupName, consumerName),
                            Range.unbounded(), consumerConfig.getBatchSize());

            if (pendingMessages == null) {
                return;
            }

            long now = System.currentTimeMillis();
            for (PendingMessage pending : pendingMessages) {
                // 实际用 elapsedTimeSinceLastDelivery 直接比较
                if (pending.getElapsedTimeSinceLastDelivery().toMillis() >= consumerConfig.getPendingTimeoutMs()) {
                    // Spring Data Redis 2.7 未暴露 XCLAIM，通过重新 XREADGROUP 指定 ID 拉取该 pending 消息
                    try {
                        String pendingId = pending.getIdAsString();
                        // 使用 range 命令直接读取该条消息内容（绕过 XCLAIM）
                        List<MapRecord<String, Object, Object>> rangeResult =
                                redisTemplate.opsForStream().range(
                                        streamKey,
                                        org.springframework.data.domain.Range.closed(pendingId, pendingId)
                                );

                        if (rangeResult != null && !rangeResult.isEmpty()) {
                            log.debug("[RedisStream] 重新处理超时 pending 消息: stream={}, id={}",
                                    streamKey, pendingId);
                            for (MapRecord<String, Object, Object> record : rangeResult) {
                                // deliveryCount 从 pending 信息中获取
                                StreamMessage msg = buildMessage(record);
                                msg.setDeliveryCount(pending.getTotalDeliveryCount());
                                processSingleMessage(msg, record, consumerConfig);
                            }
                        }
                    } catch (Exception claimEx) {
                        log.debug("[RedisStream] 处理超时 pending 消息失败（可忽略）: stream={}, id={}, error={}",
                                streamKey, pending.getIdAsString(), claimEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[RedisStream] 处理 pending 消息异常（可忽略）: stream={}, error={}", streamKey, e.getMessage());
        }
    }

    // ========================= 工具方法 =========================

    /**
     * 将 Redis Stream record 解析为 StreamMessage
     */
    private StreamMessage buildMessage(MapRecord<String, Object, Object> record) {
        StreamMessage message = new StreamMessage();
        message.setId(record.getId().getValue());
        message.setStreamKey(streamKey);
        message.setGroupId(groupName);

        Map<Object, Object> entries = record.getValue();

        // payload
        Object payload = entries.get(StreamFieldConstants.FIELD_PAYLOAD);
        message.setPayloadJson(payload != null ? payload.toString() : null);

        // timestamp
        Object ts = entries.get(StreamFieldConstants.FIELD_TIMESTAMP);
        if (ts != null) {
            try {
                message.setTimestamp(Long.parseLong(ts.toString()));
            } catch (NumberFormatException ignore) {
            }
        }

        // headers（JSON 反序列化）
        Object headersJson = entries.get(StreamFieldConstants.FIELD_HEADERS);
        if (headersJson != null && !headersJson.toString().isEmpty()) {
            try {
                Map<String, String> headers = JSON.parseObject(headersJson.toString(),
                        new TypeReference<Map<String, String>>() {});
                message.setHeaders(headers != null ? headers : new HashMap<>());
            } catch (Exception ignore) {
            }
        }

        // deliveryCount（从消息 ID 解析投递次数较复杂，此处默认置为 1，pending 流程中会通过 XPENDING 补充）
        message.setDeliveryCount(1);

        return message;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
