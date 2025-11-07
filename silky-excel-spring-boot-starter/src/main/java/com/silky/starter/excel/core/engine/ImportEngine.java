package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.BatchTask;
import com.silky.starter.excel.core.model.DataProcessor;
import com.silky.starter.excel.core.model.imports.DataImporterSupplier;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.entity.ImportRecord;
import com.silky.starter.excel.enums.ImportStatus;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.service.compression.CompressionService;
import com.silky.starter.excel.service.imports.ImportRecordService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 导入引擎核心类，负责协调整个导入流程，包括文件下载、数据读取、数据处理和数据导入
 *
 * @author zy
 * @date 2025-10-27 15:23
 **/
@Slf4j
public class ImportEngine {

    private static final int CACHE_CLEANUP_DELAY_MINUTES = 5;
    private static final int BATCH_PROCESSING_TIMEOUT_MINUTES = 30;

    // 统计变量
    private final AtomicLong totalProcessedImports = new AtomicLong(0);
    private final AtomicLong successImports = new AtomicLong(0);
    private final AtomicLong failedImports = new AtomicLong(0);

    // 缓存管理
    private final ConcurrentMap<String, ImportTask<?>> taskCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BatchTask<?>> batchTaskCache = new ConcurrentHashMap<>();

    // 依赖服务
    private final ImportRecordService recordService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final CompressionService compressionService;

    // 清理执行器
    private final ScheduledExecutorService cleanupExecutor;

    // 引擎启动时间
    private final long engineStartTime = System.currentTimeMillis();

    public ImportEngine(ImportRecordService recordService,
                        ThreadPoolTaskExecutor taskExecutor,
                        CompressionService compressionService) {
        this.recordService = recordService;
        this.taskExecutor = taskExecutor;
        this.compressionService = compressionService;

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "import-engine-cleanup")
        );

        // 启动缓存清理任务
        startCacheCleanupTask();
    }

    /**
     * 异步导入单个任务
     */
    public <T> ImportResult importAsync(ImportTask<?> task) {
        try {
            taskExecutor.execute(() -> processImportTask(task));
            return ImportResult.asyncSuccess(task.getTaskId());
        } catch (Exception e) {
            log.error("异步提交导入任务失败: {}", task.getTaskId(), e);
            return ImportResult.fail(task.getTaskId(), "异步提交失败: " + e.getMessage());
        }
    }

    /**
     * 同步导入单个任务
     */
    public <T> ImportResult importSync(ImportRequest<T> request) {
        ImportTask<T> task = createImportTask(request);
        return processImportTask(task);
    }

    /**
     * 大文件批次导入 - 单条记录，内部多线程处理
     */
    public <T> ImportResult importLargeFile(ImportTask<T> task, int batchSize) {
        return importLargeFile(task, batchSize, Math.min(batchSize, 10)); // 默认最大并发10个批次
    }

    /**
     * 大文件批次导入 - 支持自定义并发控制
     */
    public <T> ImportResult importLargeFile(ImportTask<T> task, int batchSize, int maxConcurrentBatches) {
        ImportRequest<T> request = task.getRequest();
        String taskId = task.getTaskId();
        long startTime = System.currentTimeMillis();

        log.info("开始大文件批次导入: {}, 业务类型: {}, 批次大小: {}, 最大并发: {}",
                taskId, request.getBusinessType(), batchSize, maxConcurrentBatches);

        // 创建导入记录（单条记录）
        ImportRecord record = createImportRecord(taskId, request);
        recordService.addImportRecord(record);
        cacheTask(task);

        File downloadedFile = null;
        File decompressedFile = null;
        File tempFile = null;

        try {
            // 下载文件
            downloadedFile = downloadImportFile(request, taskId);

            // 处理解压
            decompressedFile = processDecompression(downloadedFile, request, taskId);

            // 准备数据导入器
            prepareDataImporter(request);

            // 执行批次导入
            ImportResult importResult = executeBatchImport(task, decompressedFile, batchSize, maxConcurrentBatches);

            long costTime = System.currentTimeMillis() - startTime;
            successImports.incrementAndGet();

            log.info("大文件批次导入完成: {}, 结果: {}, 总耗时: {}ms",
                    taskId, importResult.getSummary(), costTime);

            return importResult.withCostTime(costTime);

        } catch (Exception e) {
            log.error("大文件批次导入失败: {}, 业务类型: {}", taskId, request.getBusinessType(), e);
            failedImports.incrementAndGet();
            recordService.updateFail(taskId, "导入失败: " + e.getMessage());
            return ImportResult.fail(taskId, "导入失败: " + e.getMessage());
        } finally {
            cleanupDataImporter(request);
            cleanupTempFiles(downloadedFile, decompressedFile, tempFile);
            totalProcessedImports.incrementAndGet();
        }
    }

    /**
     * 执行批次导入
     */
    private <T> ImportResult executeBatchImport(ImportTask<T> task, File importFile,
                                                int batchSize, int maxConcurrentBatches) {
        String taskId = task.getTaskId();
        ImportRequest<T> request = task.getRequest();

        try (EnhancedExcelReader<T> reader = new EnhancedExcelReader<>(importFile.getAbsolutePath(),
                request.getDataClass(), request.isSkipHeader())) {

            // 创建批次上下文
            BatchTask<List<T>> batchContext = BatchTask.<List<T>>builder()
                    .taskId(taskId)
                    .batchId("IMPORT_BATCH_" + IdUtil.fastSimpleUUID())
                    .batchIndex(0)
                    .totalBatches(0) // 稍后计算
                    .processedCount(new AtomicLong(0))
                    .successCount(new AtomicLong(0))
                    .failedCount(new AtomicLong(0))
                    .startTime(System.currentTimeMillis())
                    .compressed(request.isCompressionEnabled())
                    .compressionType(request.getCompressionType())
                    .build();

            // 使用信号量控制并发
            Semaphore semaphore = new Semaphore(maxConcurrentBatches);
            List<CompletableFuture<ImportBatchResult>> pageFutures = new ArrayList<>();

            int pageNum = 1;
            long totalCount = 0;
            long successCount = 0;
            long failedCount = 0;
            List<ImportResult.ImportError> allErrors = new ArrayList<>();

            // 开始事务（如果启用）
            if (request.isEnableTransaction()) {
                request.getDataImporterSupplier().beginTransaction();
            }

            try {
                while (true) {
                    final int currentPage = pageNum;

                    CompletableFuture<ImportBatchResult> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            semaphore.acquire();
                            return processImportPage(reader, request, currentPage, batchContext);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("导入页面处理被中断, 任务: {}, 页面: {}", taskId, currentPage, e);
                            return ImportBatchResult.failed("处理被中断");
                        } finally {
                            semaphore.release();
                        }
                    }, taskExecutor);

                    pageFutures.add(future);
                    batchContext.setBatchIndex(pageNum);

                    // 读取下一页数据
                    List<T> pageData = reader.readPage(pageNum, request.getPageSize());
                    if (CollUtil.isEmpty(pageData)) {
                        break;
                    }

                    totalCount += pageData.size();
                    batchContext.setTotalBatches(pageNum);
                    pageNum++;

                    // 更新进度
                    updateImportProgress(taskId, batchContext);
                }

                // 等待所有页面完成
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                        pageFutures.toArray(new CompletableFuture[0])
                );

                allFutures.get(BATCH_PROCESSING_TIMEOUT_MINUTES, TimeUnit.MINUTES);

                // 收集所有结果
                for (CompletableFuture<ImportBatchResult> future : pageFutures) {
                    try {
                        ImportBatchResult batchResult = future.get();
                        successCount += batchResult.getSuccessCount();
                        failedCount += batchResult.getFailedCount();
                        allErrors.addAll(batchResult.getErrors());
                    } catch (Exception e) {
                        log.error("获取导入批次结果失败", e);
                        failedCount += request.getPageSize(); // 估算失败数量
                    }
                }

                // 提交事务（如果启用）
                if (request.isEnableTransaction()) {
                    request.getDataImporterSupplier().commitTransaction();
                }

                log.info("批次导入完成: {}, 总数据量: {}, 成功: {}, 失败: {}, 错误数: {}",
                        taskId, totalCount, successCount, failedCount, allErrors.size());

                // 构建导入结果
                if (failedCount == 0) {
                    return ImportResult.success(taskId, totalCount, successCount);
                } else {
                    return ImportResult.partialSuccess(taskId, totalCount, successCount, failedCount, allErrors)
                            .withSkippedCount(0L);
                }

            } catch (TimeoutException e) {
                throw new ExcelExportException("批次导入超时", e);
            } catch (InterruptedException | ExecutionException e) {
                throw new ExcelExportException("批次导入失败", e);
            }
        } catch (Exception e) {
            log.error("批次导入执行失败: {}", taskId, e);
            // 回滚事务（如果启用）
            if (request.isEnableTransaction()) {
                try {
                    request.getDataImporterSupplier().rollbackTransaction();
                } catch (Exception rollbackEx) {
                    log.error("导入任务异常，尝试回滚事务失败: {}", taskId, rollbackEx);
                }
            }
            throw new ExcelExportException("批次导入执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理单个导入页面
     */
    private <T> ImportBatchResult processImportPage(EnhancedExcelReader<T> reader,
                                                    ImportRequest<T> request,
                                                    int pageNum,
                                                    BatchTask<List<T>> batchContext) {
        String taskId = batchContext.getTaskId();

        try {
            // 检查任务超时
            if (isTaskTimeout(taskId)) {
                return ImportBatchResult.failed("任务执行超时");
            }
            // 检查错误数量是否超过限制
            if (batchContext.getFailedCount().get() >= request.getMaxErrorCount()) {
                return ImportBatchResult.skipped("错误数量超过限制");
            }

            // 读取数据
            List<T> pageData = reader.readPage(pageNum, request.getPageSize());
            if (CollUtil.isEmpty(pageData)) {
                return ImportBatchResult.skipped("数据为空");
            }

            // 处理数据
            List<T> processedData = processImportData(pageData, request.getProcessors(), pageNum);

            // 数据导入
            DataImporterSupplier.ImportBatchResult batchResult = request.getDataImporterSupplier()
                    .importData(processedData, pageNum, request.getParams());

            // 更新统计
            batchContext.getProcessedCount().addAndGet(processedData.size());
            batchContext.getSuccessCount().addAndGet(batchResult.getSuccessCount());
            batchContext.getFailedCount().addAndGet(batchResult.getFailedCount());

            log.debug("第{}页数据处理完成, 数据量: {}, 成功: {}, 失败: {}, 任务: {}",
                    pageNum, processedData.size(), batchResult.getSuccessCount(),
                    batchResult.getFailedCount(), taskId);

            return ImportBatchResult.from(batchResult);

        } catch (Exception e) {
            log.error("第{}页数据处理失败, 任务: {}", pageNum, taskId, e);
            batchContext.getFailedCount().addAndGet(request.getPageSize()); // 估算失败数量
            return ImportBatchResult.failed("处理失败: " + e.getMessage());
        }
    }

    /**
     * 处理解压
     */
    private <T> File processDecompression(File sourceFile, ImportRequest<T> request, String taskId) throws IOException {
        if (!request.isCompressionEnabled()) {
            return sourceFile;
        }

        SilkyExcelProperties.CompressionConfig compressionConfig = SilkyExcelProperties.CompressionConfig.builder()
                .enabled(true)
                .type(request.getCompressionType())
                .compressionLevel(request.getCompressionLevel())
                .splitLargeFiles(request.isSplitLargeFiles())
                .splitSize(request.getSplitSize())
                .build();

        String decompressedPath = sourceFile.getAbsolutePath() + "_decompressed";
        return compressionService.decompressFile(sourceFile, compressionConfig, decompressedPath);
    }

    /**
     * 更新导入进度
     */
    private void updateImportProgress(String taskId, BatchTask<?> batchContext) {
        recordService.updateProgress(taskId,
                batchContext.getProcessedCount().get(),
                batchContext.getSuccessCount().get(),
                batchContext.getFailedCount().get());

        // 记录进度日志
        if (batchContext.getBatchIndex() % 10 == 0) { // 每10个批次记录一次
            log.info("导入进度: {}, 批次: {}/{}, 已处理: {}, 成功: {}, 失败: {}, 进度: {:.2f}%",
                    batchContext.getTaskId(),
                    batchContext.getBatchIndex(),
                    batchContext.getTotalBatches(),
                    batchContext.getProcessedCount().get(),
                    batchContext.getSuccessCount().get(),
                    batchContext.getFailedCount().get(),
                    batchContext.getProgress());
        }
    }

    /**
     * 处理单个导入任务（兼容原有逻辑）
     */
    private <T> ImportResult processImportTask(ImportTask<T> task) {
        ImportRequest<T> request = task.getRequest();
        String taskId = task.getTaskId();
        long startTime = System.currentTimeMillis();

        log.info("开始处理导入任务: {}, 业务类型: {}", taskId, request.getBusinessType());

        File downloadedFile = null;
        File decompressedFile = null;
        File tempFile = null;

        try {
            validateImportRequest(request);
            cacheTask(task);

            ImportRecord record = createImportRecord(taskId, request);
            recordService.addImportRecord(record);

            downloadedFile = downloadImportFile(request, taskId);
            decompressedFile = processDecompression(downloadedFile, request, taskId);
            prepareDataImporter(request);

            ImportResult result = doImport(request, taskId, decompressedFile);
            long costTime = System.currentTimeMillis() - startTime;

            log.info("导入任务完成: {}, 结果: {}, 总耗时: {}ms", taskId, result.getSummary(), costTime);

            if (result.isSuccess()) {
                successImports.incrementAndGet();
            } else {
                failedImports.incrementAndGet();
            }

            return result.withCostTime(costTime);

        } catch (Exception e) {
            log.error("导入任务失败: {}, 业务类型: {}", taskId, request.getBusinessType(), e);
            failedImports.incrementAndGet();
            recordService.updateFail(taskId, "导入失败: " + e.getMessage());
            return ImportResult.fail(taskId, "导入失败: " + e.getMessage());
        } finally {
            cleanupDataImporter(request);
            cleanupTempFiles(downloadedFile, decompressedFile, tempFile);
            totalProcessedImports.incrementAndGet();
        }
    }

    // ========== 导入批次结果辅助类 ==========

    /**
     * 导入批次结果
     */
    @Data
    @Builder
    private static class ImportBatchResult {
        private long successCount;
        private long failedCount;
        private long skippedCount;
        private List<ImportResult.ImportError> errors;
        private String message;
        private boolean success;

        public static ImportBatchResult success(long successCount) {
            return ImportBatchResult.builder()
                    .successCount(successCount)
                    .failedCount(0)
                    .skippedCount(0)
                    .errors(new ArrayList<>())
                    .success(true)
                    .build();
        }

        public static ImportBatchResult failed(String message) {
            return ImportBatchResult.builder()
                    .successCount(0)
                    .failedCount(0) // 具体数量由调用方设置
                    .skippedCount(0)
                    .errors(new ArrayList<>())
                    .message(message)
                    .success(false)
                    .build();
        }

        public static ImportBatchResult skipped(String message) {
            return ImportBatchResult.builder()
                    .successCount(0)
                    .failedCount(0)
                    .skippedCount(0) // 具体数量由调用方设置
                    .errors(new ArrayList<>())
                    .message(message)
                    .success(true)
                    .build();
        }

        public static ImportBatchResult from(DataImporterSupplier.ImportBatchResult supplierResult) {
            return ImportBatchResult.builder()
                    .successCount(supplierResult.getSuccessCount())
                    .failedCount(supplierResult.getFailedCount())
                    .errors(supplierResult.getErrors())
                    .success(supplierResult.getFailedCount() == 0)
                    .build();
        }
    }

    // ========== 缓存管理 ==========

    private void startCacheCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredCaches();
            } catch (Exception e) {
                log.error("导入缓存清理任务执行失败", e);
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    private void cleanupExpiredCaches() {
        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime - TimeUnit.MINUTES.toMillis(CACHE_CLEANUP_DELAY_MINUTES);

        // 清理任务缓存
        int taskCacheSizeBefore = taskCache.size();
        taskCache.entrySet().removeIf(entry -> {
            ImportTask<?> task = entry.getValue();
            return task.getFinishTime() != null && task.getFinishTime() < expireTime;
        });

        // 清理批次任务缓存
        int batchCacheSizeBefore = batchTaskCache.size();
        batchTaskCache.entrySet().removeIf(entry -> {
            BatchTask<?> task = entry.getValue();
            return task.getStartTime() < expireTime;
        });

        if (taskCacheSizeBefore != taskCache.size() || batchCacheSizeBefore != batchTaskCache.size()) {
            log.debug("导入缓存清理完成: 任务缓存 {}->{}, 批次缓存 {}->{}",
                    taskCacheSizeBefore, taskCache.size(),
                    batchCacheSizeBefore, batchTaskCache.size());
        }
    }

    /**
     * 执行导入
     *
     * @param request  导入请求
     * @param taskId   任务ID
     * @param tempFile 临时文件
     * @param <T>
     * @return
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

            if (request.isEnableTransaction()) {
                request.getDataImporterSupplier().beginTransaction();
            }

            while (hasMoreData) {
                long pageStartTime = System.currentTimeMillis();

                if (isTaskTimeout(taskId)) {
                    throw new ExcelExportException("任务执行超时，已中断");
                }

                if (allErrors.size() >= request.getMaxErrorCount()) {
                    log.warn("错误数量超过限制: {}，停止导入", request.getMaxErrorCount());
                    break;
                }

                List<T> pageData = reader.readPage(pageNum, request.getPageSize());
                if (pageData == null || pageData.isEmpty()) {
                    log.debug("第{}页数据为空，导入完成", pageNum);
                    break;
                }

                List<T> processedData = processImportData(pageData, request.getProcessors(), pageNum);
                DataImporterSupplier.ImportBatchResult batchResult = request.getDataImporterSupplier()
                        .importData(processedData, pageNum, request.getParams());

                totalCount += pageData.size();
                successCount += batchResult.getSuccessCount();
                failedCount += batchResult.getFailedCount();
                allErrors.addAll(batchResult.getErrors());

                recordService.updateProgress(taskId, totalCount, successCount, failedCount);

                long pageCostTime = System.currentTimeMillis() - pageStartTime;
                log.debug("第{}页数据处理完成: 数据量={}, 成功={}, 失败={}, 耗时={}ms",
                        pageNum, pageData.size(), batchResult.getSuccessCount(),
                        batchResult.getFailedCount(), pageCostTime);

                hasMoreData = reader.hasMoreData();
                pageNum++;
            }

            long totalCostTime = System.currentTimeMillis() - startImportTime;

            if (request.isEnableTransaction()) {
                request.getDataImporterSupplier().commitTransaction();
            }

            log.info("导入任务数据处理完成: {}, 总数据量: {}, 成功: {}, 失败: {}, 处理耗时: {}ms",
                    taskId, totalCount, successCount, failedCount, totalCostTime);

            if (failedCount == 0) {
                return ImportResult.success(taskId, totalCount, successCount)
                        .withCostTime(totalCostTime);
            } else {
                return ImportResult.partialSuccess(taskId, totalCount, successCount, failedCount, allErrors)
                        .withCostTime(totalCostTime)
                        .withSkippedCount(skippedCount);
            }

        } catch (Exception e) {
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

    private <T> File downloadImportFile(ImportRequest<T> request, String taskId) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String filePath = tempDir + File.separator + "silky_import_" +
                System.currentTimeMillis() + "_" + request.getFileName();
        File tempFile = new File(filePath);
        log.info("下载导入文件: {} -> {}", request.getFileUrl(), filePath);
        return tempFile;
    }

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

    private void cleanupTempFiles(File downloadedFile, File decompressedFile, File tempFile) {
        if (downloadedFile != null && downloadedFile.exists()) {
            try {
                FileUtil.del(downloadedFile);
            } catch (Exception e) {
                log.warn("下载文件删除异常: {}", downloadedFile.getAbsolutePath(), e);
            }
        }
        if (decompressedFile != null && decompressedFile.exists() && !decompressedFile.equals(downloadedFile)) {
            try {
                FileUtil.del(decompressedFile);
            } catch (Exception e) {
                log.warn("解压文件删除异常: {}", decompressedFile.getAbsolutePath(), e);
            }
        }
        if (tempFile != null && tempFile.exists()) {
            try {
                FileUtil.del(tempFile);
            } catch (Exception e) {
                log.warn("临时文件删除异常: {}", tempFile.getAbsolutePath(), e);
            }
        }
    }

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
                .compressionEnabled(request.isCompressionEnabled())
                .compressionType(request.getCompressionType())
                .build();
    }

    private String generateTaskId(String businessType) {
        String prefix = StrUtil.isNotBlank(businessType) ?
                businessType.replaceAll("[^a-zA-Z0-9]", "_") : "IMPORT";
        return prefix + "_" + System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID();
    }

    private <T> ImportTask<T> createImportTask(ImportRequest<T> request) {
        ImportTask<T> task = new ImportTask<>();
        task.setRequest(request);
        task.setTaskId(generateTaskId(request.getBusinessType()));
        return task;
    }

    private <T> void cacheTask(ImportTask<T> task) {
        taskCache.put(task.getTaskId(), task);
        log.debug("导入任务已缓存: {}", task.getTaskId());
    }

    private boolean isTaskTimeout(String taskId) {
        ImportTask<?> task = taskCache.get(taskId);
        if (task == null) {
            return false;
        }
        return task.isTimeout();
    }

    public void shutdown() {
        log.info("开始关闭导入引擎...");
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 清理所有缓存
        taskCache.clear();
        batchTaskCache.clear();

        log.info("导入引擎已关闭, 统计信息: 总任务={}, 成功={}, 失败={}",
                totalProcessedImports.get(), successImports.get(), failedImports.get());
    }

    /**
     * 获取引擎统计信息
     */
    public ImportEngineStatus getEngineStatus() {
        return ImportEngineStatus.builder()
                .engineStartTime(LocalDateTime.now())
                .totalProcessedTasks(totalProcessedImports.get())
                .successTasks(successImports.get())
                .failedTasks(failedImports.get())
                .cachedTasks(taskCache.size())
                .batchTasks(batchTaskCache.size())
                .uptime(System.currentTimeMillis() - engineStartTime)
                .build();
    }

    @Data
    @Builder
    public static class ImportEngineStatus {
        private LocalDateTime engineStartTime;
        private long totalProcessedTasks;
        private long successTasks;
        private long failedTasks;
        private int cachedTasks;
        private int batchTasks;
        private long uptime;

        public double getSuccessRate() {
            return totalProcessedTasks > 0 ? (double) successTasks / totalProcessedTasks : 0.0;
        }

        public long getAverageProcessTime() {
            return totalProcessedTasks > 0 ? uptime / totalProcessedTasks : 0;
        }
    }
}
