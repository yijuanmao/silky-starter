package com.silky.starter.excel.core.resolve;

import com.silky.starter.excel.core.annotation.ExcelDict;
import com.silky.starter.excel.core.annotation.ExcelEnum;
import com.silky.starter.excel.core.annotation.ExcelMask;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段转换管道
 * 统一调度 ExcelFieldResolver，按优先级顺序执行
 * <p>
 * 执行顺序：原值 -> 枚举翻译 -> 字典翻译 -> 脱敏 -> 输出
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public class ExcelFieldResolverPipeline {

    private final List<ExcelFieldResolver> resolvers;
    private final Map<Class<?>, List<FieldAnnotationInfo>> classFieldCache = new ConcurrentHashMap<>();

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
            for (FieldAnnotationInfo info : fieldInfos) {
                try {
                    Object fieldValue = info.field.get(item);
                    if (fieldValue == null) {
                        continue;
                    }
                    Object resolvedValue = fieldValue;
                    for (ExcelFieldResolver resolver : resolvers) {
                        if (resolver.supports(info.field, info.annotation)) {
                            resolvedValue = resolver.resolve(info.field, info.annotation, resolvedValue, context);
                            if (resolvedValue == null) {
                                break;
                            }
                        }
                    }
                    if (resolvedValue != null && !resolvedValue.equals(fieldValue)) {
                        info.field.set(item, resolvedValue);
                    }
                } catch (IllegalAccessException e) {
                    log.warn("字段访问失败: field={}", info.field.getName(), e);
                }
            }
        }

        return data;
    }

    /**
     * 解析类上带有注解的字段
     */
    private List<FieldAnnotationInfo> parseFields(Class<?> clazz) {
        List<FieldAnnotationInfo> infos = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                Annotation annotation = findResolverAnnotation(field);
                if (annotation != null) {
                    field.setAccessible(true);
                    infos.add(new FieldAnnotationInfo(field, annotation));
                }
            }
            current = current.getSuperclass();
        }
        return infos;
    }

    /**
     * 查找字段上的解析器注解
     * 如果同时存在多个注解，优先返回 ExcelEnum > ExcelDict > ExcelMask
     */
    private Annotation findResolverAnnotation(Field field) {
        ExcelEnum excelEnum = field.getAnnotation(ExcelEnum.class);
        ExcelDict excelDict = field.getAnnotation(ExcelDict.class);
        ExcelMask excelMask = field.getAnnotation(ExcelMask.class);

        // 优先级：ExcelEnum > ExcelDict > ExcelMask
        if (excelEnum != null) {
            return excelEnum;
        }
        if (excelDict != null) {
            return excelDict;
        }
        return excelMask;
    }

    /**
     * 字段注解信息
     */
    private static class FieldAnnotationInfo {
        final Field field;
        final Annotation annotation;

        FieldAnnotationInfo(Field field, Annotation annotation) {
            this.field = field;
            this.annotation = annotation;
        }
    }
}
