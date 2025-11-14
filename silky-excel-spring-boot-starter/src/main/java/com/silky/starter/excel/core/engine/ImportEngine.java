package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.AnalysisListenersContext;
import com.silky.starter.excel.core.model.BatchTask;
import com.silky.starter.excel.core.model.DataProcessor;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.core.storage.factory.StorageStrategyFactory;
import com.silky.starter.excel.entity.ImportRecord;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.ImportStatus;
import com.silky.starter.excel.enums.StorageType;
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
import java.util.List;
import java.util.Objects;
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
    private final StorageStrategyFactory storageStrategyFactory;
    private final SilkyExcelProperties silkyExcelProperties;

    // 清理执行器
    private final ScheduledExecutorService cleanupExecutor;

    // 引擎启动时间
    private final long engineStartTime = System.currentTimeMillis();

    private final StorageType defaultStorageType;

    public ImportEngine(ImportRecordService recordService,
                        ThreadPoolTaskExecutor taskExecutor,
                        CompressionService compressionService,
                        StorageStrategyFactory storageStrategyFactory,
                        SilkyExcelProperties properties) {
        this.recordService = recordService;
        this.taskExecutor = taskExecutor;
        this.compressionService = compressionService;
        this.storageStrategyFactory = storageStrategyFactory;
        this.silkyExcelProperties = properties;
        this.defaultStorageType = properties.getStorage().getStorageType();

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
     * 处理单个导入任务（兼容原有逻辑）
     */
    private <T> ImportResult processImportTask(ImportTask<T> task) {
        ImportRequest<T> request = task.getRequest();
        String taskId = task.getTaskId();
        long startTime = System.currentTimeMillis();

        log.info("开始处理导入任务: {}, 业务类型: {}", taskId, request.getBusinessType());

        File downloadedFile = null;
        File decompressedFile = null;

        try {
            validateImportRequest(request);
            cacheTask(task);

            ImportRecord record = createImportRecord(taskId, request);
            recordService.addImportRecord(record);

            downloadedFile = downloadImportFile(request);
            decompressedFile = processDecompression(downloadedFile, request, taskId);
            prepareDataImporter(request);

            ImportResult result = doImport(request, taskId, decompressedFile, task.getAsyncType());
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
            cleanupTempFiles(downloadedFile, decompressedFile);
            totalProcessedImports.incrementAndGet();
        }
    }


    private void startCacheCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredCaches();
            } catch (Exception e) {
                log.error("导入缓存清理任务执行失败", e);
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    /**
     * 清理过期缓存
     */
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
     * @param request   导入请求
     * @param taskId    任务ID
     * @param tempFile  临时文件
     * @param asyncType 异步类型
     * @param <T>
     * @return ImportResult
     */
    private <T> ImportResult doImport(ImportRequest<T> request, String taskId, File tempFile, AsyncType asyncType) {
        long skippedCount = 0;

        AnalysisListenersContext<T> context = AnalysisListenersContext.<T>builder()
                .maxErrorCount(this.getMaxErrorCount(request.getMaxErrorCount()))
                .pageSize(this.getPageSize(request.getPageSize()))
                .request(request)
                .TotalCount(0)
                .SuccessCount(0)
                .FailCount(0)
                .build();

        try (ExcelReaderWrapper<T> reader = new ExcelReaderWrapper<>(tempFile.getAbsolutePath(), request.isSkipHeader(), context)) {
            long startImportTime = System.currentTimeMillis();
            // 开始事务（如果启用）
            if (request.isEnableTransaction()) {
                request.getDataImporterSupplier().beginTransaction();
            }
            long pageStartTime = System.currentTimeMillis();

            // 检查任务超时
            if (isTaskTimeout(taskId)) {
                throw new ExcelExportException("任务执行超时，已中断");
            }

            if (AsyncType.THREAD_POOL.equals(asyncType)) {
                reader.doReadAll();
            } else {
                reader.doRead();
            }
            long successCount = reader.getSuccessRowCount();
            long failedCount = reader.getFailRowCount();
            long totalCount = reader.getAllCount();

            // 更新进度
            recordService.updateProgress(taskId, totalCount, successCount, failedCount);

            // 记录分页处理时间
            long pageCostTime = System.currentTimeMillis() - pageStartTime;
            log.debug("数据处理完成: 数据量={}, 成功={}, 失败={}, 耗时={}ms",
                    totalCount, successCount, totalCount, pageCostTime);

            long totalCostTime = System.currentTimeMillis() - startImportTime;

            // 提交事务（如果启用）
            if (request.isEnableTransaction()) {
                request.getDataImporterSupplier().commitTransaction();
            }

            log.debug("导入任务数据处理完成: {}, 总数据量: {}, 成功: {}, 失败: {}, 处理耗时: {}ms",
                    taskId, totalCount, successCount, failedCount, totalCostTime);

            // 构建导入结果
            if (failedCount == 0) {
                return ImportResult.success(taskId, totalCount, successCount)
                        .withCostTime(totalCostTime);
            } else {
                return ImportResult.partialSuccess(taskId, totalCount, successCount, failedCount, reader.getAllErrors())
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
     *
     * @param data       原始数据
     * @param processors 数据处理器列表
     * @param <T>        数据类型
     * @return 处理后的数据
     */
    private <T> List<T> processImportData(List<T> data, List<DataProcessor<T>> processors) {
        if (CollUtil.isEmpty(processors)) {
            return data;
        }
        List<T> processedData = data;
        for (DataProcessor<T> processor : processors) {
            long startTime = System.currentTimeMillis();
            processedData = processor.process(processedData);
            long costTime = System.currentTimeMillis() - startTime;
            log.debug("数据处理器执行完成: {}, 耗时: {}ms",
                    processor.getClass().getSimpleName(), costTime);
        }
        return processedData;
    }

    /**
     * 下载导入文件
     *
     * @param request 导入请求
     * @return 下载的临时文件
     */
    private <T> File downloadImportFile(ImportRequest<T> request) {
        StorageType storageType = request.getStorageType() == null ? defaultStorageType : request.getStorageType();
        return storageStrategyFactory.getStrategy(storageType).downloadFile(request.getFileUrl());
    }

    /**
     * 准备数据导入器
     *
     * @param request 导入请求
     * @param <T>     数据类型
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
     * 清理临时文件
     *
     * @param downloadedFile   下载的文件
     * @param decompressedFile 解压后的文件
     */
    private void cleanupTempFiles(File downloadedFile, File decompressedFile) {
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

    /**
     * 缓存任务
     *
     * @param task 导入任务
     * @param <T>  数据类型
     */
    private <T> void cacheTask(ImportTask<T> task) {
        taskCache.put(task.getTaskId(), task);
        log.debug("导入任务已缓存: {}", task.getTaskId());
    }

    /**
     * 获取最大错误数量
     *
     * @param maxErrorCount 最大错误数量
     * @return 最大错误数量
     */
    private int getMaxErrorCount(Integer maxErrorCount) {
        return Objects.isNull(maxErrorCount) ? silkyExcelProperties.getImports().getMaxErrorCount() : maxErrorCount;
    }

    /**
     * 获取最大读取数量
     *
     * @param pageSize 最大读取数量
     * @return 最大错误数量
     */
    private int getPageSize(Integer pageSize) {
        return Objects.isNull(pageSize) ? silkyExcelProperties.getImports().getPageSize() : pageSize;
    }

    /**
     * 判断任务是否超时
     *
     * @param taskId 任务ID
     * @return 是否超时
     */
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
