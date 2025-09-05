package com.silky.starter.mongodb.annotation;

import java.lang.annotation.*;

/**
 * 设置默认值
 *
 * @author zy
 * @date 2025-09-04 17:31
 **/
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface InitValue {

    /**
     * 默认值
     */
    String value() default "";
}
