package com.silky.starter.excel.core.resolve;

import cn.idev.excel.metadata.Head;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.write.handler.CellWriteHandler;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteTableHolder;
import org.apache.poi.ss.usermodel.Cell;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析值单元格写入处理器
 *
 * @author zy
 * @since 1.1.0
 */
public class ResolveCellWriteHandler implements CellWriteHandler {

    /**
     * 当前页数据引用 -> 行索引的映射
     * 使用 IdentityHashMap 保证对象引用一致性
     */
    private final Map<Object, Integer> dataRowMap = new IdentityHashMap<>();

    /**
     * 行索引 -> (字段名 -> 解析值) 的映射
     */
    private Map<Integer, Map<String, Object>> rowIndexResolvedValues = Collections.emptyMap();

    /**
     * 设置当前页数据的解析值
     * 在每页数据写入前调用
     *
     * @param pageData              当前页数据列表
     * @param fieldResolverPipeline 字段转换管道
     */
    public void setCurrentPageData(List<?> pageData, ExcelFieldResolverPipeline fieldResolverPipeline) {
        dataRowMap.clear();
        if (fieldResolverPipeline == null || pageData == null || pageData.isEmpty()) {
            rowIndexResolvedValues = Collections.emptyMap();
            return;
        }
        IdentityHashMap<Integer, Map<String, Object>> resolvedMap = new IdentityHashMap<>();
        for (int i = 0; i < pageData.size(); i++) {
            Object item = pageData.get(i);
            dataRowMap.put(item, i);
            Map<String, Object> resolved = fieldResolverPipeline.getResolvedValues(item);
            if (resolved != null && !resolved.isEmpty()) {
                resolvedMap.put(i, resolved);
            }
        }
        rowIndexResolvedValues = resolvedMap;
    }

    /**
     * 值单元格写入后处理
     *
     * @param writeSheetHolder       写入sheet信息
     * @param writeTableHolder       写入的表格信息
     * @param cellDataList           单元格数据列表
     * @param cell                   单元格
     * @param head                   头信息
     * @param relativeRowIndex       相对于当前行的索引
     * @param isHead                 是否是表头行
     */
    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder,
                                 WriteTableHolder writeTableHolder,
                                 List<WriteCellData<?>> cellDataList,
                                 Cell cell,
                                 Head head,
                                 Integer relativeRowIndex,
                                 Boolean isHead) {
        // 跳过表头行
        if (Boolean.TRUE.equals(isHead)) {
            return;
        }
        if (relativeRowIndex == null || rowIndexResolvedValues.isEmpty()) {
            return;
        }

        Map<String, Object> resolvedValues = rowIndexResolvedValues.get(relativeRowIndex);
        if (resolvedValues == null || resolvedValues.isEmpty()) {
            return;
        }

        // 通过 Head 获取字段名
        if (head == null || head.getFieldName() == null) {
            return;
        }

        Object resolvedValue = resolvedValues.get(head.getFieldName());
        if (resolvedValue != null) {
            // 用解析值覆盖单元格（转为 String 写入）
            cell.setCellValue(String.valueOf(resolvedValue));
        }
    }
}
