package com.silky.starter.excel.core.resolve;

import com.silky.starter.excel.core.annotation.ExcelDict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * 字典翻译解析器
 * 将字典编码翻译为字典文本，支持批量查询和缓存
 * <p>
 * 执行顺序：原值 -> 枚举翻译 -> 字典翻译
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public class DictFieldResolver implements ExcelFieldResolver {

    private final DictionaryProvider dictionaryProvider;

    public DictFieldResolver(DictionaryProvider dictionaryProvider) {
        this.dictionaryProvider = dictionaryProvider;
    }

    /**
     * 字典翻译支持判断
     *
     * @param field       字段
     * @param annotation  注解
     * @return 是否支持
     */
    @Override
    public boolean supports(Field field, Annotation annotation) {
        return annotation instanceof ExcelDict && dictionaryProvider != null;
    }

    /**
     * 字段解析
     *
     * @param field       字段
     * @param annotation  注解
     * @param fieldValue  字段值
     * @param context     解析上下文
     * @return 解析结果
     */
    @Override
    public Object resolve(Field field, Annotation annotation, Object fieldValue, ResolveContext context) {
        if (fieldValue == null) {
            return null;
        }

        ExcelDict excelDict = (ExcelDict) annotation;
        String dictCode = excelDict.dictCode();
        String codeStr = String.valueOf(fieldValue);

        // 先从缓存中获取
        String label = context.getDictLabel(dictCode, codeStr);
        if (label != null) {
            return label;
        }
        // 收集待查询的字典键
        context.collectDictKey(dictCode, codeStr);

        // 执行批量查询
        try {
            Set<String> pendingKeys = context.getPendingDictKeys().get(dictCode);
            if (pendingKeys != null && !pendingKeys.isEmpty()) {
                Map<String, String> result = dictionaryProvider.batchQuery(dictCode, new ArrayList<>(pendingKeys));
                context.putDictResult(dictCode, result);
                context.clearPendingDictKeys(dictCode);

                label = result.get(codeStr);
                if (label != null) {
                    return label;
                }
            }
        } catch (Exception e) {
            log.warn("字典批量查询失败: dictCode={}, keys={}", dictCode, context.getPendingDictKeys().get(dictCode), e);
        }
        // 未命中处理
        switch (excelDict.onMiss()) {
            case BLANK:
                return "";
            case PLACEHOLDER:
                return excelDict.placeholder();
            case KEEP_ORIGINAL:
            default:
                return fieldValue;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
