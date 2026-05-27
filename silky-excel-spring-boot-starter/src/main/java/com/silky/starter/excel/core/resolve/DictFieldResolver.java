package com.silky.starter.excel.core.resolve;

import com.silky.starter.excel.core.annotation.ExcelDict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典翻译解析器
 * 将字典编码翻译为字典文本。
 * <p>
 * 支持两种翻译模式：
 * <ol>
 *   <li>字典查询模式：通过 DictionaryProvider 按页批量查询字典文本</li>
 *   <li>表达式模式：通过 @ExcelDict(readConverterExp) 直接翻译，兼容 dictCode 表达式历史写法</li>
 * </ol>
 * <p>
 * 执行顺序：原值 -> 枚举翻译 -> 字典翻译
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public class DictFieldResolver implements BatchExcelFieldResolver<ExcelDict> {

    private final DictionaryProvider dictionaryProvider;

    /**
     * 表达式模式缓存：readConverterExp -> (code -> label)
     */
    private final Map<String, Map<String, String>> converterExpCache = new ConcurrentHashMap<>();

    public DictFieldResolver(DictionaryProvider dictionaryProvider) {
        this.dictionaryProvider = dictionaryProvider;
    }

    @Override
    public Class<ExcelDict> getAnnotationType() {
        return ExcelDict.class;
    }

    @Override
    public void prepare(List<FieldValueContext> contexts, ExcelDict excelDict) {
        String converterExp = resolveConverterExp(excelDict);
        if (dictionaryProvider == null || excelDict.dictCode().isEmpty() || !converterExp.isEmpty()) {
            return;
        }

        String dictCode = excelDict.dictCode();
        Set<String> codes = new LinkedHashSet<>();
        for (FieldValueContext context : contexts) {
            try {
                Object value = context.getField().get(context.getRow());
                if (value != null) {
                    collectCodes(codes, String.valueOf(value), excelDict.separator());
                }
            } catch (IllegalAccessException e) {
                log.warn("收集字典字段失败: field={}", context.getFieldName(), e);
            }
        }
        if (codes.isEmpty()) {
            return;
        }

        try {
            Map<String, String> result = dictionaryProvider.batchQuery(dictCode, new ArrayList<>(codes));
            contexts.get(0).getResolveContext().putDictResult(dictCode, result);
        } catch (Exception e) {
            log.warn("字典批量查询失败: dictCode={}, keys={}", dictCode, codes, e);
        }
    }

    /**
     * 字段解析
     *
     * @param context    字段解析上下文
     * @param excelDict  字典注解
     * @param fieldValue 字段值
     * @return 解析结果
     */
    @Override
    public Object resolve(FieldValueContext context, ExcelDict excelDict, Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        String codeStr = String.valueOf(fieldValue);
        String converterExp = resolveConverterExp(excelDict);

        // 优先使用表达式模式（兼容 dictCode 表达式）
        if (!converterExp.isEmpty()) {
            return resolveByConverterExp(excelDict, converterExp, codeStr);
        }

        String dictCode = excelDict.dictCode();
        if (dictCode.isEmpty() || dictionaryProvider == null) {
            return fieldValue;
        }

        String separator = excelDict.separator();
        if (codeStr.contains(separator) && !separator.isEmpty()) {
            return resolveMultiValueByContext(context, excelDict, dictCode, codeStr);
        }

        String label = context.getResolveContext().getDictLabel(dictCode, codeStr);
        if (label != null) {
            return label;
        }

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
     * @param converterExp 表达式内容
     * @param codeStr   字段值字符串
     * @return 翻译后的文本
     */
    private String resolveByConverterExp(ExcelDict excelDict, String converterExp, String codeStr) {
        String kvSeparator = resolveKvSeparator(converterExp, excelDict.kvSeparator());
        String cacheKey = buildConverterExpCacheKey(converterExp, kvSeparator);
        Map<String, String> expMap = converterExpCache.computeIfAbsent(
                cacheKey,
                key -> parseConverterExp(converterExp, kvSeparator));

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
     * 格式：code{kvSeparator}label，多个用逗号分隔。
     * 例如："0=男,1=女,2=未知" -> {0: "男", 1: "女", 2: "未知"}
     *
     * @param converterExp 表达式字符串
     * @param kvSeparator  键值分隔符
     * @return code -> label 映射
     */
    private Map<String, String> parseConverterExp(String converterExp, String kvSeparator) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = converterExp.split(",");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separatorIndex = trimmed.indexOf(kvSeparator);
            if (separatorIndex > 0 && separatorIndex + kvSeparator.length() < trimmed.length()) {
                String code = trimmed.substring(0, separatorIndex).trim();
                String label = trimmed.substring(separatorIndex + kvSeparator.length()).trim();
                map.put(code, label);
            }
        }
        return map;
    }

    /**
     * 解析当前字段最终使用的表达式来源。
     * <p>
     * 优先 readConverterExp；为空时兼容 dictCode 表达式写法。
     *
     * @param excelDict 字典注解
     * @return 可用表达式，未命中返回空字符串
     */
    private String resolveConverterExp(ExcelDict excelDict) {
        String readConverterExp = trimToEmpty(excelDict.readConverterExp());
        if (!readConverterExp.isEmpty()) {
            return readConverterExp;
        }

        String dictCode = trimToEmpty(excelDict.dictCode());
        if (dictCode.isEmpty()) {
            return "";
        }

        return isCompatibleConverterExp(dictCode, excelDict.kvSeparator()) ? dictCode : "";
    }

    /**
     * 判断 dictCode 是否符合“兼容表达式”格式。
     * <p>
     * 默认支持 "1=男,2=女"，并兼容 "1:男,2:女"。
     *
     * @param dictCode    字典编码原始值
     * @param kvSeparator 配置的键值分隔符
     * @return true 表示应按表达式处理
     */
    private boolean isCompatibleConverterExp(String dictCode, String kvSeparator) {
        if (containsValidPair(dictCode, "=")) {
            return true;
        }

        String normalizedKvSeparator = normalizeKvSeparator(kvSeparator);
        if (!"=".equals(normalizedKvSeparator) && dictCode.contains(",")
                && containsValidPair(dictCode, normalizedKvSeparator)) {
            return true;
        }

        return dictCode.contains(",") && containsValidPair(dictCode, ":");
    }

    /**
     * 基于当前表达式内容推断实际键值分隔符。
     * <p>
     * 优先使用注解配置；若不匹配，会自动回退到 "=" 或 ":" 以兼容历史写法。
     *
     * @param converterExp 表达式内容
     * @param kvSeparator  注解配置分隔符
     * @return 最终可用分隔符
     */
    private String resolveKvSeparator(String converterExp, String kvSeparator) {
        String normalizedKvSeparator = normalizeKvSeparator(kvSeparator);
        if (containsValidPair(converterExp, normalizedKvSeparator)) {
            return normalizedKvSeparator;
        }
        if (!"=".equals(normalizedKvSeparator) && containsValidPair(converterExp, "=")) {
            return "=";
        }
        if (!":".equals(normalizedKvSeparator) && containsValidPair(converterExp, ":")) {
            return ":";
        }
        return normalizedKvSeparator;
    }

    /**
     * 检查表达式中是否存在至少一个合法键值对。
     *
     * @param converterExp 表达式内容
     * @param kvSeparator  待检测分隔符
     * @return true 表示包含合法键值对
     */
    private boolean containsValidPair(String converterExp, String kvSeparator) {
        if (converterExp == null || converterExp.trim().isEmpty()) {
            return false;
        }
        if (kvSeparator == null || kvSeparator.trim().isEmpty()) {
            return false;
        }

        String[] pairs = converterExp.split(",");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separatorIndex = trimmed.indexOf(kvSeparator);
            if (separatorIndex > 0 && separatorIndex + kvSeparator.length() < trimmed.length()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 归一化键值分隔符。
     *
     * @param kvSeparator 注解配置分隔符
     * @return 非空分隔符，默认 "="
     */
    private String normalizeKvSeparator(String kvSeparator) {
        if (kvSeparator == null) {
            return "=";
        }
        String trimmed = kvSeparator.trim();
        return trimmed.isEmpty() ? "=" : trimmed;
    }

    /**
     * 生成表达式缓存键。
     *
     * @param converterExp 表达式
     * @param kvSeparator  分隔符
     * @return 缓存键
     */
    private String buildConverterExpCacheKey(String converterExp, String kvSeparator) {
        return converterExp + "\u0001" + kvSeparator;
    }

    /**
     * 将空字符串安全归一化。
     *
     * @param value 原始字符串
     * @return 去空格后的字符串，空值返回空串
     */
    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveMultiValueByContext(FieldValueContext context, ExcelDict excelDict, String dictCode, String codeStr) {
        String separator = excelDict.separator();
        String[] codes = codeStr.split(java.util.regex.Pattern.quote(separator));
        StringBuilder sb = new StringBuilder();
        for (String code : codes) {
            String trimmed = code.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(separator);
            }
            String label = context.getResolveContext().getDictLabel(dictCode, trimmed);
            sb.append(label != null ? label : handleMissStr(excelDict, trimmed));
        }
        return sb.toString();
    }

    private void collectCodes(Set<String> codes, String codeStr, String separator) {
        if (separator != null && !separator.isEmpty() && codeStr.contains(separator)) {
            String[] values = codeStr.split(java.util.regex.Pattern.quote(separator));
            for (String value : values) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    codes.add(trimmed);
                }
            }
            return;
        }
        codes.add(codeStr);
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
