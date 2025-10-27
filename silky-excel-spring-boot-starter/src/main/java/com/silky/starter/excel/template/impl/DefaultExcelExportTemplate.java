package com.silky.starter.excel.template.impl;

import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.template.ExcelExportTemplate;

/**
 * Excel 导出模板默认实现
 *
 * @author zy
 * @date 2025-10-24 16:46
 **/
public class DefaultExcelExportTemplate implements ExcelExportTemplate {

    private final ExportEngine exportEngine;

    public DefaultExcelExportTemplate(ExportEngine exportEngine) {
        this.exportEngine = exportEngine;
    }

    /**
     * 导出数据（使用默认异步方式）
     *
     * @param request 导出请求
     * @return 导出结果
     */
    @Override
    public <T> ExportResult export(ExportRequest<T> request) {
        return exportEngine.execute(request);
    }

    /**
     * 导出数据（指定异步方式）
     *
     * @param request   导出请求
     * @param asyncType 异步类型
     * @return 导出结果
     */
    @Override
    public <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType) {
        return exportEngine.execute(request, asyncType);
    }

    /**
     * 同步导出数据（适合小数据量）
     *
     * @param request 导出请求
     * @return 导出结果
     */
    @Override
    public <T> ExportResult exportSync(ExportRequest<T> request) {
        return exportEngine.exportSync(request);
    }
}
