package com.silky.starter.excel.core.annotation;

import java.lang.annotation.*;

/**
 * 数据脱敏注解
 * 用于导出时对敏感字段进行脱敏处理
 * <p>
 * 使用示例：
 * <pre>
 *     ExcelMask(strategy = MaskStrategy.PHONE)
 *     ExcelProperty("手机号")
 *     private String phone;
 *
 *     ExcelMask(strategy = MaskStrategy.CUSTOM, customPattern = "(\\d{3})\\d{4}(\\d{4})")
 *     ExcelProperty("身份证号")
 *     private String idCard;
 * </pre>
 *
 * @author zy
 * @since 1.1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelMask {

    /**
     * 脱敏策略
     */
    MaskStrategy strategy() default MaskStrategy.CUSTOM;

    /**
     * 自定义正则表达式（当 strategy = CUSTOM 时使用）
     * 分组1和最后一个分组保留，中间替换为 maskChar
     */
    String customPattern() default "";

    /**
     * 替换字符
     */
    char maskChar() default '*';

    /**
     * 脱敏策略枚举
     */
    enum MaskStrategy {

        /**
         * 手机号脱敏：138****1234
         */
        PHONE,

        /**
         * 身份证号脱敏：110***********1234
         */
        ID_CARD,

        /**
         * 邮箱脱敏：ab****@example.com
         */
        EMAIL,

        /**
         * 姓名脱敏：张**
         */
        NAME,

        /**
         * 银行卡脱敏：6222 **** **** 1234
         */
        BANK_CARD,

        /**
         * 自定义脱敏，使用 customPattern 正则
         */
        CUSTOM,

        /**
         * 全部脱敏，保留前 keepPrefix 位和后 keepSuffix 位
         */
        PARTIAL
    }

    /**
     * 当 strategy = PARTIAL 时，保留前 N 位
     */
    int keepPrefix() default 1;

    /**
     * 当 strategy = PARTIAL 时，保留后 N 位
     */
    int keepSuffix() default 1;
}
