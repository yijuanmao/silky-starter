package com.silky.starter.excel.core.resolve;

import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 解析上下文
 * 在一次导出过程中共享，用于批量收集字典查询键值
 *
 * @author zy
 * @since 1.1.0
 */
public class ResolveContext {

    /**
     * 字典缓存：dictCode -> (code -> label)
     */
    @Getter
    private final Map<String, Map<String, String>> dictCache = new HashMap<>();

    /**
     * 待查询的字典键收集：dictCode -> Set<code>
     */
    @Getter
    private final Map<String, Set<String>> pendingDictKeys = new HashMap<>();

    /**
     * 枚举缓存：enumClassName -> (code -> label)
     */
    @Getter
    private final Map<String, Map<String, String>> enumCache = new HashMap<>();

    /**
     * 收集待查询的字典键
     *
     * @param dictCode 字典编码
     * @param code     字典值
     */
    public void collectDictKey(String dictCode, String code) {
        pendingDictKeys.computeIfAbsent(dictCode, k -> new HashSet<>()).add(code);
    }

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
            dictCache.put(dictCode, result);
        }
    }

    /**
     * 清空待查询的字典键（批量查询后调用）
     *
     * @param dictCode 字典编码
     */
    public void clearPendingDictKeys(String dictCode) {
        pendingDictKeys.remove(dictCode);
    }
}
