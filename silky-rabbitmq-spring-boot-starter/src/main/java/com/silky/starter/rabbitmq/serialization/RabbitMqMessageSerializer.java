package com.silky.starter.rabbitmq.serialization;


import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.silky.starter.rabbitmq.exception.SerializationException;

/**
 * 消息序列化器接口
 *
 * @author zy
 * @date 2025-10-12 09:38
 **/
public interface RabbitMqMessageSerializer {

    /**
     * 序列化对象为字节数组
     *
     * @param object 对象
     */
    <T> byte[] serialize(T object) throws SerializationException;

    /**
     * 序列化对象为字节数组
     *
     * @param object   待序列化的对象
     * @param features 序列化选项
     * @return 序列化后的字节数组
     */
    <T> byte[] serialize(T object, JSONWriter.Feature... features) throws SerializationException;

    /**
     * 反序列化字节数组为对象
     *
     * @param bytes 字节数组
     * @param clazz 类
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException;

    /**
     * 反序列化字节数组为对象（支持泛型）
     *
     * @param bytes         字节数组
     * @param typeReference 类型引用
     */
    <T> T deserialize(byte[] bytes, TypeReference<T> typeReference) throws SerializationException;

    /**
     * 序列化对象为字符串
     *
     * @param object 待序列化的对象
     * @return 序列化后的字符串
     */
    <T> String serializeToString(T object) throws SerializationException;

    /**
     * 反序列化字符串为对象
     *
     * @param jsonString 待反序列化的字符串
     * @param clazz      目标对象的类
     * @return 反序列化后的对象
     */
    <T> T deserializeFromString(String jsonString, Class<T> clazz) throws SerializationException;
}
