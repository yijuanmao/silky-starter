package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.core.model.export.ExportDataProcessor;
import com.silky.starter.excel.core.model.export.ExportPageData;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.enums.ExportStatus;
import com.silky.starter.excel.service.export.ExportRecordService;
import com.silky.starter.excel.service.storage.StorageService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 导出引擎，负责协调导出任务的整个生命周期，包括任务创建、数据处理、文件生成和上传
 *
 * @author zy
 * @date 2025-10-24 15:25
 **/
@Slf4j
public class ExportEngine {

    private static final long DEFAULT_MAX_ROWS_PER_SHEET = 200000;

    private static final String TEMP_FILE_PREFIX = "silky_export_";

    /**
     * 已处理任务总数
     */
    private final AtomicLong totalProcessedTasks = new AtomicLong(0);

    /**
     * 成功任务数
     */
    private final AtomicLong successTasks = new AtomicLong(0);

    /**
     * 失败任务数
     */
    private final AtomicLong failedTasks = new AtomicLong(0);

    private final StorageService storageService;

    private final ExportRecordService recordService;

    public ExportEngine(StorageService storageService, ExportRecordService recordService) {
        this.storageService = storageService;
        this.recordService = recordService;
    }

    /**
     * 同步导出
     */
    public <T> ExcelProcessResult exportSync(ExportRequest<T> request, String taskId) {
        long startTime = System.currentTimeMillis();

        log.info("开始同步导出任务: {}, 业务类型: {}", taskId, request.getBusinessType());

        File tempFile = null;
        try {
            // 参数验证
            validateExportRequest(request);

            // 创建导出记录
            ExportRecord record = createExportRecord(taskId, request);
            recordService.save(record);

            // 准备数据
            prepareExportData(request);

            // 创建临时文件
            tempFile = createTempFile(request.getFileName());

            // 执行导出
            ExportResult exportResult = this.executeExport(request, taskId, tempFile);

            // 上传文件
            String fileUrl = uploadExportFile(tempFile, request, taskId);

            // 更新记录
            updateRecordOnSuccess(taskId, fileUrl, exportResult);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("同步导出任务完成: {}, 文件URL: {}, 耗时: {}ms", taskId, fileUrl, costTime);

            totalProcessedTasks.incrementAndGet();
            successTasks.incrementAndGet();

            return ExcelProcessResult.exportSuccess(taskId,
                            fileUrl,
                            exportResult.getTotalCount(),
                            tempFile.length())
                    .setCostTime(costTime)
                    .setSheetCount(exportResult.getSheetCount())
                    .setTotalCount(exportResult.getTotalCount())
                    .setSuccessCount(exportResult.getSuccessCount())
                    .setFailedCount(exportResult.getFailedCount());

        } catch (Exception e) {
            failedTasks.incrementAndGet();
            log.error("同步导出任务失败: {}", taskId, e);
            recordService.updateFailed(taskId, "导出失败: " + e.getMessage());
            return ExcelProcessResult.fail(taskId, "导出失败: " + e.getMessage())
                    .setFailedCount(failedTasks.get());
        } finally {
            cleanupExportData(request);
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 执行导出逻辑
     *
     * @param request  导出请求
     * @param taskId   任务ID
     * @param tempFile 临时文件
     */
    private <T> ExportResult executeExport(ExportRequest<T> request, String taskId, File tempFile) {
        try (EnhancedExcelWriter writer = new EnhancedExcelWriter(tempFile.getAbsolutePath(),
                getMaxRowsPerSheet(request))) {

            ExportContext context = new ExportContext(taskId);
            int pageNum = 1;
            boolean hasNext = true;

            while (hasNext) {
                // 获取分页数据
                ExportPageData<T> pageData = request.getDataSupplier()
                        .getPageData(pageNum, request.getPageSize(), request.getParams());

                if (CollUtil.isEmpty(pageData.getData())) {
                    break;
                }

                // 处理数据
                List<T> processedData = processPageData(pageData.getData(),
                        request.getProcessors(), pageNum);

                // 写入数据
                if (pageNum == 1) {
                    writer.write(processedData, request.getDataClass(), request.getHeaderMapping());
                } else {
                    writer.write(processedData, request.getDataClass());
                }

                // 更新进度
                context.addProcessedCount(processedData.size());
                recordService.updateProgress(taskId, context.getProcessedCount());

                log.debug("第{}页处理完成, 数据量: {}", pageNum, processedData.size());

                hasNext = pageData.isHasNext();
                pageNum++;
            }
            if (request.getAsyncType().isAsync()) {
                return ExportResult.asyncSuccess(taskId);
            } else {
                return ExportResult.success(taskId)
                        .setTotalCount(totalProcessedTasks.get())
                        .setSuccessCount(successTasks.get())
                        .setFailedCount(failedTasks.get())
                        .setSheetCount(writer.getCurrentSheetIndex());
            }
        } catch (Exception e) {
            log.error("导出执行失败: {}", taskId, e);
            throw new ExcelExportException("导出执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理分页数据
     */
    private <T> List<T> processPageData(List<T> data, List<ExportDataProcessor<T>> processors, int pageNum) {
        if (CollUtil.isEmpty(processors)) {
            return data;
        }

        List<T> processedData = data;
        for (ExportDataProcessor<T> processor : processors) {
            processedData = processor.process(processedData, pageNum);
        }
        return processedData;
    }

    /**
     * 准备导出数据
     */
    private <T> void prepareExportData(ExportRequest<T> request) {
        Optional.ofNullable(request.getDataSupplier())
                .ifPresent(supplier -> supplier.prepare(request.getParams()));

        Optional.ofNullable(request.getProcessors())
                .ifPresent(processors -> processors.forEach(ExportDataProcessor::prepare));
    }

    /**
     * 清理导出数据
     */
    private <T> void cleanupExportData(ExportRequest<T> request) {
        Optional.ofNullable(request.getDataSupplier())
                .ifPresent(supplier -> {
                    try {
                        supplier.cleanup(request.getParams());
                    } catch (Exception e) {
                        log.warn("数据供应器清理异常", e);
                    }
                });

        Optional.ofNullable(request.getProcessors())
                .ifPresent(processors -> processors.forEach(processor -> {
                    try {
                        processor.cleanup();
                    } catch (Exception e) {
                        log.warn("数据处理器清理异常", e);
                    }
                }));
    }

    /**
     * 创建临时文件
     */
    private File createTempFile(String fileName) {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            String safeFileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String filePath = tempDir.resolve(TEMP_FILE_PREFIX + System.currentTimeMillis() + "_" + safeFileName).toString();

            File file = new File(filePath);
            File parentDir = file.getParentFile();

            if (parentDir != null && !parentDir.exists()) {
                Files.createDirectories(parentDir.toPath());
            }

            if (!file.createNewFile()) {
                throw new ExcelExportException("创建临时文件失败: " + filePath);
            }

            return file;

        } catch (IOException e) {
            throw new ExcelExportException("创建临时文件异常", e);
        }
    }

    /**
     * 上传导出文件
     */
    private <T> String uploadExportFile(File tempFile, ExportRequest<T> request, String taskId) {
        return storageService.upload(tempFile, request.getFileName(),
                request.getStorageType(), taskId);
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            try {
                FileUtil.del(tempFile);
            } catch (Exception e) {
                log.warn("临时文件删除异常: {}", tempFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * 更新成功记录
     */
    private void updateRecordOnSuccess(String taskId, String fileUrl, ExportResult exportResult) {
        recordService.update(taskId, record -> {
            record.setFileUrl(fileUrl);
            record.setTotalCount(exportResult.getTotalCount());
            record.setFileSize(exportResult.getFileSize());
            record.setStatus(ExportStatus.COMPLETED);
        });
    }

    /**
     * 验证导出请求
     */
    private <T> void validateExportRequest(ExportRequest<T> request) {
        if (request == null) {
            throw new IllegalArgumentException("导出请求不能为null");
        }
        if (request.getDataClass() == null) {
            throw new IllegalArgumentException("数据类类型不能为null");
        }
        if (StrUtil.isBlank(request.getFileName())) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (request.getDataSupplier() == null) {
            throw new IllegalArgumentException("数据供应器不能为null");
        }
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            throw new IllegalArgumentException("分页大小必须大于0");
        }
    }

    /**
     * 创建导出记录
     */
    private <T> ExportRecord createExportRecord(String taskId, ExportRequest<T> request) {
        return ExportRecord.builder()
                .taskId(taskId)
                .businessType(request.getBusinessType())
                .fileName(request.getFileName())
                .storageType(request.getStorageType())
                .asyncType(request.getAsyncType())
                .createUser(request.getCreateUser())
                .status(ExportStatus.PROCESSING)
                .createTime(LocalDateTime.now())
                .params(request.getParams())
                .totalCount(0L)
                .processedCount(0L)
                .build();
    }

    /**
     * 获取最大行数
     */
    private <T> long getMaxRowsPerSheet(ExportRequest<T> request) {
        return Optional.of(request.getMaxRowsPerSheet())
                .orElse(DEFAULT_MAX_ROWS_PER_SHEET);
    }


    /**
     * 导出上下文
     */
    @Getter
    private static class ExportContext {

        private final String taskId;

        private long processedCount = 0;

        public ExportContext(String taskId) {
            this.taskId = taskId;
        }

        public void addProcessedCount(int count) {
            processedCount += count;
        }

    }
}
