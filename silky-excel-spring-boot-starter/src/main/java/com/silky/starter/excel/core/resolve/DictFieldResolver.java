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
 *   <li>表达式模式：通过 @ExcelDict(readConverterExp) 直接翻译</li>
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
        if (dictionaryProvider == null || excelDict.dictCode().isEmpty() || !excelDict.readConverterExp().isEmpty()) {
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

        // 优先使用表达式模式
        if (!excelDict.readConverterExp().isEmpty()) {
            return resolveByConverterExp(excelDict, codeStr);
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
