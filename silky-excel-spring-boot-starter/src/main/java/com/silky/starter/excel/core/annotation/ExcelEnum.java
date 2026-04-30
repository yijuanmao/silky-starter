package com.silky.starter.excel.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 枚举翻译注解
 * 用于将枚举值自动翻译为显示文本
 * <p>
 * 使用示例：
 * <pre>
 *     public enum UserStatus { ACTIVE(1, "启用"), DISABLED(0, "禁用");
 *         private final int code; private final String desc;
 *         // getter...
 *     }
 *
 *     ExcelEnum(enumClass = UserStatus.class, codeField = "code", labelField = "desc")
 *     ExcelProperty("状态")
 *     private Integer status;
 * </pre>
 *
 * @author zy
 * @since 1.1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelEnum {

    /**
     * 枚举类
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * 枚举中存储值的字段名
     * 默认 "code"
     */
    String codeField() default "code";

    /**
     * 枚举中显示文本的字段名
     * 默认 "desc"
     */
    String labelField() default "desc";

    /**
     * 匹配模式：按字段值还是按枚举名称匹配
     */
    MatchMode matchMode() default MatchMode.BY_FIELD;

    /**
     * 翻译失败时的策略
     */
    OnMiss onMiss() default OnMiss.KEEP_ORIGINAL;

    /**
     * 匹配模式
     */
    enum MatchMode {

        /**
         * 按枚举字段值匹配（默认）
         */
        BY_FIELD,

        /**
         * 按枚举名称匹配
         */
        BY_NAME
    }

    /**
     * 匹配失败时的处理策略
     */
    enum OnMiss {

        /**
         * 保留原始值
         */
        KEEP_ORIGINAL,

        /**
         * 输出空字符串
         */
        BLANK,

        /**
         * 输出自定义占位符
         */
        PLACEHOLDER
    }

    /**
     * 当 onMiss = PLACEHOLDER 时使用的占位符
     */
    String placeholder() default "";
}
