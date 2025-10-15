package com.silky.starter.rabbitmq.annotation;

import com.silky.starter.rabbitmq.enums.SendMode;

import java.lang.annotation.*;

/**
 * RabbitMessage 消息标记注解
 *
 * @author zy
 * @date 2025-10-12 08:07
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RabbitMessage {

    /**
     * 交换机
     */
    String exchange();

    /**
     * 路由键
     */
    String routingKey();

    /**
     * 业务类型（优先于消息对象中的businessType）
     */
    String businessType() default "";

    /**
     * 消息描述（优先于消息对象中的description）
     */
    String description() default "";

    /**
     * 发送模式
     */
    SendMode sendMode() default SendMode.AUTO;

    /**
     * 是否异步持久化
     */
//    boolean asyncPersistence() default true;

    /**
     * 延迟时间（毫秒）
     */
    long delay() default 0L;

    /**
     * 发送失败时是否抛出异常
     */
    boolean throwOnFailure() default true;

    /**
     * 是否启用失败回调
     */
    boolean enableFailureCallback() default false;

    /**
     * 重试次数（仅当throwOnFailure为false时有效）
     */
    int retryCount() default 0;

    /**
     * 重试间隔（毫秒）
     */
    long retryInterval() default 1000L;
}
