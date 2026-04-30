package com.silky.starter.excel.core.resolve;

import com.silky.starter.excel.core.annotation.ExcelDict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典翻译解析器
 * 将字典编码翻译为字典文本，支持批量查询和缓存
 * <p>
 * 支持两种翻译模式：
 * <ol>
 *   <li>字典查询模式：通过 DictionaryProvider 查询字典文本</li>
 *   <li>表达式模式：通过 @ExcelDict(readConverterExp) 直接翻译</li>
 * </ol>
 * <p>
 * 执行顺序：原值 -> 枚举翻译 -> 字典翻译
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public class DictFieldResolver implements ExcelFieldResolver {

    private final DictionaryProvider dictionaryProvider;

    /**
     * 表达式模式缓存：readConverterExp -> (code -> label)
     */
    private final Map<String, Map<String, String>> converterExpCache = new ConcurrentHashMap<>();

    public DictFieldResolver(DictionaryProvider dictionaryProvider) {
        this.dictionaryProvider = dictionaryProvider;
    }

    /**
     * 字典翻译支持判断
     * <p>
     * 支持条件：
     * <ul>
     *   <li>注解是 ExcelDict</li>
     *   <li>有字典查询模式（dictCode 非空且有 DictionaryProvider）</li>
     *   <li>或有表达式模式（readConverterExp 非空）</li>
     * </ul>
     *
     * @param field       字段
     * @param annotation  注解
     * @return 是否支持
     */
    @Override
    public boolean supports(Field field, Annotation annotation) {
        if (!(annotation instanceof ExcelDict)) {
            return false;
        }
        ExcelDict excelDict = (ExcelDict) annotation;
        // 表达式模式始终支持
        if (!excelDict.readConverterExp().isEmpty()) {
            return true;
        }
        // 字典查询模式需要有 DictionaryProvider
        return !excelDict.dictCode().isEmpty() && dictionaryProvider != null;
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
        String codeStr = String.valueOf(fieldValue);

        // 优先使用表达式模式
        if (!excelDict.readConverterExp().isEmpty()) {
            return resolveByConverterExp(excelDict, codeStr);
        }

        // 字典查询模式
        String dictCode = excelDict.dictCode();
        if (dictCode.isEmpty()) {
            return fieldValue;
        }

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
        return handleMiss(excelDict, fieldValue);
    }

    /**
     * 通过表达式翻译
     * <p>
     * 表达式格式：code=label，多个用逗号分隔。
     * 例如："0=男,1=女,2=未知"
     * <p>
     * 支持分隔符模式：如果字段值包含分隔符，则逐个翻译后拼接。
     *
     * @param excelDict 注解
     * @param codeStr   字段值字符串
     * @return 翻译后的文本
     */
    private String resolveByConverterExp(ExcelDict excelDict, String codeStr) {
        String converterExp = excelDict.readConverterExp();
        Map<String, String> expMap = converterExpCache.computeIfAbsent(converterExp, this::parseConverterExp);

        String separator = excelDict.separator();

        // 如果字段值包含分隔符，逐个翻译
        if (codeStr.contains(separator) && !separator.isEmpty()) {
            String[] codes = codeStr.split(java.util.regex.Pattern.quote(separator));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < codes.length; i++) {
                String trimmed = codes[i].trim();
                if (!trimmed.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(separator);
                    }
                    String translated = expMap.get(trimmed);
                    sb.append(translated != null ? translated : handleMissStr(excelDict, trimmed));
                }
            }
            return sb.toString();
        }

        // 单值翻译
        String translated = expMap.get(codeStr);
        return translated != null ? translated : handleMissStr(excelDict, codeStr);
    }

    /**
     * 解析表达式字符串
     * <p>
     * 格式：code=label，多个用逗号分隔。
     * 例如："0=男,1=女,2=未知" -> {0: "男", 1: "女", 2: "未知"}
     *
     * @param converterExp 表达式字符串
     * @return code -> label 映射
     */
    private Map<String, String> parseConverterExp(String converterExp) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = converterExp.split(",");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eqIndex = trimmed.indexOf('=');
            if (eqIndex > 0) {
                String code = trimmed.substring(0, eqIndex).trim();
                String label = trimmed.substring(eqIndex + 1).trim();
                map.put(code, label);
            }
        }
        return map;
    }

    /**
     * 未命中处理（Object 返回值）
     */
    private Object handleMiss(ExcelDict excelDict, Object fieldValue) {
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

    /**
     * 未命中处理（String 返回值）
     */
    private String handleMissStr(ExcelDict excelDict, String value) {
        switch (excelDict.onMiss()) {
            case BLANK:
                return "";
            case PLACEHOLDER:
                return excelDict.placeholder();
            case KEEP_ORIGINAL:
            default:
                return value;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
