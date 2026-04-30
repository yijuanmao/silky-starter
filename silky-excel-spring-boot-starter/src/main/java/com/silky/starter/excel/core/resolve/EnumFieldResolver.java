package com.silky.starter.excel.core.resolve;

import com.silky.starter.excel.core.annotation.ExcelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举翻译解析器
 * 将枚举值翻译为显示文本
 * <p>
 * 执行顺序：原值 -> 枚举翻译（优先级最高）
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public class EnumFieldResolver implements ExcelFieldResolver {

    private final Map<String, EnumMeta> enumMetaCache = new ConcurrentHashMap<>();

    /**
     * 枚举翻译支持判断
     *
     * @param field       字段
     * @param annotation  注解
     * @return 是否支持
     */
    @Override
    public boolean supports(Field field, java.lang.annotation.Annotation annotation) {
        return annotation instanceof ExcelEnum;
    }

    /**
     * 枚举翻译解析
     *
     * @param field       字段
     * @param annotation  注解
     * @param fieldValue  字段值
     * @param context     解析上下文
     * @return 解析结果
     */
    @Override
    public Object resolve(Field field, java.lang.annotation.Annotation annotation, Object fieldValue, ResolveContext context) {
        if (fieldValue == null) {
            return null;
        }

        ExcelEnum excelEnum = (ExcelEnum) annotation;
        Class<? extends Enum<?>> enumClass = excelEnum.enumClass();
        String codeField = excelEnum.codeField();
        String labelField = excelEnum.labelField();

        try {
            EnumMeta meta = enumMetaCache.computeIfAbsent(
                    enumClass.getName(),
                    k -> buildEnumMeta(enumClass, codeField, labelField)
            );

            String codeStr = String.valueOf(fieldValue);
            String label = meta.codeToLabel.get(codeStr);

            if (label == null) {
                switch (excelEnum.onMiss()) {
                    case BLANK:
                        return "";
                    case PLACEHOLDER:
                        return excelEnum.placeholder();
                    case KEEP_ORIGINAL:
                    default:
                        return fieldValue;
                }
            }

            return label;
        } catch (Exception e) {
            log.warn("枚举翻译失败: field={}, enumClass={}, value={}", field.getName(), enumClass.getSimpleName(), fieldValue, e);
            return fieldValue;
        }
    }

    /**
     * 枚举翻译解析器优先级最高
     *
     * @return 优先级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 构建枚举元数据
     *
     * @param enumClass   枚举类
     * @param codeField   枚举值字段
     * @param labelField  枚举文本字段
     * @return 枚举元数据
     */
    private EnumMeta buildEnumMeta(Class<? extends Enum<?>> enumClass, String codeField, String labelField) {
        EnumMeta meta = new EnumMeta();
        Method codeGetter = findGetter(enumClass, codeField);
        Method labelGetter = findGetter(enumClass, labelField);

        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
            try {
                Object codeValue = codeGetter.invoke(enumConstant);
                Object labelValue = labelGetter.invoke(enumConstant);
                meta.codeToLabel.put(String.valueOf(codeValue), String.valueOf(labelValue));
            } catch (Exception e) {
                log.warn("解析枚举值失败: {}, field={}", enumClass.getSimpleName(), codeField, e);
            }
        }
        return meta;
    }

    private Method findGetter(Class<?> clazz, String fieldName) {
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            return clazz.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            // 尝试 is 前缀（boolean）
            if (boolean.class.isAssignableFrom(clazz) || Boolean.class.isAssignableFrom(clazz)) {
                String isGetter = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                try {
                    return clazz.getMethod(isGetter);
                } catch (NoSuchMethodException ignored) {
                }
            }
            throw new IllegalArgumentException("无法找到字段 " + fieldName + " 的 getter 方法: " + getterName);
        }
    }

    private static class EnumMeta {
        final Map<String, String> codeToLabel = new ConcurrentHashMap<>();
    }
}
