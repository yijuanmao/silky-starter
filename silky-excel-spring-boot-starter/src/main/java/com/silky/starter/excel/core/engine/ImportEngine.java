package com.silky.starter.excel.core.engine;

import cn.hutool.core.io.FileUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.entity.ImportRecord;
import com.silky.starter.excel.enums.ImportStatus;
import com.silky.starter.excel.service.imports.ImportRecordService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.LocalDateTime;

/**
 * 导入引擎核心类，负责协调整个导入流程，包括文件下载、数据读取、数据处理和数据导入
 *
 * @author zy
 * @date 2025-10-27 15:23
 **/
@Slf4j
public class ImportEngine {

    private final ImportRecordService recordService;

    public ImportEngine(ImportRecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * 同步导入
     */
    public <T> ExcelProcessResult importSync(ImportRequest<T> request) {
        String taskId = generateTaskId(request.getBusinessType());
        long startTime = System.currentTimeMillis();

        log.info("开始同步导入任务: {}, 业务类型: {}", taskId, request.getBusinessType());

        File tempFile = null;
        try {
            // 参数验证
            validateImportRequest(request);

            // 创建导入记录
            ImportRecord record = createImportRecord(taskId, request);
            recordService.addImportRecord(record);
            recordService.updateStatus(taskId, ImportStatus.PROCESSING);

            // 下载文件
            tempFile = downloadImportFile(request, taskId);

            // 执行导入
            ImportResult result = doImport(request, taskId, tempFile);

            // 更新记录
            if (result.isSuccess()) {
                recordService.updateSuccess(taskId, result);
            } else {
                recordService.updateFail(taskId, result.getMessage());
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.info("同步导入任务完成: {}, 结果: {}, 耗时: {}ms", taskId, result.getSummary(), costTime);

            return ExcelProcessResult.success(taskId, "导入完成", result.getTotalCount())
                    .setCostTime(costTime);

        } catch (Exception e) {
            log.error("同步导入任务失败: {}", taskId, e);
            recordService.updateFail(taskId, "导入失败: " + e.getMessage());
            return ExcelProcessResult.fail(taskId, "导入失败: " + e.getMessage());
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 执行导入逻辑
     */
    private <T> ImportResult doImport(ImportRequest<T> request, String taskId, File tempFile) {
        try (EnhancedExcelReader<T> reader = new EnhancedExcelReader<>(
                tempFile.getAbsolutePath(), request.getDataClass(), true)) {

            // 简化的导入逻辑 - 实际项目中需要根据具体需求实现
            long totalCount = 0;
            long successCount = 0;

            // 这里实现具体的导入逻辑
            // 例如：reader.readAll() 或者分页读取处理

            return ImportResult.success(taskId, totalCount, successCount);

        } catch (Exception e) {
            throw new ExcelExportException("导入执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载导入文件
     */
    private <T> File downloadImportFile(ImportRequest<T> request, String taskId) {
        // 简化实现 - 实际项目中需要根据fileUrl下载文件
        String tempDir = System.getProperty("java.io.tmpdir");
        String filePath = tempDir + File.separator + "silky_import_" +
                System.currentTimeMillis() + "_" + request.getFileName();

        // 这里应该实现从存储服务下载文件的逻辑
        log.info("下载导入文件: {} -> {}", request.getFileUrl(), filePath);

        return new File(filePath);
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
     * 验证导入请求
     */
    private <T> void validateImportRequest(ImportRequest<T> request) {
        if (request == null) {
            throw new IllegalArgumentException("导入请求不能为null");
        }
        if (request.getDataClass() == null) {
            throw new IllegalArgumentException("数据类类型不能为null");
        }
        if (request.getFileName() == null || request.getFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (request.getFileUrl() == null || request.getFileUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("文件URL不能为空");
        }
    }

    /**
     * 创建导入记录
     */
    private <T> ImportRecord createImportRecord(String taskId, ImportRequest<T> request) {
        return ImportRecord.builder()
                .taskId(taskId)
                .businessType(request.getBusinessType())
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .storageType(request.getStorageType())
                .createUser(request.getCreateUser())
                .status(ImportStatus.PROCESSING)
                .createTime(LocalDateTime.now())
                .params(request.getParams())
                .totalCount(0L)
                .successCount(0L)
                .failCount(0L)
                .build();
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId(String businessType) {
        return businessType + "_" + System.currentTimeMillis() + "_" +
                java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
