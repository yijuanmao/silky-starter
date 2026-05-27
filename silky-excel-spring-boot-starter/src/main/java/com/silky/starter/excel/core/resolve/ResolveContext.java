package com.silky.starter.excel.core.resolve;

import java.util.HashMap;
import java.util.Map;

/**
 * 解析上下文
 * 在一次导出过程中共享页级字段解析结果。
 *
 * @author zy
 * @since 1.1.0
 */
public class ResolveContext {

    /**
     * 字典缓存：dictCode -> (code -> label)
     */
    private final Map<String, Map<String, String>> dictCache = new HashMap<>();

    /**
     * 获取字典翻译文本
     * 优先从缓存获取，缓存未命中则返回 null
     *
     * @param dictCode 字典编码
     * @param code     字典值
     * @return 字典文本，未找到返回 null
     */
    public String getDictLabel(String dictCode, String code) {
        Map<String, String> dict = dictCache.get(dictCode);
        return dict != null ? dict.get(code) : null;
    }

    /**
     * 将批量查询结果放入缓存
     *
     * @param dictCode 字典编码
     * @param result   查询结果
     */
    public void putDictResult(String dictCode, Map<String, String> result) {
        if (result != null && !result.isEmpty()) {
            dictCache.computeIfAbsent(dictCode, key -> new HashMap<>()).putAll(result);
        }
    }
}
