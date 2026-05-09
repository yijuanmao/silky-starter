package com.silky.starter.redis.stream.core;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Stream 消息体
 *
 * @author zy
 * @date 2026-05-09
 */
@Data
@NoArgsConstructor
public class StreamMessage {

    /**
     * Stream 消息 ID（由 Redis 生成，格式：timestamp-sequence，如 1715000000000-0）
     */
    private String id;

    /**
     * Stream Key（队列名称）
     */
    private String streamKey;

    /**
     * 消费者组名
     */
    private String groupId;

    /**
     * 原始 payload JSON 字符串
     */
    private String payloadJson;

    /**
     * 扩展属性（可存储业务 ID、追踪 ID 等）
     */
    private Map<String, String> headers = new HashMap<>();

    /**
     * 消息发送时间戳（毫秒，由生产者填充）
     */
    private long timestamp;

    /**
     * 已投递次数（从 XPENDING 信息解析，首次消费为 1）
     */
    private long deliveryCount;

    public StreamMessage(String streamKey, Object payload) {
        this.streamKey = streamKey;
        this.payloadJson = JSON.toJSONString(payload);
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 获取业务载荷并反序列化为指定类型
     *
     * @param clazz 目标类型
     * @param <T>   业务对象类型
     * @return 反序列化后的业务对象
     */
    public <T> T getPayload(Class<T> clazz) {
        if (payloadJson == null || payloadJson.isEmpty()) {
            return null;
        }
        return JSON.parseObject(payloadJson, clazz);
    }

    /**
     * 获取指定头部值
     *
     * @param key 头部键
     * @return 头部值，不存在时返回 null
     */
    public String getHeader(String key) {
        return headers == null ? null : headers.get(key);
    }

    /**
     * 设置头部值
     *
     * @param key   头部键
     * @param value 头部值
     */
    public void putHeader(String key, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(key, value);
    }
}
