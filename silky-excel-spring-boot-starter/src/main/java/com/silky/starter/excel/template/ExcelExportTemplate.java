package com.silky.starter.excel.template;

import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.enums.AsyncType;

/**
 * Excel 导出模板接口
 *
 * @author zy
 * @date 2025-10-24 16:44
 **/
public interface ExcelExportTemplate {

    /**
     * 导出数据（使用默认异步方式）
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 导出结果
     */
    <T> ExportResult export(ExportRequest<T> request);

    /**
     * 导出数据（指定异步方式）
     *
     * @param request   导出请求
     * @param asyncType 异步类型
     * @param <T>       数据类型
     * @return 导出结果
     */
    <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType);

    /**
     * 同步导出数据（适合小数据量）
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 导出结果
     */
    <T> ExportResult exportSync(ExportRequest<T> request);

    /**
     * 同步导入数据（适合小数据量）
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 导出结果
     */
    <T> ImportResult importSync(ImportRequest<T> request);

    /**
     * 导出数据
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 导出结果
     */
    <T> ExportResult importAsync(ExportRequest<T> request);

    /**
     * 导出数据（指定异步方式）
     *
     * @param request   导出请求
     * @param asyncType 异步类型
     * @param <T>       数据类型
     * @return 导出结果
     */
    <T> ExportResult importAsync(ExportRequest<T> request, AsyncType asyncType);


}
