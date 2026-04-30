package com.silky.starter.excel.core.annotation;

import java.lang.annotation.*;

/**
 * 字典翻译注解
 * 用于将字典编码自动翻译为字典文本，支持批量查询和缓存
 * 需要实现 {@link com.silky.starter.excel.core.resolve.DictionaryProvider} 接口并注册为 Spring Bean
 *
 * @author zy
 * @since 1.1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelDict {

    /**
     * 字典编码，对应 DictionaryProvider 中的字典类型
     */
    String dictCode();

    /**
     * 翻译失败时的策略
     */
    ExcelEnum.OnMiss onMiss() default ExcelEnum.OnMiss.KEEP_ORIGINAL;

    /**
     * 当 onMiss = PLACEHOLDER 时使用的占位符
     */
    String placeholder() default "";
}
