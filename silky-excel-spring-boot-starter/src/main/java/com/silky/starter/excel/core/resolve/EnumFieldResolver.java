package com.silky.starter.excel.core.resolve;

import com.silky.starter.excel.core.annotation.ExcelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.util.Objects;
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
public class EnumFieldResolver implements ExcelFieldResolver<ExcelEnum> {

    private final Map<EnumMetaKey, EnumMeta> enumMetaCache = new ConcurrentHashMap<>();

    @Override
    public Class<ExcelEnum> getAnnotationType() {
        return ExcelEnum.class;
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
    public Object resolve(FieldValueContext context, ExcelEnum excelEnum, Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        Class<? extends Enum<?>> enumClass = excelEnum.enumClass();
        String codeField = excelEnum.codeField();
        String labelField = excelEnum.labelField();

        try {
            EnumMetaKey key = new EnumMetaKey(enumClass, codeField, labelField);
            EnumMeta meta = enumMetaCache.computeIfAbsent(
                    key,
                    k -> buildEnumMeta(k.enumClass, k.codeField, k.labelField)
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
            log.warn("枚举翻译失败: field={}, enumClass={}, value={}",
                    context.getFieldName(), enumClass.getSimpleName(), fieldValue, e);
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

    private static class EnumMetaKey {
        final Class<? extends Enum<?>> enumClass;
        final String codeField;
        final String labelField;

        EnumMetaKey(Class<? extends Enum<?>> enumClass, String codeField, String labelField) {
            this.enumClass = enumClass;
            this.codeField = codeField;
            this.labelField = labelField;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EnumMetaKey)) {
                return false;
            }
            EnumMetaKey that = (EnumMetaKey) o;
            return Objects.equals(enumClass, that.enumClass)
                    && Objects.equals(codeField, that.codeField)
                    && Objects.equals(labelField, that.labelField);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enumClass, codeField, labelField);
        }
    }
}
