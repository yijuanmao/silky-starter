package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.DataProcessor;
import com.silky.starter.excel.core.model.imports.DataImporterSupplier;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.entity.ImportRecord;
import com.silky.starter.excel.enums.ImportStatus;
import com.silky.starter.excel.service.imports.ImportRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 导入引擎核心类，负责协调整个导入流程，包括文件下载、数据读取、数据处理和数据导入
 *
 * @author zy
 * @date 2025-10-27 15:23
 **/
@Slf4j
public class ImportEngine {

    private final ConcurrentMap<String, ImportTask<?>> taskCache = new ConcurrentHashMap<>();
    private final long engineStartTime = System.currentTimeMillis();
    private final AtomicLong totalProcessedImports = new AtomicLong(0);
    private final AtomicLong successImports = new AtomicLong(0);
    private final AtomicLong failedImports = new AtomicLong(0);

    private final ImportRecordService recordService;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public ImportEngine(ImportRecordService recordService, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.recordService = recordService;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    /**
     * 执行导入任务
     *
     * @param task 导入任务
     * @param <T>  数据类型
     * @return 导入结果
     */
    public <T> ImportResult importAsync(ImportTask<?> task) {
        ImportRequest<?> request = task.getRequest();

        // 参数校验
        validateImportRequest(request);

        String taskId = generateTaskId(request.getBusinessType());
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始创建导入任务: {}, 业务类型: {}", taskId, request.getBusinessType());

            // 创建导入记录
            ImportRecord record = createImportRecord(taskId, request);
            recordService.addImportRecord(record);

            // 缓存任务信息
            cacheTask(task);

            File file = downloadImportFile(request, taskId);
            // 提交任务到异步执行器
            threadPoolTaskExecutor.execute(() -> {
                try {
                    this.doImport(request, taskId, file);
                } finally {
                    cleanupTempFile(file); // 清理临时文件
                }
            });
            long costTime = System.currentTimeMillis() - startTime;

            log.info("导入任务创建成功: {}, 业务类型: {}, 异步方式: {}, 耗时: {}ms",
                    taskId, request.getBusinessType(), task.getAsyncType(), costTime);

            totalProcessedImports.incrementAndGet();

            return ImportResult.asyncSuccess(taskId);

        } catch (Exception e) {
            log.error("导入任务创建失败: {}, 业务类型: {}", taskId, request.getBusinessType(), e);
            failedImports.incrementAndGet();

            // 更新记录状态为失败
            recordService.updateFail(taskId, "任务创建失败: " + e.getMessage());

            throw new ExcelExportException("导入任务创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 同步导入
     *
     * @param request 导入请求
     * @param <T>     数据类型
     * @return 导入结果
     */
    public <T> ImportResult importSync(ImportRequest<T> request) {
        // 参数校验
        validateImportRequest(request);

        String taskId = generateTaskId(request.getBusinessType());
        long startTime = System.currentTimeMillis();

        File tempFile = null;

        try {
            log.info("开始同步导入任务: {}, 业务类型: {}", taskId, request.getBusinessType());

            // 创建导入记录
            ImportRecord record = createImportRecord(taskId, request);
            recordService.addImportRecord(record);

            // 下载文件
            tempFile = downloadImportFile(request, taskId);

            // 执行数据准备
            prepareDataImporter(request);

            // 执行导入逻辑
            ImportResult result = doImport(request, taskId, tempFile);

            long costTime = System.currentTimeMillis() - startTime;

            log.info("同步导入任务完成: {}, 结果: {}, 总耗时: {}ms",
                    taskId, result.getSummary(), costTime);

            if (result.isSuccess()) {
                successImports.incrementAndGet();
            } else {
                failedImports.incrementAndGet();
            }

            return result.withCostTime(costTime);

        } catch (Exception e) {
            log.error("同步导入任务失败: {}, 业务类型: {}", taskId, request.getBusinessType(), e);

            // 更新失败状态
            recordService.updateFail(taskId, "同步导入失败: " + e.getMessage());
            failedImports.incrementAndGet();

            return ImportResult.fail(taskId, "同步导入失败: " + e.getMessage());
        } finally {
            // 执行数据清理
            cleanupDataImporter(request);
            // 清理临时文件
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 执行导入逻辑的核心方法
     *
     * @param request  导入请求
     * @param taskId   任务ID
     * @param tempFile 临时文件
     * @param <T>      数据类型
     * @return 导入结果
     */
    private <T> ImportResult doImport(ImportRequest<T> request, String taskId, File tempFile) {
        long totalCount = 0;
        long successCount = 0;
        long failedCount = 0;
        long skippedCount = 0;
        List<ImportResult.ImportError> allErrors = new ArrayList<>();

        try (EnhancedExcelReader<T> reader = new EnhancedExcelReader<>(tempFile.getAbsolutePath(),
                request.getDataClass(), request.isSkipHeader())) {

            int pageNum = 1;
            boolean hasMoreData = true;
            long startImportTime = System.currentTimeMillis();

            // 开始事务（如果启用）
            if (request.isEnableTransaction()) {
                request.getDataImporterSupplier().beginTransaction();
            }
            // 分页处理数据
            while (hasMoreData) {
                long pageStartTime = System.currentTimeMillis();

                // 检查任务超时
                if (isTaskTimeout(taskId)) {
                    throw new ExcelExportException("任务执行超时，已中断");
                }

                // 检查错误数量是否超过限制
                if (allErrors.size() >= request.getMaxErrorCount()) {
                    log.warn("错误数量超过限制: {}，停止导入", request.getMaxErrorCount());
                    break;
                }
                // 分页读取数据
                List<T> pageData = reader.readPage(pageNum, request.getPageSize());

                if (pageData == null || pageData.isEmpty()) {
                    log.debug("第{}页数据为空，导入完成", pageNum);
                    break;
                }

                // 数据处理
                List<T> processedData = processImportData(pageData, request.getProcessors(), pageNum);

                // 数据导入
                DataImporterSupplier.ImportBatchResult batchResult = request.getDataImporterSupplier()
                        .importData(processedData, pageNum, request.getParams());

                // 更新统计
                totalCount += pageData.size();
                successCount += batchResult.getSuccessCount();
                failedCount += batchResult.getFailedCount();
                allErrors.addAll(batchResult.getErrors());

                // 更新进度
                recordService.updateProgress(taskId, totalCount, successCount, failedCount);

                // 记录分页处理时间
                long pageCostTime = System.currentTimeMillis() - pageStartTime;
                log.debug("第{}页数据处理完成: 数据量={}, 成功={}, 失败={}, 耗时={}ms",
                        pageNum, pageData.size(), batchResult.getSuccessCount(),
                        batchResult.getFailedCount(), pageCostTime);

                // 判断是否还有更多数据
                hasMoreData = reader.hasMoreData();
                pageNum++;
            }

            long totalCostTime = System.currentTimeMillis() - startImportTime;

            // 提交事务（如果启用）
            if (request.isEnableTransaction()) {
                request.getDataImporterSupplier().commitTransaction();
            }

            log.info("导入任务数据处理完成: {}, 总数据量: {}, 成功: {}, 失败: {}, 处理耗时: {}ms",
                    taskId, totalCount, successCount, failedCount, totalCostTime);

            // 构建导入结果
            if (failedCount == 0) {
                return ImportResult.success(taskId, totalCount, successCount)
                        .withCostTime(totalCostTime);
            } else {
                return ImportResult.partialSuccess(taskId, totalCount, successCount, failedCount, allErrors)
                        .withCostTime(totalCostTime)
                        .withSkippedCount(skippedCount);
            }

        } catch (Exception e) {
            // 回滚事务（如果启用）
            if (request.isEnableTransaction()) {
                try {
                    request.getDataImporterSupplier().rollbackTransaction();
                } catch (Exception rollbackEx) {
                    log.error("导入任务异常，尝试回滚事务失败: {}", taskId, rollbackEx);
                }
            }
            throw e;
        }
    }

    /**
     * 处理导入数据
     */
    private <T> List<T> processImportData(List<T> data, List<DataProcessor<T>> processors, int pageNum) {
        if (CollUtil.isEmpty(processors)) {
            return data;
        }
        List<T> processedData = data;
        for (DataProcessor<T> processor : processors) {
            long startTime = System.currentTimeMillis();
            processedData = processor.process(processedData, pageNum);
            long costTime = System.currentTimeMillis() - startTime;

            log.debug("数据处理器执行完成: {}, 页码: {}, 耗时: {}ms",
                    processor.getClass().getSimpleName(), pageNum, costTime);
        }
        return processedData;
    }

    /**
     * 下载导入文件
     */
    private <T> File downloadImportFile(ImportRequest<T> request, String taskId) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String filePath = tempDir + File.separator + "silky_import_" +
                System.currentTimeMillis() + "_" + request.getFileName();

        File tempFile = new File(filePath);

        // 从存储服务下载文件
        // 这里需要根据fileUrl和storageType来下载文件
        // 简化实现，实际项目中需要根据存储类型具体实现
        log.info("下载导入文件: {} -> {}", request.getFileUrl(), filePath);
        return tempFile;
    }

    /**
     * 准备数据导入器
     */
    private <T> void prepareDataImporter(ImportRequest<T> request) {
        if (request.getDataImporterSupplier() != null) {
            request.getDataImporterSupplier().prepare(request.getParams());
        }

        if (request.getProcessors() != null) {
            for (DataProcessor<T> processor : request.getProcessors()) {
                processor.prepare();
            }
        }
    }

    /**
     * 清理数据导入器
     */
    private <T> void cleanupDataImporter(ImportRequest<T> request) {
        try {
            if (request.getDataImporterSupplier() != null) {
                request.getDataImporterSupplier().cleanup(request.getParams());
            }
        } catch (Exception e) {
            log.warn("数据导入器清理异常", e);
        }

        try {
            if (request.getProcessors() != null) {
                for (DataProcessor<T> processor : request.getProcessors()) {
                    processor.cleanup();
                }
            }
        } catch (Exception e) {
            log.warn("数据处理器清理异常", e);
        }
    }

    /**
     * 校验导入请求参数
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
        if (request.getDataImporterSupplier() == null) {
            throw new IllegalArgumentException("数据导入器不能为null");
        }
        if (request.getPageSize() == null || request.getPageSize() <= 0) {
            throw new IllegalArgumentException("分页大小必须大于0");
        }
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            try {
                boolean deleted = FileUtil.del(tempFile);
                if (deleted) {
                    log.debug("临时文件删除成功: {}", tempFile.getAbsolutePath());
                } else {
                    log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
                }
            } catch (Exception e) {
                log.warn("临时文件删除异常: {}", tempFile.getAbsolutePath(), e);
            }
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
                .status(ImportStatus.PENDING)
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
        String prefix = StrUtil.isNotBlank(businessType) ?
                businessType.replaceAll("[^a-zA-Z0-9]", "_") : "IMPORT";
        return prefix + "_" + System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID();
    }

    /**
     * 缓存任务
     */
    private <T> void cacheTask(ImportTask<T> task) {
        taskCache.put(task.getTaskId(), task);
        log.debug("导入任务已缓存: {}", task.getTaskId());
    }

    /**
     * 检查任务是否超时
     */
    private boolean isTaskTimeout(String taskId) {
        ImportTask<?> task = taskCache.get(taskId);
        if (task == null) {
            return false;
        }
        return task.isTimeout();
    }

    /**
     * 调度任务缓存清理
     */
    private void scheduleTaskCacheCleanup(String taskId) {
        // 延迟清理
        new Thread(() -> {
            try {
                Thread.sleep(5 * 60 * 1000); // 5分钟
                taskCache.remove(taskId);
                log.debug("导入任务缓存已清理: {}", taskId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
