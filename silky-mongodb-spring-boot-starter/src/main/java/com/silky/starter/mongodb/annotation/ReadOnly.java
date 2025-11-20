package com.silky.starter.mongodb.annotation;

import java.lang.annotation.*;

/**
 * 读写分离注解
 *
 * @author: zy
 * @date: 2025-11-19
 * @version: 1.0.3
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ReadOnly {

    String value() default "";
}
