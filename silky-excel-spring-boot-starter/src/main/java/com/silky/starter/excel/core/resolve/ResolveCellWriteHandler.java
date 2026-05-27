package com.silky.starter.excel.core.resolve;

import cn.idev.excel.metadata.Head;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.write.handler.CellWriteHandler;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteTableHolder;
import org.apache.poi.ss.usermodel.Cell;

import java.util.List;

/**
 * 解析值单元格写入处理器
 *
 * @author zy
 * @since 1.1.0
 */
public class ResolveCellWriteHandler implements CellWriteHandler {

    /**
     * 当前页字段解析结果。
     */
    private ResolvedPage resolvedPage = new ResolvedPage();

    /**
     * 设置当前页数据的解析值
     * 在每页数据写入前调用
     *
     * @param resolvedPage 当前页解析结果
     */
    public void setResolvedPage(ResolvedPage resolvedPage) {
        this.resolvedPage = resolvedPage == null ? new ResolvedPage() : resolvedPage;
    }

    /**
     * 值单元格写入后处理
     *
     * @param writeSheetHolder 写入sheet信息
     * @param writeTableHolder 写入的表格信息
     * @param cellDataList     单元格数据列表
     * @param cell             单元格
     * @param head             头信息
     * @param relativeRowIndex 相对于当前行的索引
     * @param isHead           是否是表头行
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
        if (relativeRowIndex == null || resolvedPage.isEmpty()) {
            return;
        }

        // 通过 Head 获取字段名
        if (head == null || head.getFieldName() == null) {
            return;
        }

        Object resolvedValue = resolvedPage.getValue(relativeRowIndex, head.getFieldName());
        if (resolvedValue != null) {
            // 用解析值覆盖单元格（转为 String 写入）
            cell.setCellValue(String.valueOf(resolvedValue));
        }
    }
}
