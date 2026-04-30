package com.silky.starter.excel.core.resolve;

import java.util.List;
import java.util.Map;

/**
 * 字典数据提供者接口
 * 业务系统实现此接口以支持字典翻译
 * <p>
 * 使用示例：
 * <pre>
 *     Component
 *     public class DbDictionaryProvider implements DictionaryProvider {
 *         Autowired
 *         private DictService dictService;
 *
 *         Override
 *         public Map<String, String>  batchQuery(String dictCode, Se<String> codes) {
 *             // 批量查询字典，禁止逐行查库
 *             return dictService.batchGetDictLabel(dictCode, codes);
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
}
