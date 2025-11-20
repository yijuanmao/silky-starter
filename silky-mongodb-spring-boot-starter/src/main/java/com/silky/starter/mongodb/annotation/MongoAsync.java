package com.silky.starter.mongodb.annotation;

import java.lang.annotation.*;

/**
 * 异步操作注解
 *
 * @author: zy
 * @date: 2025-11-19
 * @version: 1.0.3
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MongoAsync {
}
