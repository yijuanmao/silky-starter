package com.silky.starter.excel.core.resolve;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel 字段值解析管道。
 * <p>
 * 管道只生成导出展示值，不修改原始 DTO，也不保存任务级临时状态。
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public class ExcelFieldResolverPipeline {

    private final List<ExcelFieldResolver<?>> resolvers;

    private final Map<Class<? extends Annotation>, ExcelFieldResolver<?>> resolverMap = new HashMap<>();

    private final Map<Class<?>, ExcelResolveMetadata> metadataCache = new ConcurrentHashMap<>();

    public ExcelFieldResolverPipeline(List<ExcelFieldResolver<?>> resolvers) {
        this.resolvers = new ArrayList<>(resolvers);
        this.resolvers.sort(Comparator.comparingInt(ExcelFieldResolver::getOrder));
        for (ExcelFieldResolver<?> resolver : this.resolvers) {
            resolverMap.put(resolver.getAnnotationType(), resolver);
        }
        log.info("ExcelFieldResolverPipeline 初始化完成, 解析器数量: {}", this.resolvers.size());
    }

    public <T> ResolvedPage resolve(List<T> rows, Class<T> rowType, ResolveContext resolveContext) {
        ResolvedPage resolvedPage = new ResolvedPage();
        if (rows == null || rows.isEmpty()) {
            return resolvedPage;
        }

        ExcelResolveMetadata metadata = metadataCache.computeIfAbsent(rowType, this::parseMetadata);
        if (metadata.isEmpty()) {
            return resolvedPage;
        }

        prepareBatchResolvers(rows, metadata, resolveContext);

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            T row = rows.get(rowIndex);
            if (row == null) {
                continue;
            }
            resolveRow(row, rowIndex, metadata, resolveContext, resolvedPage);
        }

        return resolvedPage;
    }

    private <T> void prepareBatchResolvers(List<T> rows, ExcelResolveMetadata metadata, ResolveContext resolveContext) {
        for (FieldResolveMetadata fieldMetadata : metadata.getFields()) {
            Field field = fieldMetadata.getField();
            for (Annotation annotation : fieldMetadata.getAnnotations()) {
                ExcelFieldResolver<?> resolver = resolverMap.get(annotation.annotationType());
                if (!(resolver instanceof BatchExcelFieldResolver)) {
                    continue;
                }
                List<FieldValueContext> contexts = new ArrayList<>();
                for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                    T row = rows.get(rowIndex);
                    if (row != null) {
                        contexts.add(new FieldValueContext(row, rowIndex, field, resolveContext));
                    }
                }
                prepareResolver((BatchExcelFieldResolver<?>) resolver, contexts, annotation);
            }
        }
    }

    private void resolveRow(Object row,
                            int rowIndex,
                            ExcelResolveMetadata metadata,
                            ResolveContext resolveContext,
                            ResolvedPage resolvedPage) {
        for (FieldResolveMetadata fieldMetadata : metadata.getFields()) {
            Field field = fieldMetadata.getField();
            try {
                Object originalValue = field.get(row);
                if (originalValue == null) {
                    continue;
                }

                Object resolvedValue = originalValue;
                FieldValueContext fieldContext = new FieldValueContext(row, rowIndex, field, resolveContext);
                for (Annotation annotation : fieldMetadata.getAnnotations()) {
                    ExcelFieldResolver<?> resolver = resolverMap.get(annotation.annotationType());
                    if (resolver == null) {
                        continue;
                    }
                    resolvedValue = resolveWithResolver(resolver, fieldContext, annotation, resolvedValue);
                    if (resolvedValue == null) {
                        break;
                    }
                }

                if (resolvedValue != null && !resolvedValue.equals(originalValue)) {
                    resolvedPage.putValue(rowIndex, fieldMetadata.getFieldName(), resolvedValue);
                }
            } catch (IllegalAccessException e) {
                log.warn("字段访问失败: field={}", field.getName(), e);
            }
        }
    }

    private ExcelResolveMetadata parseMetadata(Class<?> rowType) {
        List<FieldResolveMetadata> fields = new ArrayList<>();
        Class<?> current = rowType;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                List<Annotation> annotations = findResolverAnnotations(field);
                if (!annotations.isEmpty()) {
                    field.setAccessible(true);
                    fields.add(new FieldResolveMetadata(field, annotations));
                }
            }
            current = current.getSuperclass();
        }
        return new ExcelResolveMetadata(rowType, fields);
    }

    private List<Annotation> findResolverAnnotations(Field field) {
        List<Annotation> annotations = new ArrayList<>();
        for (ExcelFieldResolver<?> resolver : resolvers) {
            Annotation annotation = field.getAnnotation(resolver.getAnnotationType());
            if (annotation != null) {
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object resolveWithResolver(ExcelFieldResolver resolver,
                                       FieldValueContext context,
                                       Annotation annotation,
                                       Object value) {
        return resolver.resolve(context, annotation, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void prepareResolver(BatchExcelFieldResolver resolver,
                                 List<FieldValueContext> contexts,
                                 Annotation annotation) {
        resolver.prepare(contexts, annotation);
    }
}
