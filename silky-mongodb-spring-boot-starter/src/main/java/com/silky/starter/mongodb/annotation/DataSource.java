package com.silky.starter.mongodb.annotation;

import java.lang.annotation.*;

/**
 * 数据源注解
 *
 * @author: zy
 * @date: 2025-11-19
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {

    String value();

    /**
     * 是否只读操作
     */
    boolean readOnly() default false;
}
