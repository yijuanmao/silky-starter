package com.silky.starter.excel.template.impl;

import cn.hutool.core.util.IdUtil;
import com.silky.starter.excel.core.async.executor.AsyncExecutor;
import com.silky.starter.excel.core.async.model.ProcessorStatus;
import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.engine.ImportEngine;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultExcelExportTemplate.class);

    private final AsyncExecutor asyncExecutor;

    private final ExportEngine exportEngine;

    private final ImportEngine importEngine;

    public DefaultExcelExportTemplate(AsyncExecutor asyncExecutor,
                                      ExportEngine exportEngine,
                                      ImportEngine importEngine) {
        this.asyncExecutor = asyncExecutor;
        this.exportEngine = exportEngine;
        this.importEngine = importEngine;
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
        try {
            // 直接使用ExportEngine的同步导出
            ExcelProcessResult processResult = exportEngine.exportSync(request);

            return ExportResult.success(
                    processResult.getTaskId(),
                    processResult.getFileUrl(),
                    processResult.getTotalCount(),
                    processResult.getFileSize(),
                    processResult.getCostTime());

        } catch (Exception e) {
            log.error("同步导出失败", e);
            return ExportResult.fail("SYNC_" + System.currentTimeMillis(),
                    "同步导出失败: " + e.getMessage());
        }
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
        if (AsyncType.SYNC.equals(asyncType)) {
            return exportSync(request);
        }
        // 异步处理
        try {
            request.setAsyncType(asyncType);
            ExportTask<T> exportTask = createExportTask(request);

            ExcelProcessResult result = asyncExecutor.submitExport(exportTask, asyncType);

            return ExportResult.asyncSuccess(result.getTaskId());

        } catch (Exception e) {
            log.error("异步导出失败", e);
            return ExportResult.fail("ASYNC_" + System.currentTimeMillis(),
                    "异步导出失败: " + e.getMessage());
        }
    }

    /**
     * 同步导入数据（适合小数据量）
     *
     * @param request 导出请求
     * @return 导入结果
     */
    @Override
    public <T> ImportResult importSync(ImportRequest<T> request) {
        try {
            // 使用ImportEngine的同步导入
            ExcelProcessResult processResult = importEngine.importSync(request);

            return ImportResult.success(processResult.getTaskId(),
                            processResult.getTotalCount(), processResult.getSuccessCount())
                    .withCostTime(processResult.getCostTime());

        } catch (Exception e) {
            log.error("同步导入失败", e);
            return ImportResult.fail("SYNC_IMPORT_" + System.currentTimeMillis(),
                    "同步导入失败: " + e.getMessage());
        }
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
        if (asyncType == AsyncType.SYNC) {
            return importSync(request);
        }
        // 异步处理
        try {
            ImportTask<T> importTask = createImportTask(request);
            ExcelProcessResult result = asyncExecutor.submitImport(importTask, asyncType);

            return ImportResult.asyncSuccess(result.getTaskId());

        } catch (Exception e) {
            log.error("异步导入失败", e);
            return ImportResult.fail("ASYNC_IMPORT_" + System.currentTimeMillis(),
                    "异步导入失败: " + e.getMessage());
        }
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
     * 创建导出任务
     */
    private <T> ExportTask<T> createExportTask(ExportRequest<T> request) {
        ExportTask<T> task = new ExportTask<>();
        task.setRequest(request);
        task.setTaskId(buildTaskId());
        task.setTaskType(TaskType.EXPORT);
        task.setBusinessType(request.getBusinessType());
        return task;
    }

    /**
     * 创建导入任务
     */
    private <T> ImportTask<T> createImportTask(ImportRequest<T> request) {
        ImportTask<T> task = new ImportTask<>();
        task.setRequest(request);
        task.setTaskId(buildTaskId());
        task.setTaskType(TaskType.IMPORT);
        task.setBusinessType(request.getBusinessType());
        return task;
    }

    /**
     * 生成任务ID
     */
    private String buildTaskId() {
        return IdUtil.fastSimpleUUID();
    }

}
