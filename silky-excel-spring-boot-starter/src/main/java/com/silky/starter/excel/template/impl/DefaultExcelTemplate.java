package com.silky.starter.excel.template.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.engine.EnhancedWriterWrapper;
import com.silky.starter.excel.core.engine.ExcelReaderWrapper;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.AnalysisListenersContext;
import com.silky.starter.excel.core.model.export.*;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.core.resolve.ExcelFieldResolverPipeline;
import com.silky.starter.excel.core.resolve.ResolvedPage;
import com.silky.starter.excel.core.resolve.ResolveCellWriteHandler;
import com.silky.starter.excel.core.resolve.ResolveContext;
import com.silky.starter.excel.core.storage.StorageObject;
import com.silky.starter.excel.core.storage.factory.StorageStrategyFactory;
import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.entity.ImportRecord;
import com.silky.starter.excel.enums.ExportStatus;
import com.silky.starter.excel.enums.ImportStatus;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.service.export.ExportRecordService;
import com.silky.starter.excel.service.imports.ImportRecordService;
import com.silky.starter.excel.template.ExcelTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ExcelTemplate 默认实现。
 *
 * @author zy
 * @since 1.0.0
 */
@Slf4j
public class DefaultExcelTemplate implements ExcelTemplate {

    private static final String TEMP_EXPORT_PREFIX = "silky_export_";
    private static final long EXCEL_MAX_ROWS_PER_SHEET = 1048576;

    private final StorageStrategyFactory storageStrategyFactory;
    private final ExportRecordService exportRecordService;
    private final ImportRecordService importRecordService;
    private final SilkyExcelProperties properties;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ExcelFieldResolverPipeline fieldResolverPipeline;

    public DefaultExcelTemplate(StorageStrategyFactory storageStrategyFactory,
                                ExportRecordService exportRecordService,
                                ImportRecordService importRecordService,
                                SilkyExcelProperties properties,
                                ThreadPoolTaskExecutor taskExecutor,
                                ExcelFieldResolverPipeline fieldResolverPipeline) {
        this.storageStrategyFactory = storageStrategyFactory;
        this.exportRecordService = exportRecordService;
        this.importRecordService = importRecordService;
        this.properties = properties;
        this.taskExecutor = taskExecutor;
        this.fieldResolverPipeline = fieldResolverPipeline;
    }

    /**
     * Excel 导出。
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 导出结果
     */
    @Override
    public <T> ExportResult exportSync(ExportRequest<T> request) {
        ExportTask<T> task = createExportTask(request, false);
        return doExport(task);
    }

    /**
     * Excel 异步导出。
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 导出结果
     */
    @Override
    public <T> ExportResult exportAsync(ExportRequest<T> request) {
        ExportTask<T> task = createExportTask(request, true);
        createExportRecord(task);
        try {
            taskExecutor.execute(() -> doExport(task));
            return ExportResult.asyncSuccess(task.getTaskId());
        } catch (Exception e) {
            exportRecordService.markFailed(task.getTaskId(), "异步提交失败: " + e.getMessage());
            return ExportResult.fail(task.getTaskId(), "异步提交失败: " + e.getMessage());
        }
    }

    /**
     * Excel 导入。
     *
     * @param request 导入请求
     * @param <T>     数据类型
     * @return 导入结果
     */
    @Override
    public <T> ImportResult importSync(ImportRequest<T> request) {
        ImportTask<T> task = createImportTask(request, false);
        return doImport(task);
    }

    /**
     * Excel 异步导入。
     *
     * @param request 导入请求
     * @param <T>     数据类型
     * @return 导入结果
     */
    @Override
    public <T> ImportResult importAsync(ImportRequest<T> request) {
        ImportTask<T> task = createImportTask(request, true);
        createImportRecord(task);
        try {
            taskExecutor.execute(() -> doImport(task));
            return ImportResult.asyncSuccess(task.getTaskId());
        } catch (Exception e) {
            importRecordService.markFailed(task.getTaskId(), "异步提交失败: " + e.getMessage());
            return ImportResult.fail(task.getTaskId(), "异步提交失败: " + e.getMessage());
        }
    }

    /**
     * 获取导出记录。
     *
     * @param taskId 任务ID
     * @return 导出记录
     */
    @Override
    public ExportRecord getExportRecord(String taskId) {
        return exportRecordService.getByTaskId(taskId);
    }

    /**
     * 获取导入记录。
     *
     * @param taskId 任务ID
     * @return 导入记录
     */
    @Override
    public ImportRecord getImportRecord(String taskId) {
        return importRecordService.getByTaskId(taskId);
    }

    /**
     * 开始导出任务。
     *
     * @param task 任务
     * @param <T>  数据类型
     * @return 导出任务
     */
    private <T> ExportResult doExport(ExportTask<T> task) {
        ExportRequest<T> request = task.getRequest();
        String taskId = task.getTaskId();
        long startTime = System.currentTimeMillis();
        File tempFile = null;
        try {
            validateExportRequest(request);
            if (exportRecordService.getByTaskId(taskId) == null) {
                createExportRecord(task);
            }
            exportRecordService.markProcessing(taskId);
            task.start();

            tempFile = createTempFile(request.getFileName());
            ExportResult result = writeExportFile(task, tempFile);
            StorageObject storageObject = storageStrategyFactory
                    .getStrategy(resolveExportStorageType(request))
                    .store(tempFile, request.getFileName(), request.getFileMetadata());

            long costTime = System.currentTimeMillis() - startTime;
            task.finish();
            exportRecordService.markSuccess(taskId, storageObject.getUrl(), storageObject.getSize());
            return result
                    .setFileUrl(storageObject.getUrl())
                    .setFileSize(storageObject.getSize())
                    .setCostTime(costTime);
        } catch (Exception e) {
            log.error("Excel 导出任务失败: {}", taskId, e);
            exportRecordService.markFailed(taskId, "导出失败: " + e.getMessage());
            return ExportResult.fail(taskId, "导出失败: " + e.getMessage());
        } finally {
            task.finish();
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 文件写入
     *
     * @param task     任务
     * @param tempFile 临时文件
     * @param <T>      数据类型
     * @return 导出结果
     */
    private <T> ExportResult writeExportFile(ExportTask<T> task, File tempFile) {
        ExportRequest<T> request = task.getRequest();
        ResolveCellWriteHandler resolveHandler = fieldResolverPipeline != null ? new ResolveCellWriteHandler() : null;
        ResolveContext resolveContext = new ResolveContext();
        long totalRows = 0;
        long totalCount = estimateExportTotalCount(request);

        try (EnhancedWriterWrapper writer = new EnhancedWriterWrapper(
                tempFile.getAbsolutePath(), getMaxRowsPerSheet(request), resolveHandler)) {
            for (SheetExportContext<?> sheetContext : buildSheetContexts(request)) {
                totalRows += writeSheet(task, writer, resolveHandler, sheetContext, resolveContext, totalRows, totalCount);
            }
            return ExportResult.success(task.getTaskId(), totalRows, totalRows)
                    .setSheetCount(writer.getCurrentSheetIndex());
        } catch (Exception e) {
            throw new ExcelExportException("导出执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入Sheet
     *
     * @param task                 任务
     * @param writer               写入器
     * @param resolveHandler       解析处理器
     * @param sheetContext         Sheet上下文
     * @param resolveContext       解析上下文
     * @param processedBeforeSheet 已处理的行数（Sheet 之前）
     * @param totalCount           总记录数
     * @param <T>                  数据类型
     * @return 已处理的行数（Sheet 之后）
     */
    private <T> long writeSheet(ExportTask<?> task,
                                EnhancedWriterWrapper writer,
                                ResolveCellWriteHandler resolveHandler,
                                SheetExportContext<T> sheetContext,
                                ResolveContext resolveContext,
                                long processedBeforeSheet,
                                long totalCount) {
        ExportRequest<?> request = task.getRequest();
        long sheetRows = 0;
        int pageNum = 1;
        int pageSize = getExportPageSize(request);

        while (true) {
            checkExportTimeout(task);
            ExportPageData<T> pageData = sheetContext.dataSupplier.getPageData(pageNum, pageSize, request.getParams());
            if (pageData == null || CollUtil.isEmpty(pageData.getData())) {
                break;
            }

            List<T> pageRows = pageData.getData();
            if (fieldResolverPipeline != null) {
                ResolvedPage resolvedPage = fieldResolverPipeline.resolve(pageRows, sheetContext.dataClass, resolveContext);
                if (resolveHandler != null) {
                    resolveHandler.setResolvedPage(resolvedPage);
                }
            }
            writer.write(pageRows, sheetContext.dataClass, sheetContext.sheetName);

            sheetRows += pageRows.size();
            updateExportProgress(request, task.getTaskId(), processedBeforeSheet + sheetRows, totalCount);
            pageNum++;
            if (!pageData.isHasNext()) {
                break;
            }
        }
        return sheetRows;
    }

    /**
     * 导出执行
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 导出结果
     */
    private <T> ImportResult doImport(ImportTask<T> task) {
        ImportRequest<T> request = task.getRequest();
        String taskId = task.getTaskId();
        long startTime = System.currentTimeMillis();
        File importFile = null;
        try {
            validateImportRequest(request);
            if (importRecordService.getByTaskId(taskId) == null) {
                createImportRecord(task);
            }
            importRecordService.markProcessing(taskId);
            task.start();

            importFile = storageStrategyFactory
                    .getStrategy(resolveImportStorageType(request))
                    .download(request.getFileUrl());

            AnalysisListenersContext<T> context = AnalysisListenersContext.<T>builder()
                    .maxErrorCount(getMaxErrorCount(request))
                    .pageSize(getImportPageSize(request))
                    .request(request)
                    .build();

            try (ExcelReaderWrapper<T> reader =
                         new ExcelReaderWrapper<>(importFile.getAbsolutePath(), getHeadRowNumber(request), context)) {
                checkImportTimeout(task);
                reader.doRead();
                ImportResult result = buildImportResult(taskId, reader)
                        .withCostTime(System.currentTimeMillis() - startTime);
                importRecordService.updateProgress(taskId, result.getTotalCount(),
                        result.getSuccessCount(), result.getFailedCount());
                importRecordService.markSuccess(taskId, result);
                task.finish();
                return result;
            }
        } catch (Exception e) {
            log.error("Excel 导入任务失败: {}", taskId, e);
            importRecordService.markFailed(taskId, "导入失败: " + e.getMessage());
            return ImportResult.fail(taskId, "导入失败: " + e.getMessage());
        } finally {
            task.finish();
            cleanupTempFile(importFile);
        }
    }

    /**
     * 构建导入结果
     *
     * @param taskId 任务ID
     * @param reader 读取器
     * @param <T>    数据类型
     * @return 导入结果
     */
    private ImportResult buildImportResult(String taskId, ExcelReaderWrapper<?> reader) {
        long successCount = reader.getSuccessRowCount();
        long failedCount = reader.getFailRowCount();
        long totalCount = reader.getAllCount();
        if (failedCount == 0) {
            return ImportResult.success(taskId, totalCount, successCount);
        }
        return ImportResult.partialSuccess(taskId, totalCount, successCount, failedCount, reader.getAllErrors());
    }

    /**
     * 创建导出任务
     *
     * @param request 导出请求
     * @param async   是否异步执行
     * @param <T>     数据类型
     * @return 导出任务
     */
    private <T> ExportTask<T> createExportTask(ExportRequest<T> request, boolean async) {
        String businessType = request != null ? request.getBusinessType() : "default_export";
        ExportTask<T> task = new ExportTask<>();
        task.setTaskId(generateTaskId("EXPORT", businessType));
        task.setBusinessType(businessType);
        task.setRequest(request);
        task.setAsync(async);
        task.setCreateTime(System.currentTimeMillis());
        return task;
    }

    /**
     * 创建导入任务
     *
     * @param request 导入请求
     * @param async   是否异步执行
     * @param <T>     数据类型
     * @return 导入任务
     */
    private <T> ImportTask<T> createImportTask(ImportRequest<T> request, boolean async) {
        String businessType = request != null ? request.getBusinessType() : "default_import";
        ImportTask<T> task = new ImportTask<>();
        task.setTaskId(generateTaskId("IMPORT", businessType));
        task.setBusinessType(businessType);
        task.setRequest(request);
        task.setAsync(async);
        task.setCreateTime(System.currentTimeMillis());
        return task;
    }

    /**
     * 创建导出记录
     *
     * @param task 导出任务
     * @param <T>  数据类型
     */
    private <T> void createExportRecord(ExportTask<T> task) {
        ExportRequest<T> request = task.getRequest();
        ExportRecord record = ExportRecord.builder()
                .taskId(task.getTaskId())
                .businessType(task.getBusinessType())
                .fileName(request != null ? request.getFileName() : null)
                .storageType(resolveExportStorageType(request))
                .status(ExportStatus.PENDING)
                .createTime(LocalDateTime.now())
                .params(request != null ? request.getParams() : null)
                .totalCount(0L)
                .processedCount(0L)
                .build();
        exportRecordService.save(record);
    }

    /**
     * 创建导入记录
     *
     * @param task 导入任务
     * @param <T>  数据类型
     */
    private <T> void createImportRecord(ImportTask<T> task) {
        ImportRequest<T> request = task.getRequest();
        ImportRecord record = ImportRecord.builder()
                .taskId(task.getTaskId())
                .businessType(task.getBusinessType())
                .fileName(request != null ? request.getFileName() : null)
                .fileUrl(request != null ? request.getFileUrl() : null)
                .storageType(resolveImportStorageType(request))
                .status(ImportStatus.PENDING)
                .createTime(LocalDateTime.now())
                .params(request != null ? request.getParams() : null)
                .totalCount(0L)
                .successCount(0L)
                .failCount(0L)
                .build();
        importRecordService.save(record);
    }

    /**
     * 构建Sheet导出上下文列表
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return Sheet导出上下文列表
     */
    private <T> List<SheetExportContext<?>> buildSheetContexts(ExportRequest<T> request) {
        List<SheetExportContext<?>> contexts = new ArrayList<>();
        if (CollUtil.isNotEmpty(request.getSheets())) {
            for (ExportSheet<?> sheet : request.getSheets()) {
                contexts.add(toSheetContext(sheet));
            }
            return contexts;
        }
        contexts.add(new SheetExportContext<>("数据", request.getDataClass(), request.getDataSupplier()));
        return contexts;
    }

    /**
     * 将ExportSheet转换为SheetExportContext
     */
    private <T> SheetExportContext<T> toSheetContext(ExportSheet<T> sheet) {
        return new SheetExportContext<>(sheet.getSheetName(), sheet.getDataClass(), sheet.getDataSupplier());
    }

    private <T> void validateExportRequest(ExportRequest<T> request) {
        if (request == null) {
            throw new IllegalArgumentException("导出请求不能为空");
        }
        if (StrUtil.isBlank(request.getFileName())) {
            throw new IllegalArgumentException("导出文件名不能为空");
        }
        if (CollUtil.isEmpty(request.getSheets())) {
            if (request.getDataClass() == null) {
                throw new IllegalArgumentException("导出数据类型不能为空");
            }
            if (request.getDataSupplier() == null) {
                throw new IllegalArgumentException("导出数据供应器不能为空");
            }
        } else {
            for (ExportSheet<?> sheet : request.getSheets()) {
                if (StrUtil.isBlank(sheet.getSheetName())) {
                    throw new IllegalArgumentException("Sheet 名称不能为空");
                }
                if (sheet.getDataClass() == null || sheet.getDataSupplier() == null) {
                    throw new IllegalArgumentException("Sheet 数据类型和数据供应器不能为空");
                }
            }
        }
        long maxRowsPerSheet = getMaxRowsPerSheet(request);
        if (maxRowsPerSheet <= 0 || maxRowsPerSheet > EXCEL_MAX_ROWS_PER_SHEET) {
            throw new IllegalArgumentException("每个 Sheet 最大行数必须在 1 到 1048576 之间");
        }
    }

    /**
     * 验证导入请求
     *
     * @param request 导入请求
     */
    private <T> void validateImportRequest(ImportRequest<T> request) {
        if (request == null) {
            throw new IllegalArgumentException("导入请求不能为空");
        }
        if (request.getDataClass() == null) {
            throw new IllegalArgumentException("导入数据类型不能为空");
        }
        if (StrUtil.isBlank(request.getFileUrl())) {
            throw new IllegalArgumentException("导入文件地址不能为空");
        }
        if (request.getDataImporterSupplier() == null) {
            throw new IllegalArgumentException("导入数据处理器不能为空");
        }
    }

    /**
     * 估计导出总记录数
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 总记录数
     */
    private <T> long estimateExportTotalCount(ExportRequest<T> request) {
        if (CollUtil.isNotEmpty(request.getSheets())) {
            long total = 0;
            boolean unknown = false;
            for (ExportSheet<?> sheet : request.getSheets()) {
                long count = sheet.getDataSupplier().getTotalCount(request.getParams());
                if (count < 0) {
                    unknown = true;
                    break;
                }
                total += count;
            }
            return unknown ? -1 : total;
        }
        return request.getDataSupplier().getTotalCount(request.getParams());
    }

    /**
     * 更新导出进度
     */
    private void updateExportProgress(ExportRequest<?> request, String taskId, long processedCount, long totalCount) {
        boolean progressEnabled = request.getEnableProgress() == null
                ? properties.getExport().isProgressEnabled()
                : request.getEnableProgress();
        if (progressEnabled) {
            exportRecordService.updateProgress(taskId, processedCount, totalCount);
        }
    }

    /**
     * 检查导出任务是否超时
     */
    private void checkExportTimeout(ExportTask<?> task) {
        long timeoutMillis = properties.getExport().getTimeoutMinutes() * 60 * 1000;
        if (task.isTimeout(timeoutMillis)) {
            throw new ExcelExportException("导出任务执行超时，已中断");
        }
    }

    /**
     * 检查导入任务是否超时
     */
    private void checkImportTimeout(ImportTask<?> task) {
        long timeoutMillis = properties.getImports().getTimeoutMinutes() * 60 * 1000;
        if (task.isTimeout(timeoutMillis)) {
            throw new ExcelExportException("导入任务执行超时，已中断");
        }
    }

    private int getExportPageSize(ExportRequest<?> request) {
        return request.getPageSize() == null || request.getPageSize() <= 0
                ? properties.getExport().getPageSize()
                : request.getPageSize();
    }

    private long getMaxRowsPerSheet(ExportRequest<?> request) {
        return request.getMaxRowsPerSheet() == null
                ? properties.getExport().getMaxRowsPerSheet()
                : request.getMaxRowsPerSheet();
    }

    private int getImportPageSize(ImportRequest<?> request) {
        return request.getPageSize() == null || request.getPageSize() <= 0
                ? properties.getImports().getPageSize()
                : request.getPageSize();
    }

    private int getMaxErrorCount(ImportRequest<?> request) {
        return request.getMaxErrorCount() == null || request.getMaxErrorCount() <= 0
                ? properties.getImports().getMaxErrorCount()
                : request.getMaxErrorCount();
    }

    private int getHeadRowNumber(ImportRequest<?> request) {
        return request.getHeadRowNumber() == null || request.getHeadRowNumber() < 0
                ? properties.getImports().getHeadRowNumber()
                : request.getHeadRowNumber();
    }

    /**
     * 创建临时文件
     */
    private File createTempFile(String fileName) throws IOException {
        String safeFileName = fileName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
        File tempFile = new File(System.getProperty("java.io.tmpdir"),
                TEMP_EXPORT_PREFIX + IdUtil.fastSimpleUUID() + "_" + safeFileName);
        Files.createDirectories(tempFile.getParentFile().toPath());
        if (!tempFile.createNewFile()) {
            throw new ExcelExportException("创建临时文件失败: " + tempFile.getAbsolutePath());
        }
        return tempFile;
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            FileUtil.del(tempFile);
        }
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId(String prefix, String businessType) {
        String safeBusinessType = StrUtil.isNotBlank(businessType)
                ? businessType.replaceAll("[^a-zA-Z0-9]", "_")
                : "default";
        return prefix + "_" + safeBusinessType + "_" + System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID();
    }

    /**
     * 解析导出存储类型
     */
    private String resolveExportStorageType(ExportRequest<?> request) {
        return request != null && StrUtil.isNotBlank(request.getStorageType())
                ? request.getStorageType()
                : properties.getStorage().getType();
    }

    /**
     * 解析导入存储类型
     */
    private String resolveImportStorageType(ImportRequest<?> request) {
        return request != null && StrUtil.isNotBlank(request.getStorageType())
                ? request.getStorageType()
                : properties.getStorage().getType();
    }

    /**
     * 表格导出上下文
     *
     * @param <T> 数据类型
     */
    private static class SheetExportContext<T> {
        final String sheetName;
        final Class<T> dataClass;
        final ExportDataSupplier<T> dataSupplier;

        SheetExportContext(String sheetName, Class<T> dataClass, ExportDataSupplier<T> dataSupplier) {
            this.sheetName = sheetName;
            this.dataClass = dataClass;
            this.dataSupplier = dataSupplier;
        }
    }
}
