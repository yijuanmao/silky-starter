package com.silky.starter.excel.core.annotation;

import java.lang.annotation.*;

/**
 * 字典翻译注解
 * 用于将字典编码自动翻译为字典文本
 * <p>
 * 支持两种模式：
 * <ol>
 *   <li><b>字典查询模式</b>：通过 {@code dictCode} 指定字典编码，配合 {@link com.silky.starter.excel.core.resolve.DictionaryProvider} 查询字典文本</li>
 *   <li><b>表达式模式</b>：通过 {@code readConverterExp} 直接指定翻译表达式（如 "0=男,1=女,2=未知"），无需实现 DictionaryProvider</li>
 * </ol>
 * <p>
 * 兼容说明：
 * 当 {@code readConverterExp} 为空且 {@code dictCode} 形如表达式时，也会按表达式模式处理，
 * 用于兼容历史写法（如 {@code @ExcelDict(dictCode = "1=男,2=女")}）。
 * <p>
 * 使用示例：
 * <pre>
 *     // 方式一：字典查询模式（需实现 DictionaryProvider）
 *     @ExcelProperty("性别")
 *     @ExcelDict(dictCode = "sys_user_sex")
 *     private Integer gender;
 *
 *     // 方式二：表达式模式（无需 DictionaryProvider）
 *     @ExcelProperty("状态")
 *     @ExcelDict(readConverterExp = "0=禁用,1=启用,2=锁定")
 *     private Integer status;
 *
 *     // 方式三：兼容历史写法（不推荐新项目继续使用）
 *     @ExcelProperty("性别")
 *     @ExcelDict(dictCode = "1=男,2=女")
 *     private Integer gender;
 *
 *     // 方式三：多值分隔符模式
 *     @ExcelProperty("标签")
 *     @ExcelDict(readConverterExp = "1=Java,2=Python,3=Go", separator = ",")
 *     private String tags;
 *
 *     // 方式四：表达式键值分隔符为冒号
 *     @ExcelProperty("等级")
 *     @ExcelDict(readConverterExp = "1:普通,2:VIP", kvSeparator = ":")
 *     private Integer level;
 * </pre>
 *
 * @author zy
 * @since 1.1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ExcelDict {

    /**
     * 字典编码，对应 DictionaryProvider 中的字典类型
     * 需要实现 {@link com.silky.starter.excel.core.resolve.DictionaryProvider} 接口并注册为 Spring Bean。
     * <p>
     * 兼容模式下，当 readConverterExp 为空且该值形如表达式（如 "1=男,2=女"）时，
     * 会按表达式模式进行翻译。
     */
    String dictCode() default "";

    /**
     * 读取内容转表达式
     * <p>
     * 格式：code=label，多个用英文逗号分隔。
     * 示例：{@code "0=男,1=女,2=未知"}
     * <p>
     * 当设置此值时，使用表达式模式翻译，无需 DictionaryProvider。
     * 如果同时设置了 dictCode 和 readConverterExp，优先使用 readConverterExp。
     */
    String readConverterExp() default "";

    /**
     * 表达式键值分隔符
     * <p>
     * 用于解析 readConverterExp（以及兼容模式下的 dictCode 表达式）。
     * 默认使用 "="，也支持 ":" 等自定义分隔符。
     */
    String kvSeparator() default "=";

    /**
     * 分隔符，读取字符串组内容
     * <p>
     * 当字段值包含多个字典编码时，使用此分隔符拆分后逐个翻译。
     * 例如字段值为 "1,2,3"，separator 为 ","，则分别翻译 1、2、3 后用 separator 拼接。
     * <p>
     * 默认值与 Spring 的 StringUtils.SEPARATOR 一致。
     */
    String separator() default ",";

    /**
     * 翻译失败时的策略
     *
     * @see com.silky.starter.excel.core.annotation.ExcelEnum.OnMiss
     */
    ExcelEnum.OnMiss onMiss() default ExcelEnum.OnMiss.KEEP_ORIGINAL;

    /**
     * 当 onMiss = PLACEHOLDER 时使用的占位符
     */
    String placeholder() default "";
}
