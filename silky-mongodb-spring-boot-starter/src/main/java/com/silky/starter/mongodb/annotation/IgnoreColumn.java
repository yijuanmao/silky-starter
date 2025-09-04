package com.silky.starter.mongodb.annotation;

import java.lang.annotation.*;

/**
 * 将带有@IgnoreColumn的字段设为null;
 *
 * @author zy
 * @date 2025-09-04 14:09
 **/
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface IgnoreColumn {
}
