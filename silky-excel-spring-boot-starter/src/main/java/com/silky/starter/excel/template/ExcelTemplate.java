package com.silky.starter.excel.template;

import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.engine.ImportEngine;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.enums.AsyncType;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Excel 导出模板接口
 *
 * @author zy
 * @date 2025-10-24 16:44
 **/
public interface ExcelTemplate {

    /**
     * 异步导出数据（使用默认异步方式）
     *
     * @param request 导出请求
     */
    <T> ExportResult exportAsync(ExportRequest<T> request);

    /**
     * 同步导出数据（适合小数据量）
     *
     * @param request 导出请求
     */
    <T> ExportResult exportSync(ExportRequest<T> request);

    /**
     * 导出数据（指定异步类型）
     *
     * @param request   导出请求
     * @param asyncType 异步类型
     */
    <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType);

    /**
     * 导出数据（支持自定义任务配置）
     *
     * @param request        导出请求
     * @param asyncType      异步类型
     * @param taskConfigurer 任务配置器
     */
    <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType, Consumer<ExportTask<T>> taskConfigurer);

    /**
     * 异步导出（返回CompletableFuture）
     *
     * @param request 导出请求
     */
    <T> CompletableFuture<ExportResult> exportFuture(ExportRequest<T> request);

    /**
     * 大文件导出
     *
     * @param request   导出请求
     * @param batchSize 批量大小
     */
    <T> ExportResult exportLargeFile(ExportRequest<T> request, int batchSize);

    /**
     * 大文件导出（支持自定义配置）
     *
     * @param request        导出请求
     * @param batchSize      批量大小
     * @param taskConfigurer 任务配置器
     */
    <T> ExportResult exportLargeFile(ExportRequest<T> request, int batchSize, Consumer<ExportTask<T>> taskConfigurer);

    /**
     * 同步导入数据（适合小数据量）
     *
     * @param request 导入请求
     */
    <T> ImportResult importSync(ImportRequest<T> request);

    /**
     * 异步导入数据
     *
     * @param request 导入请求
     */
    <T> ImportResult importAsync(ImportRequest<T> request);

    /**
     * 导入数据（指定异步类型）
     *
     * @param request   导入请求
     * @param asyncType 异步类型
     */
    <T> ImportResult imports(ImportRequest<T> request, AsyncType asyncType);

    /**
     * 导入数据（支持自定义任务配置）
     *
     * @param request        导入请求
     * @param asyncType      异步类型
     * @param taskConfigurer 任务配置器
     */
    <T> ImportResult imports(ImportRequest<T> request, AsyncType asyncType, Consumer<ImportTask<T>> taskConfigurer);

    /**
     * 异步导入（返回CompletableFuture）
     *
     * @param request 导入请求
     */
    <T> CompletableFuture<ImportResult> importFuture(ImportRequest<T> request);

    /**
     * 大文件导入
     *
     * @param request   导入请求
     * @param batchSize 批量大小
     */
    <T> ImportResult importLargeFile(ImportRequest<T> request, int batchSize);

    /**
     * 大文件导入（支持自定义配置）
     *
     * @param request        导入请求
     * @param batchSize      批量大小
     * @param taskConfigurer 任务配置器
     */
    <T> ImportResult importLargeFile(ImportRequest<T> request, int batchSize, Consumer<ImportTask<T>> taskConfigurer);

    /**
     * 获取导出引擎状态
     */
    ExportEngine.EngineStatus getExportEngineStatus();

    /**
     * 获取导入引擎状态
     */
    ImportEngine.ImportEngineStatus getImportEngineStatus();

    /**
     * 优雅关闭
     */
    void shutdown();
}
