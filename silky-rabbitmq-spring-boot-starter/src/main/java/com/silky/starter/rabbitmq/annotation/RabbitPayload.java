package com.silky.starter.rabbitmq.annotation;

import java.lang.annotation.*;

/**
 * RabbitPayload 消息体参数注解
 *
 * @author: zy
 * @date: 2026-03-12
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RabbitPayload {
}
