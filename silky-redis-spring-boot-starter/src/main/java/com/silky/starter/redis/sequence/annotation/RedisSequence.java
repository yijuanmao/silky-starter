package com.silky.starter.redis.sequence.annotation;

import cn.hutool.core.date.DatePattern;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * redis序列号注解
 *
 * @author zy
 * @date 2025-10-23 15:19
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisSequence {

    /**
     * redis 缓存key
     */
    String redisKey() default "";

    /**
     * 业务前缀
     */
    String prefix();

    /**
     * 日期格式，默认yyyyMMdd
     */
    String datePattern() default DatePattern.PURE_DATE_PATTERN;

    /**
     * 序列号长度
     */
    int sequenceLength() default 6;

    /**
     * 随机数长度
     */
    int randomLength() default 3;

    /**
     * 过期时间（天），默认1天
     */
    int expire() default 1;

    /**
     * 过期时间单位，默认天
     */
    TimeUnit timeUnit() default TimeUnit.DAYS;
}
