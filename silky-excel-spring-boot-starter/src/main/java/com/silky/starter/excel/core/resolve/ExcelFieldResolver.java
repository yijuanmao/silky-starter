package com.silky.starter.excel.core.resolve;

import java.lang.annotation.Annotation;

/**
 * 字段解析器接口
 * 枚举翻译、字典翻译、脱敏都实现此接口
 * <p>
 * 解析器按照优先级顺序组成管道，对每个字段依次执行
 *
 * @author zy
 * @since 1.1.0
 */
public interface ExcelFieldResolver<A extends Annotation> {

    /**
     * 当前解析器支持的注解类型。
     */
    Class<A> getAnnotationType();

    /**
     * 对字段值进行解析/转换
     * 执行顺序：原值 -> 枚举翻译 -> 字典翻译 -> 脱敏 -> 输出
     *
     * @param context    字段解析上下文
     * @param annotation 注解
     * @param fieldValue 字段值
     * @return 转换后的值
     */
    Object resolve(FieldValueContext context, A annotation, Object fieldValue);

    /**
     * 解析器优先级，数值越小优先级越高（越先执行）
     *
     * @return 优先级
     */
    int getOrder();
}
