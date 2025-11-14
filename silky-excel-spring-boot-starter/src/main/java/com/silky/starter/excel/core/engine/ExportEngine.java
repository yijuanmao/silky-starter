package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.BatchTask;
import com.silky.starter.excel.core.model.DataProcessor;
import com.silky.starter.excel.core.model.export.ExportPageData;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.core.storage.factory.StorageStrategyFactory;
import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.ExportStatus;
import com.silky.starter.excel.enums.StorageType;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.service.compression.CompressionService;
import com.silky.starter.excel.service.export.ExportRecordService;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 导出引擎，负责协调导出任务的整个生命周期，包括任务创建、数据处理、文件生成和上传
 *
 * @author zy
 * @date 2025-10-24 15:25
 **/
@Slf4j
public class ExportEngine {

    private static final String TEMP_FILE_PREFIX = "silky_export_";
    private static final int CACHE_CLEANUP_DELAY_MINUTES = 5;

    // 统计变量
    private final AtomicLong totalProcessedTasks = new AtomicLong(0);
    private final AtomicLong successTasks = new AtomicLong(0);
    private final AtomicLong failedTasks = new AtomicLong(0);

    // 缓存管理
    private final ConcurrentMap<String, ExportTask<?>> taskCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BatchTask<?>> batchTaskCache = new ConcurrentHashMap<>();

    // 依赖服务
    private final StorageStrategyFactory storageStrategyFactory;
    private final ExportRecordService recordService;
    private final SilkyExcelProperties properties;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final CompressionService compressionService;

    // 配置
    private final StorageType defaultStorageType;
    private final AsyncType defaultAsyncType;
    private final long defaultTimeout;

    // 清理执行器
    private final ScheduledExecutorService cleanupExecutor;

    // 引擎启动时间
    private final long engineStartTime = System.currentTimeMillis();

    public ExportEngine(StorageStrategyFactory storageStrategyFactory,
                        ExportRecordService recordService,
                        SilkyExcelProperties properties,
                        ThreadPoolTaskExecutor taskExecutor,
                        CompressionService compressionService) {
        this.storageStrategyFactory = storageStrategyFactory;
        this.recordService = recordService;
        this.properties = properties;
        this.taskExecutor = taskExecutor;
        this.compressionService = compressionService;

        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "export-engine-cleanup")
        );
        this.defaultStorageType = properties.getStorage().getStorageType();
        this.defaultAsyncType = properties.getAsync().getAsyncType();
        this.defaultTimeout = properties.getExport().getTimeoutMinutes();

        // 启动缓存清理任务
        startCacheCleanupTask();
    }

    /**
     * 同步导出单个任务
     */
    public <T> ExportResult exportSync(ExportTask<T> task) {
        return processExportTask(task);
    }

    /**
     * 异步导出单个任务
     */
    public <T> ExportResult exportAsync(ExportTask<T> task) {
        try {
            taskExecutor.execute(() -> processExportTask(task));
            return ExportResult.asyncSuccess(task.getTaskId());
        } catch (Exception e) {
            log.error("异步提交导出任务失败: {}", task.getTaskId(), e);
            return ExportResult.fail(task.getTaskId(), "异步提交失败: " + e.getMessage());
        }
    }

    /**
     * 处理压缩
     */
    private <T> File processCompression(File sourceFile, ExportRequest<T> request) throws IOException {
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

        String compressedPath = sourceFile.getAbsolutePath() + "_compressed";
        return compressionService.compressFile(sourceFile, compressionConfig, compressedPath);
    }

    /**
     * 处理单个任务
     *
     * @param task 任务
     */
    private <T> ExportResult processExportTask(ExportTask<T> task) {
        ExportRequest<T> request = task.getRequest();
        String taskId = task.getTaskId();
        long startTime = System.currentTimeMillis();

        log.debug("开始处理导出任务: {}, 业务类型: {}", taskId, request.getBusinessType());

        File tempFile = null;
        File finalFile = null;

        try {
            validateExportRequest(request);
            cacheTask(task);
            createAndSaveExportRecord(task);
            prepareExportData(request);

            tempFile = createTempFile(request.getFileName());
            ExportResult exportResult = executeSingleExport(request, taskId, tempFile, task.getAsyncType());

            // 处理压缩
            finalFile = processCompression(tempFile, request);

            String fileUrl = uploadExportFile(finalFile, request);
            updateRecordOnSuccess(taskId, fileUrl, exportResult);

            long costTime = System.currentTimeMillis() - startTime;
            successTasks.incrementAndGet();

            log.info("导出任务完成: {}, 文件URL: {}, 耗时: {}ms", taskId, fileUrl, costTime);

            return exportResult.setFileUrl(fileUrl)
                    .setFileSize(finalFile.length())
                    .setCostTime(costTime);

        } catch (Exception e) {
            log.error("导出任务失败: {}", taskId, e);
            failedTasks.incrementAndGet();
            recordService.updateFailed(taskId, "导出失败: " + e.getMessage());
            return ExportResult.fail(taskId, "导出失败: " + e.getMessage());
        } finally {
            cleanupExportResources(request, tempFile, finalFile, taskId);
            totalProcessedTasks.incrementAndGet();
        }
    }

    /**
     * 执行单个任务
     *
     * @param request   请求
     * @param taskId    任务ID
     * @param tempFile  临时文件
     * @param asyncType 异步类型
     * @param <T>       数据类型
     * @return 结果
     */
    private <T> ExportResult executeSingleExport(ExportRequest<T> request, String taskId,
                                                 File tempFile, AsyncType asyncType) {
        try (EnhancedWriterWrapper writer = new EnhancedWriterWrapper(tempFile.getAbsolutePath(),
                getMaxRowsPerSheet(request))) {

            ExportContext<T> context = new ExportContext<>(taskId, request);

            while (context.isHasNext()) {
                checkTaskTimeout(taskId, request.getTimeout());
                ExportPageData<T> pageData = fetchPageData(request, context.getCurrentPage());

                if (CollUtil.isEmpty(pageData.getData())) {
                    break;
                }

                List<T> processedData = processPageData(pageData.getData(), request.getProcessors(), context.getCurrentPage());
                writePageData(writer, processedData, request, context.getCurrentPage());

                if (request.isEnableProgress()) {
                    updateSingleExportProgress(context, processedData.size(), pageData.isHasNext());
                }
            }

            if (asyncType.isAsync()) {
                return ExportResult.asyncSuccess(taskId);
            } else {
                return ExportResult.success(taskId)
                        .setTotalCount(writer.getTotalRows().get())
                        .setSuccessCount(writer.getTotalRows().get())
                        .setFailedCount(0L)
                        .setSheetCount(writer.getCurrentSheetIndex());
            }
        } catch (Exception e) {
            log.error("导出执行失败: {}", taskId, e);
            throw new ExcelExportException("导出执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新单个任务进度
     *
     * @param context   上下文
     * @param batchSize 批次大小
     * @param hasNext   是否有下一页
     * @param <T>       数据类型
     */
    private <T> void updateSingleExportProgress(ExportContext<T> context, int batchSize, boolean hasNext) {
        context.addProcessedCount(batchSize);
        context.setHasNext(hasNext);
        context.nextPage();
        recordService.updateProgress(context.getTaskId(), context.getProcessedCount(), batchSize, 0);
        log.debug("导出进度更新: 任务ID={}, 当前页={}, 已处理={}, 还有下一页={}",
                context.getTaskId(), context.getCurrentPage(), context.getProcessedCount(), hasNext);
    }


    /**
     * 启动缓存清理任务
     */
    private void startCacheCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredCaches();
            } catch (Exception e) {
                log.error("缓存清理任务执行失败", e);
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
            ExportTask<?> task = entry.getValue();
            return task.getFinishTime() != null && task.getFinishTime() < expireTime;
        });

        // 清理批次任务缓存
        int batchCacheSizeBefore = batchTaskCache.size();
        batchTaskCache.entrySet().removeIf(entry -> {
            BatchTask<?> task = entry.getValue();
            return task.getStartTime() < expireTime;
        });

        if (taskCacheSizeBefore != taskCache.size() || batchCacheSizeBefore != batchTaskCache.size()) {
            log.debug("缓存清理完成: 任务缓存 {}->{}, 批次缓存 {}->{}",
                    taskCacheSizeBefore, taskCache.size(),
                    batchCacheSizeBefore, batchTaskCache.size());
        }
    }


    /**
     * 处理页面数据
     *
     * @param data       数据
     * @param processors 处理器
     * @param pageNum    页码
     * @param <T>        数据类型
     * @return 处理后的数据
     */
    private <T> List<T> processPageData(List<T> data, List<DataProcessor<T>> processors, int pageNum) {
        if (CollUtil.isEmpty(processors)) {
            return data;
        }
        List<T> processedData = data;
        for (DataProcessor<T> processor : processors) {
            processedData = processor.process(processedData);
        }
        return processedData;
    }

    /**
     * 准备导出数据
     *
     * @param request 请求
     * @param <T>     数据类型
     */
    private <T> void prepareExportData(ExportRequest<T> request) {
        Optional.ofNullable(request.getDataSupplier())
                .ifPresent(supplier -> supplier.prepare(request.getParams()));
        Optional.ofNullable(request.getProcessors())
                .ifPresent(processors -> processors.forEach(DataProcessor::prepare));
    }

    /**
     * 清理导出数据
     *
     * @param request 请求
     * @param <T>     数据类型
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
     *
     * @param fileName 文件名
     * @return 临时文件
     */
    private File createTempFile(String fileName) {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            String safeFileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String uniqueId = IdUtil.fastSimpleUUID();
            String filePath = tempDir.resolve(TEMP_FILE_PREFIX + uniqueId + "_" + safeFileName).toString();

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
     *
     * @param tempFile 临时文件
     * @param request  导出请求
     * @param <T>      数据类型
     * @return 文件URL
     */
    private <T> String uploadExportFile(File tempFile, ExportRequest<T> request) {
        StorageType storageType = request.getStorageType() == null ? defaultStorageType : request.getStorageType();
        return storageStrategyFactory.getStrategy(storageType).storeFile(tempFile, request.getFileName(), request.getFileMetadata());
    }

    /**
     * 清理临时文件
     *
     * @param tempFile 临时文件
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
     * 清理导出资源
     *
     * @param request   导出请求
     * @param tempFile  临时文件
     * @param finalFile 最终文件
     * @param taskId    任务ID
     */
    private void cleanupExportResources(ExportRequest<?> request, File tempFile, File finalFile, String taskId) {
        cleanupTempFile(tempFile);
        if (finalFile != null && !finalFile.equals(tempFile)) {
            cleanupTempFile(finalFile);
        }
        cleanupExportData(request);
    }

    /**
     * 更新导出记录
     *
     * @param taskId       任务ID
     * @param fileUrl      文件URL
     * @param exportResult 导出结果
     */
    private void updateRecordOnSuccess(String taskId, String fileUrl, ExportResult exportResult) {
        recordService.update(taskId, record -> {
            record.setFileUrl(fileUrl);
            record.setTotalCount(exportResult.getTotalCount());
            record.setFileSize(exportResult.getFileSize());
            record.setStatus(ExportStatus.COMPLETED);
        });
    }

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
    }

    private <T> ExportRecord createAndSaveExportRecord(ExportTask<T> task) {
        String taskId = task.getTaskId();
        ExportRequest<T> request = task.getRequest();
        ExportRecord record = ExportRecord.builder()
                .taskId(taskId)
                .businessType(request.getBusinessType())
                .fileName(request.getFileName())
                .storageType(Objects.isNull(request.getStorageType()) ? defaultStorageType : request.getStorageType())
                .asyncType(Objects.isNull(task.getAsyncType()) ? defaultAsyncType : task.getAsyncType())
                .createUser(request.getCreateUser())
                .status(ExportStatus.PROCESSING)
                .createTime(LocalDateTime.now())
                .params(request.getParams())
                .totalCount(0L)
                .processedCount(0L)
                .compressionEnabled(request.isCompressionEnabled())
                .compressionType(request.getCompressionType())
                .build();
        recordService.save(record);
        return record;
    }

    /**
     * 获取最大行数限制
     *
     * @param request 导出请求
     */
    private <T> long getMaxRowsPerSheet(ExportRequest<T> request) {
        return Objects.isNull(request.getMaxRowsPerSheet()) ? properties.getExport().getMaxRowsPerSheet() : request.getMaxRowsPerSheet();
    }

    /**
     * 检查任务是否超时
     *
     * @param taskId  任务ID
     * @param timeout 超时时间
     */
    private void checkTaskTimeout(String taskId, Long timeout) {
        ExportTask<?> task = taskCache.get(taskId);
        if (task != null && task.isTimeout(Objects.isNull(timeout) ? defaultTimeout : timeout)) {
            throw new ExcelExportException("导出任务执行超时，已中断");
        }
    }

    /**
     * 缓存任务
     *
     * @param task 任务
     * @param <T>  数据类型
     */
    private <T> void cacheTask(ExportTask<T> task) {
        taskCache.put(task.getTaskId(), task);
        log.debug("任务已缓存: {}", task.getTaskId());
    }

    /**
     * 写入页面数据
     *
     * @param writer  写入器
     * @param data    数据
     * @param request 请求
     * @param pageNum 页码
     * @param <T>
     */
    private <T> void writePageData(EnhancedWriterWrapper writer, List<T> data,
                                   ExportRequest<T> request, int pageNum) {
        long startTime = System.currentTimeMillis();
        writer.write(data, request.getDataClass());
        long costTime = System.currentTimeMillis() - startTime;
        log.debug("第{}页数据写入完成, 数据量: {}, 当前Sheet: {}, 耗时: {}ms",
                pageNum, data.size(), writer.getCurrentSheetIndex(), costTime);
    }

    /**
     * 获取页面数据
     *
     * @param request 请求
     * @param pageNum 页码
     * @param <T>     数据类型
     * @return 页面数据
     */
    private <T> ExportPageData<T> fetchPageData(ExportRequest<T> request, int pageNum) {
        long startTime = System.currentTimeMillis();
        ExportPageData<T> pageData = request.getDataSupplier().getPageData(
                pageNum, request.getPageSize(), request.getParams());
        log.debug("第{}页数据查询完成, 数据量: {}, 耗时: {}ms",
                pageNum, CollUtil.size(pageData.getData()), System.currentTimeMillis() - startTime);
        return pageData;
    }

    public void shutdown() {
        log.info("开始关闭导出引擎...");
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

        log.info("导出引擎已关闭, 统计信息: 总任务={}, 成功={}, 失败={}",
                totalProcessedTasks.get(), successTasks.get(), failedTasks.get());
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId(String businessType) {
        String safeBusinessType = StrUtil.isNotBlank(businessType) ?
                businessType.replaceAll("[^a-zA-Z0-9]", "_") : "EXPORT";
        return String.format("EXPORT_%s_%s_%s",
                safeBusinessType,
                System.currentTimeMillis(),
                IdUtil.fastSimpleUUID());
    }

    /**
     * 获取引擎统计信息
     */
    public EngineStatus getEngineStatus() {
        return EngineStatus.builder()
                .engineStartTime(LocalDateTime.now())
                .totalProcessedTasks(totalProcessedTasks.get())
                .successTasks(successTasks.get())
                .failedTasks(failedTasks.get())
                .cachedTasks(taskCache.size())
                .batchTasks(batchTaskCache.size())
                .uptime(System.currentTimeMillis() - engineStartTime)
                .build();
    }

    @Getter
    private static class ExportContext<T> {
        private final String taskId;
        private final ExportRequest<T> request;
        private int currentPage = 1;
        private long processedCount = 0;

        private long successCount;

        private long failedCount;

        @Setter
        private boolean hasNext = true;

        public ExportContext(String taskId, ExportRequest<T> request) {
            this.taskId = taskId;
            this.request = request;
        }

        public void nextPage() {
            currentPage++;
        }

        public void addProcessedCount(int count) {
            processedCount += count;
        }

        public void addSuccessCount(int successCount) {
            successCount += successCount;
        }

        public void addFailedCount(int failedCount) {
            failedCount += failedCount;
        }
    }

    @Data
    @Builder
    public static class EngineStatus {
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
