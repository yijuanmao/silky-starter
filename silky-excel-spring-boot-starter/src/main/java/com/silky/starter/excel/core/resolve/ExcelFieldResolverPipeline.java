package com.silky.starter.excel.core.resolve;

import com.silky.starter.excel.core.annotation.ExcelDict;
import com.silky.starter.excel.core.annotation.ExcelEnum;
import com.silky.starter.excel.core.annotation.ExcelMask;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段转换管道
 * 统一调度 ExcelFieldResolver，按优先级顺序执行
 * <p>
 * 执行顺序：原值 -> 枚举翻译 -> 字典翻译 -> 脱敏 -> 输出
 * <p>
 * 支持组合注解：同一字段可同时标注多个解析注解（如 @ExcelEnum + @ExcelMask），
 * 管道会按优先级依次执行所有匹配的解析器。
 * <p>
 * 对于类型兼容的字段（如 String），直接修改原字段值；
 * 对于类型不兼容的字段（如 Integer -> String），将转换值存储到 resolvedValueStore 中，
 * 由 {@link ResolveCellWriteHandler} 在 Excel 单元格写入时替换。
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public class ExcelFieldResolverPipeline {

    /**
     * 解析器列表（按优先级排序）
     */
    private final List<ExcelFieldResolver> resolvers;
    /**
     * 类字段注解缓存
     */
    private final Map<Class<?>, List<FieldAnnotationInfo>> classFieldCache = new ConcurrentHashMap<>();

    /**
     * 解析值旁路存储
     * Key: 数据对象（IdentityHashMap 保证同一引用）
     * Value: 字段名 -> 解析后的值（用于类型不兼容的场景）
     */
    private final Map<Object, Map<String, Object>> resolvedValueStore = new IdentityHashMap<>();

    /**
     * 构造函数
     *
     * @param resolvers 解析器列表
     */
    public ExcelFieldResolverPipeline(List<ExcelFieldResolver> resolvers) {
        this.resolvers = new ArrayList<>(resolvers);
        this.resolvers.sort(Comparator.comparingInt(ExcelFieldResolver::getOrder));
        log.info("ExcelFieldResolverPipeline 初始化完成, 解析器数量: {}", this.resolvers.size());
    }

    /**
     * 对数据对象进行字段转换处理
     * <p>
     * 支持组合注解：同一字段的多个注解会依次经过匹配的解析器处理。
     *
     * @param data    数据对象列表
     * @param clazz   数据类
     * @param context 解析上下文
     * @param <T>     数据类型
     * @return 处理后的数据列表（原列表修改）
     */
    public <T> List<T> resolve(List<T> data, Class<T> clazz, ResolveContext context) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        List<FieldAnnotationInfo> fieldInfos = classFieldCache.computeIfAbsent(clazz, this::parseFields);
        if (fieldInfos.isEmpty()) {
            return data;
        }

        for (T item : data) {
            if (item == null) {
                continue;
            }
            Map<String, Object> itemResolvedValues = null;
            for (FieldAnnotationInfo info : fieldInfos) {
                try {
                    Object fieldValue = info.field.get(item);
                    if (fieldValue == null) {
                        continue;
                    }
                    Object resolvedValue = fieldValue;
                    // 遍历所有解析器，支持组合注解
                    for (ExcelFieldResolver resolver : resolvers) {
                        if (resolver.supports(info.field, info.annotation)) {
                            resolvedValue = resolver.resolve(info.field, info.annotation, resolvedValue, context);
                            if (resolvedValue == null) {
                                break;
                            }
                        }
                    }
                    if (resolvedValue != null && !resolvedValue.equals(fieldValue)) {
                        // 类型兼容：直接 set
                        if (isTypeCompatible(info.field, resolvedValue)) {
                            info.field.set(item, resolvedValue);
                        } else {
                            // 类型不兼容：存入旁路存储
                            if (itemResolvedValues == null) {
                                itemResolvedValues = new ConcurrentHashMap<>();
                                resolvedValueStore.put(item, itemResolvedValues);
                            }
                            itemResolvedValues.put(info.field.getName(), resolvedValue);
                        }
                    }
                } catch (IllegalAccessException e) {
                    log.warn("字段访问失败: field={}", info.field.getName(), e);
                }
            }
        }

        return data;
    }

    /**
     * 获取对象的解析值存储
     *
     * @param item 数据对象
     * @return 字段名 -> 解析值映射
     */
    public Map<String, Object> getResolvedValues(Object item) {
        return resolvedValueStore.get(item);
    }

    /**
     * 获取指定对象的指定字段的解析值
     *
     * @param item      数据对象
     * @param fieldName 字段名
     * @return 解析后的值
     */
    public Object getResolvedValue(Object item, String fieldName) {
        Map<String, Object> values = resolvedValueStore.get(item);
        return values != null ? values.get(fieldName) : null;
    }

    /**
     * 判断解析值是否可以安全地 set 到字段上
     */
    private boolean isTypeCompatible(Field field, Object resolvedValue) {
        if (resolvedValue == null) {
            return false;
        }
        Class<?> fieldType = field.getType();
        if (fieldType == Object.class) {
            return true;
        }
        return fieldType.isInstance(resolvedValue);
    }

    /**
     * 清理指定对象的解析值存储（防止内存泄漏）
     *
     * @param items 数据对象列表
     */
    public void clearResolvedValues(List<?> items) {
        for (Object item : items) {
            resolvedValueStore.remove(item);
        }
    }

    /**
     * 清理所有解析值存储
     */
    public void clearAllResolvedValues() {
        resolvedValueStore.clear();
    }

    /**
     * 解析类上带有注解的字段
     * 支持组合注解：一个字段可返回多个 FieldAnnotationInfo
     */
    private List<FieldAnnotationInfo> parseFields(Class<?> clazz) {
        List<FieldAnnotationInfo> infos = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                // 收集字段上所有解析器注解
                List<Annotation> annotations = findResolverAnnotations(field);
                for (Annotation annotation : annotations) {
                    field.setAccessible(true);
                    infos.add(new FieldAnnotationInfo(field, annotation));
                }
            }
            current = current.getSuperclass();
        }
        return infos;
    }

    /**
     * 查找字段上的所有解析器注解（支持组合注解）
     * 按优先级排序：ExcelEnum > ExcelDict > ExcelMask
     */
    private List<Annotation> findResolverAnnotations(Field field) {
        List<Annotation> annotations = new ArrayList<>();

        ExcelEnum excelEnum = field.getAnnotation(ExcelEnum.class);
        if (excelEnum != null) {
            annotations.add(excelEnum);
        }
        ExcelDict excelDict = field.getAnnotation(ExcelDict.class);
        if (excelDict != null) {
            annotations.add(excelDict);
        }
        ExcelMask excelMask = field.getAnnotation(ExcelMask.class);
        if (excelMask != null) {
            annotations.add(excelMask);
        }

        return annotations;
    }

    /**
     * 字段注解信息
     */
    static class FieldAnnotationInfo {
        final Field field;
        final Annotation annotation;

        FieldAnnotationInfo(Field field, Annotation annotation) {
            this.field = field;
            this.annotation = annotation;
        }
    }
}
