package com.silky.starter.redis.lock.annotation;

import com.silky.starter.redis.lock.enums.LockType;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * redis分布式锁注解
 *
 * @author zy
 * @date 2025-10-21 15:31
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLock {

    /**
     * 锁的key，支持SpEL表达式
     */
    String key();

    /**
     * 锁类型，默认可重入锁
     */
    LockType lockType() default LockType.REENTRANT;

    /**
     * 等待获取锁时间，默认30秒
     */
    long waitTime() default 30;

    /**
     * 锁自动释放时间，默认10秒
     */
    long leaseTime() default 10;

    /**
     * 时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 在事务提交后释放锁，默认true
     */
    boolean releaseAfterTransaction() default true;
}
