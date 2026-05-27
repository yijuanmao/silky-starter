package com.silky.starter.excel.template;

import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.entity.ImportRecord;

/**
 * Excel 导入导出门面接口。
 *
 * @author zy
 * @date 2025-10-24 16:44
 **/
public interface ExcelTemplate {

    /**
     * 同步导出数据。
     */
    <T> ExportResult exportSync(ExportRequest<T> request);

    /**
     * 异步导出数据。
     */
    <T> ExportResult exportAsync(ExportRequest<T> request);

    /**
     * 同步导入数据。
     */
    <T> ImportResult importSync(ImportRequest<T> request);

    /**
     * 异步导入数据。
     */
    <T> ImportResult importAsync(ImportRequest<T> request);

    /**
     * 查询导出任务记录。
     */
    ExportRecord getExportRecord(String taskId);

    /**
     * 查询导入任务记录。
     */
    ImportRecord getImportRecord(String taskId);
}
