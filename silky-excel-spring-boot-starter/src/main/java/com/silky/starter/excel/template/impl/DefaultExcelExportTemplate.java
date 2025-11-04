package com.silky.starter.excel.template.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.async.executor.AsyncExecutor;
import com.silky.starter.excel.core.async.model.ProcessorStatus;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.TaskType;
import com.silky.starter.excel.template.ExcelExportTemplate;

/**
 * Excel 导出模板默认实现
 *
 * @author zy
 * @date 2025-10-24 16:46
 **/
public class DefaultExcelExportTemplate implements ExcelExportTemplate {

    private final AsyncExecutor asyncExecutor;

    public DefaultExcelExportTemplate(AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * 导出数据（使用默认异步方式）
     *
     * @param request 导出请求
     * @return 导出结果
     */
    @Override
    public <T> ExportResult exportAsync(ExportRequest<T> request) {
        return this.export(request, AsyncType.ASYNC);
    }

    /**
     * 同步导出数据（适合小数据量）
     *
     * @param request 导出请求
     * @return 导出结果
     */
    @Override
    public <T> ExportResult exportSync(ExportRequest<T> request) {
        return this.export(request, AsyncType.SYNC);
    }

    /**
     * 导出数据，制定异步方式类型
     *
     * @param request   导出请求
     * @param asyncType 异步类型
     * @return 导出结果
     */
    @Override
    public <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType) {
        request.setAsyncType(asyncType);
        ExportTask<T> exportTask = new ExportTask<>();
        exportTask.setRequest(request);
        exportTask.setTaskId(buildTaskId());
        exportTask.setTaskType(TaskType.EXPORT);
        exportTask.setBusinessType(StrUtil.isBlank(request.getBusinessType()) ? "" : request.getBusinessType());

        ExcelProcessResult result = asyncExecutor.submitExport(exportTask, asyncType);
        return ExportResult.success(result.getTaskId());
    }

    /**
     * 同步导入数据（适合小数据量）
     *
     * @param request 导出请求
     * @return 导入结果
     */
    @Override
    public <T> ImportResult importSync(ImportRequest<T> request) {
        return this.imports(request, AsyncType.SYNC);
    }

    /**
     * 导入数据,异步方法
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 导入结果
     */
    @Override
    public <T> ImportResult importAsync(ImportRequest<T> request) {
        return this.imports(request, AsyncType.ASYNC);
    }

    /**
     * 导入数据（指定异步方式）
     *
     * @param request   导出请求
     * @param asyncType 异步类型
     * @return 导入结果
     */
    @Override
    public <T> ImportResult imports(ImportRequest<T> request, AsyncType asyncType) {
        ImportTask<T> importTask = new ImportTask<>();
        importTask.setRequest(request);
        importTask.setTaskId(buildTaskId());
        importTask.setTaskType(TaskType.IMPORT);
        importTask.setBusinessType(request.getBusinessType());

        ExcelProcessResult result = asyncExecutor.submitImport(importTask, asyncType);
        return ImportResult.success(result.getTaskId(), result.getTotalCount(), result.getSuccessCount());
    }

    /**
     * 获取处理器状态
     *
     * @param asyncType 处理器类型
     * @return 处理器状态
     */
    @Override
    public ProcessorStatus getProcessorStatus(String asyncType) {
        return asyncExecutor.getProcessorStatus(asyncType);
    }

    /**
     * 生成任务ID
     *
     * @return 任务ID
     */
    private String buildTaskId() {
        return IdUtil.fastSimpleUUID();
    }

}
