package com.silky.starter.rabbitmq.serialization.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.silky.starter.rabbitmq.exception.SerializationException;
import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消息序列化器接口
 *
 * @author zy
 * @date 2025-10-12 09:42
 **/
@Slf4j
@Component
public class FastJson2MessageSerializer implements RabbitMqMessageSerializer {

    /**
     * 序列化对象为字节数组
     *
     * @param object 对象
     */
    @Override
    public <T> byte[] serialize(T object) throws SerializationException {
        if (object == null) {
            return null;
        }
        try {
            return JSON.toJSONBytes(object, JSONWriter.Feature.WriteClassName);
        } catch (Exception e) {
            log.error("Failed to serialize object: {}", object.getClass(), e);
            throw new SerializationException("Failed to serialize object: " + object.getClass(), e);
        }
    }

    /**
     * 序列化对象为字节数组
     *
     * @param object   待序列化的对象
     * @param features 序列化选项
     * @return 序列化后的字节数组
     */
    @Override
    public <T> byte[] serialize(T object, JSONWriter.Feature... features) throws SerializationException {
        if (object == null) {
            return null;
        }
        try {
            return JSON.toJSONBytes(object, features);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize object: " + object.getClass(), e);
        }
    }

    /**
     * 反序列化字节数组为对象
     *
     * @param bytes 字节数组
     * @param clazz 类
     */
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return JSON.parseObject(bytes, clazz);
        } catch (Exception e) {
            log.error("Failed to deserialize to class: {}", clazz, e);
            throw new SerializationException("Failed to deserialize to class: " + clazz, e);
        }
    }

    /**
     * 反序列化字节数组为对象（支持泛型）
     *
     * @param bytes         字节数组
     * @param typeReference 类型引用
     */
    @Override
    public <T> T deserialize(byte[] bytes, TypeReference<T> typeReference) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return JSONObject.parseObject(new String(bytes), typeReference);
        } catch (Exception e) {
            log.error("Failed to deserialize to type: {}", typeReference.getType(), e);
            throw new SerializationException("Failed to deserialize to type: " + typeReference.getType(), e);
        }
    }

    /**
     * 序列化对象为字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的字符串
     */
    @Override
    public <T> String serializeToString(T object) throws SerializationException {
        if (object == null) {
            return null;
        }
        try {
            return JSON.toJSONString(object, JSONWriter.Feature.WriteClassName);
        } catch (Exception e) {
            log.error("Failed to serialize object to string: {}", object.getClass(), e);
            throw new SerializationException("Failed to serialize object to string: " + object.getClass(), e);
        }
    }

    /**
     * 反序列化字符串为对象
     *
     * @param jsonString 待反序列化的字符串
     * @param clazz      目标对象的类
     * @return 反序列化后的对象
     */
    @Override
    public <T> T deserializeFromString(String jsonString, Class<T> clazz) throws SerializationException {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(jsonString, clazz);
        } catch (Exception e) {
            log.error("Failed to deserialize string to class: {}", clazz, e);
            throw new SerializationException("Failed to deserialize from string to class: " + clazz, e);
        }
    }
}
