package com.silky.starter.rabbitmq.converter;

import com.alibaba.fastjson2.TypeReference;
import com.silky.starter.rabbitmq.serialization.RabbitMqMessageSerializer;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * 自定义消息转换器
 *
 * @author: zy
 * @date: 2026-03-13
 */
public class FastJson2MessageConverter implements SmartMessageConverter {

    private final RabbitMqMessageSerializer serializer;

    public FastJson2MessageConverter(RabbitMqMessageSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        byte[] body = serializer.serialize(object);
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return new Message(body, messageProperties);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        Type type = message.getMessageProperties().getInferredArgumentType();
        return this.fromMessage(message, Objects.isNull(type) ? String.class : type);
    }

    @Override
    public Object fromMessage(Message message, Object conversionHint) throws MessageConversionException {
        // 根据转换提示的类型进行反序列化
        if (conversionHint instanceof Class) {
            Class<?> targetClass = (Class<?>) conversionHint;
            return serializer.deserialize(message.getBody(), targetClass);
        } else if (conversionHint instanceof ResolvableType) {
            ResolvableType resolvableType = (ResolvableType) conversionHint;
            Type type = resolvableType.getType();
            TypeReference<?> typeReference = new TypeReference<Object>(type) {
            };
            return serializer.deserialize(message.getBody(), typeReference);
        } else {
            // 其他情况，尝试根据转换提示的类进行反序列化（不常见）
            return serializer.deserialize(message.getBody(), conversionHint.getClass());
        }
    }
}
