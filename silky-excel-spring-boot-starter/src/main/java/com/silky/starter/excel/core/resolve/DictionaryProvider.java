package com.silky.starter.excel.core.resolve;

import java.util.List;
import java.util.Map;

/**
 * 字典数据提供者接口
 * 业务系统实现此接口以支持字典翻译
 * <p>
 * 使用示例：
 * <pre>
 *     @Component
 *     public class DbDictionaryProvider implements DictionaryProvider {
 *         @Autowired
 *         private DictService dictService;
 *
 *         @Override
 *         public Map<String, String> batchQuery(String dictCode, List<String> codes) {
 *             // 批量查询字典（推荐方式，性能更好）
 *             return dictService.batchGetDictLabel(dictCode, codes);
 *         }
 *
 *         @Override
 *         public String query(String dictCode, String code) {
 *             // 单条查询字典（可选实现，不实现则默认委托到 batchQuery）
 *             return dictService.getDictLabel(dictCode, code);
 *         }
 *     }
 * </pre>
 *
 * @author zy
 * @since 1.1.0
 */
public interface DictionaryProvider {

    /**
     * 批量查询字典数据
     * 实现方必须保证批量查询，禁止逐行逐列查库
     *
     * @param dictCode 字典编码
     * @param codes    字典值集合
     * @return 字典值 -> 字典文本的映射，不存在的值不返回
     */
    Map<String, String> batchQuery(String dictCode, List<String> codes);

    /**
     * 单条查询字典数据
     * <p>
     * 开发者可选择实现此方法，适用于无法批量查询的场景。
     * 默认实现委托到 {@link #batchQuery(String, List)}。
     * <p>
     * 框架内部会优先使用批量查询，仅在单条查询场景下调用此方法。
     *
     * @param dictCode 字典编码
     * @param code     字典值
     * @return 字典文本，未找到返回 null
     */
    default String query(String dictCode, String code) {
        Map<String, String> result = batchQuery(dictCode, java.util.Collections.singletonList(code));
        return result != null ? result.get(code) : null;
    }
}
