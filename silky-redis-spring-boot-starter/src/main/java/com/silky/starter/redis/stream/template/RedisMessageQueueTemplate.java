package com.silky.starter.redis.stream.template;

import com.silky.starter.redis.stream.core.MessageRepository;
import com.silky.starter.redis.stream.core.StreamMessage;
import com.silky.starter.redis.stream.support.StreamFieldConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Stream 消息生产者模板
 *
 * @author zy
 * @date 2026-05-09
 */
public class RedisMessageQueueTemplate {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageQueueTemplate.class);
    private static final String HEADER_DB_ID = "dbId";
    private static final int MAX_HEADERS_COUNT = 64;
    private static final int MAX_HEADER_KEY_LENGTH = 128;
    private static final int MAX_HEADER_VALUE_LENGTH = 2048;

    private final StringRedisTemplate stringRedisTemplate;
    private final MessageRepository messageRepository;

    public RedisMessageQueueTemplate(StringRedisTemplate stringRedisTemplate,
                                     MessageRepository messageRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.messageRepository = messageRepository;
    }

    /**
     * 发送消息到指定 Stream
     *
     * @param streamKey Stream Key
     * @param payload   业务对象（会被序列化为 JSON）
     * @return Redis 生成的消息 ID（格式：timestamp-sequence）
     */
    public String send(String streamKey, Object payload) {
        return send(streamKey, payload, null);
    }

    /**
     * 发送消息到指定 Stream，并携带扩展头部
     *
     * @param streamKey Stream Key
     * @param payload   业务对象
     * @param headers   扩展属性（如 traceId、bizType 等）
     * @return Redis 生成的消息 ID
     */
    public String send(String streamKey, Object payload, Map<String, String> headers) {
        validateSendArguments(streamKey, payload, headers);

        StreamMessage message = new StreamMessage(streamKey, payload);
        if (headers != null) {
            headers.forEach(message::putHeader);
        }

        // 回调 onSend（用户可在此做入库记录）
        try {
            String dbId = messageRepository.onSend(message);
            if (dbId != null && !dbId.isEmpty()) {
                message.putHeader(HEADER_DB_ID, dbId);
            }
        } catch (Exception e) {
            log.warn("[RedisStream] MessageRepository.onSend 回调异常，stream={}", streamKey, e);
        }

        // 构造 Stream record（map 存储，所有 value 均为 String）
        Map<String, String> recordMap = buildRecordMap(message);

        RecordId recordId = stringRedisTemplate.opsForStream()
                .add(StreamRecords.mapBacked(recordMap).withStreamKey(streamKey));

        if (recordId == null || recordId.getValue() == null || recordId.getValue().isEmpty()) {
            throw new IllegalStateException("Redis Stream add returned null RecordId, streamKey=" + streamKey);
        }

        String id = recordId.getValue();
        message.setId(id);

        log.debug("[RedisStream] 消息发送成功: stream={}, id={}, payloadLen={}",
                streamKey, id, message.getPayloadJson() != null ? message.getPayloadJson().length() : 0);

        return id;
    }

    /**
     * 构造存入 Stream 的 Map（所有 field 均为 String）
     */
    private Map<String, String> buildRecordMap(StreamMessage message) {
        Map<String, String> map = new HashMap<>(4);
        map.put(StreamFieldConstants.FIELD_PAYLOAD, message.getPayloadJson() != null ? message.getPayloadJson() : "");
        map.put(StreamFieldConstants.FIELD_TIMESTAMP, String.valueOf(message.getTimestamp()));

        // headers 序列化为 JSON 存储（避免多 field 膨胀）
        if (message.getHeaders() != null && !message.getHeaders().isEmpty()) {
            map.put(StreamFieldConstants.FIELD_HEADERS, com.alibaba.fastjson2.JSON.toJSONString(message.getHeaders()));
        }
        return map;
    }

    /**
     * 校验发送参数，避免将非法数据写入 Stream。
     */
    private void validateSendArguments(String streamKey, Object payload, Map<String, String> headers) {
        if (streamKey == null || streamKey.trim().isEmpty()) {
            throw new IllegalArgumentException("streamKey must not be blank");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        if (headers == null || headers.isEmpty()) {
            return;
        }
        if (headers.size() > MAX_HEADERS_COUNT) {
            throw new IllegalArgumentException("headers size exceeds limit: " + MAX_HEADERS_COUNT);
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("header key must not be blank");
            }
            if (key.length() > MAX_HEADER_KEY_LENGTH) {
                throw new IllegalArgumentException("header key length exceeds limit: " + MAX_HEADER_KEY_LENGTH);
            }
            if (value != null && value.length() > MAX_HEADER_VALUE_LENGTH) {
                throw new IllegalArgumentException("header value length exceeds limit: " + MAX_HEADER_VALUE_LENGTH);
            }
        }
    }
}
