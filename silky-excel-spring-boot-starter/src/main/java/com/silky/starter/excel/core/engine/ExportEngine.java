package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.DataProcessor;
import com.silky.starter.excel.core.model.export.*;
import com.silky.starter.excel.core.resolve.ExcelFieldResolverPipeline;
import com.silky.starter.excel.core.resolve.ResolveContext;
import com.silky.starter.excel.core.storage.StorageObject;
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
 * 导出引擎，负责协调导出任务的整个生命周期
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public class ExportEngine {

    private static final String TEMP_FILE_PREFIX = "silky_export_";
    private static final int CACHE_CLEANUP_DELAY_MINUTES = 5;
    private static final long EXCEL_MAX_ROWS_PER_SHEET = 1048576;

    private final AtomicLong totalProcessedTasks = new AtomicLong(0);
    private final AtomicLong successTasks = new AtomicLong(0);
    private final AtomicLong failedTasks = new AtomicLong(0);
    private final ConcurrentMap<String, ExportTask<?>> taskCache = new ConcurrentHashMap<>();

    private final StorageStrategyFactory storageStrategyFactory;
    private final ExportRecordService recordService;
    private final SilkyExcelProperties properties;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final CompressionService compressionService;
    private final ExcelFieldResolverPipeline fieldResolverPipeline;

    private final StorageType defaultStorageType;
    private final AsyncType defaultAsyncType;
    private final long defaultTimeout;
    private final ScheduledExecutorService cleanupExecutor;
    private final long engineStartTime = System.currentTimeMillis();

    public ExportEngine(StorageStrategyFactory storageStrategyFactory,
                        ExportRecordService recordService,
                        SilkyExcelProperties properties,
                        ThreadPoolTaskExecutor taskExecutor,
                        CompressionService compressionService,
                        ExcelFieldResolverPipeline fieldResolverPipeline) {
        this.storageStrategyFactory = storageStrategyFactory;
        this.recordService = recordService;
        this.properties = properties;
        this.taskExecutor = taskExecutor;
        this.compressionService = compressionService;
        this.fieldResolverPipeline = fieldResolverPipeline;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "export-engine-cleanup"));
        this.defaultStorageType = properties.getStorage().getStorageType();
        this.defaultAsyncType = properties.getAsync().getAsyncType();
        this.defaultTimeout = properties.getExport().getTimeoutMinutes();
        startCacheCleanupTask();
    }

    /**
     * 同步导出
     *
     * @param task 任务
     * @param <T>  数据类型
     * @return 导出结果
     */
    public <T> ExportResult exportSync(ExportTask<T> task) {
        return processExportTask(task);
    }


    /**
     * 异步导出
     *
     * @param task 任务
     * @param <T>  数据类型
     * @return 导出结果
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
     *
     * @param sourceFile 源文件
     * @param request    请求
     * @return 压缩后的文件
     * @throws IOException IO异常
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
     * 创建临时文件
     *
     * @param task 任务
     * @return 临时文件
     */
    private <T> ExportResult processExportTask(ExportTask<T> task) {
        ExportRequest<T> request = task.getRequest();
        String taskId = task.getTaskId();
        long startTime = System.currentTimeMillis();
        File tempFile = null;
        File finalFile = null;
        try {
            validateExportRequest(request);
            cacheTask(task);
            createAndSaveExportRecord(task);
            prepareExportData(request);
            tempFile = createTempFile(request.getFileName());
            ExportResult exportResult;
            if (CollUtil.isNotEmpty(request.getSheets())) {
                exportResult = executeMultiSheetExport(request, taskId, tempFile, task.getAsyncType());
            } else {
                exportResult = executeSingleExport(request, taskId, tempFile, task.getAsyncType());
            }
            finalFile = processCompression(tempFile, request);
            StorageObject storageObject = uploadExportFile(finalFile, request);
            String fileUrl = storageObject.getUrl();
            long fileSize = storageObject.getSize();
            updateRecordOnSuccess(taskId, fileUrl, fileSize, exportResult);
            long costTime = System.currentTimeMillis() - startTime;
            successTasks.incrementAndGet();
            log.info("导出任务完成: {}, 文件URL: {}, 耗时: {}ms", taskId, fileUrl, costTime);
            return exportResult.setFileUrl(fileUrl).setFileSize(fileSize).setCostTime(costTime);
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
     * 执行多 Sheet 导出（不同数据源/表头）
     */
    private <T> ExportResult executeMultiSheetExport(ExportRequest<T> request, String taskId,
                                                     File tempFile, AsyncType asyncType) {
        try (EnhancedWriterWrapper writer = new EnhancedWriterWrapper(tempFile.getAbsolutePath(),
                getMaxRowsPerSheet(request))) {
            ResolveContext resolveContext = new ResolveContext();
            long totalRows = 0;
            for (ExportSheet<T> sheet : request.getSheets()) {
                int pageNum = 1;
                while (true) {
                    checkTaskTimeout(taskId, request.getTimeout());
                    ExportPageData<T> pageData = sheet.getDataSupplier().getPageData(
                            pageNum, request.getPageSize(), request.getParams());
                    if (CollUtil.isEmpty(pageData.getData())) {
                        break;
                    }
                    if (fieldResolverPipeline != null) {
                        fieldResolverPipeline.resolve(pageData.getData(), sheet.getDataClass(), resolveContext);
                    }
                    List<T> processedData = processPageData(pageData.getData(), request.getProcessors(), pageNum);
                    writer.write(processedData, sheet.getDataClass(), sheet.getSheetName());
                    totalRows += processedData.size();
                    pageNum++;
                    if (request.isEnableProgress()) {
                        recordService.updateProgress(taskId, totalRows, processedData.size(), 0);
                    }
                    if (!pageData.isHasNext()) {
                        break;
                    }
                }
            }
            if (asyncType.isAsync()) {
                return ExportResult.asyncSuccess(taskId);
            } else {
                return ExportResult.success(taskId)
                        .setTotalCount(totalRows).setSuccessCount(totalRows)
                        .setFailedCount(0L).setSheetCount(writer.getCurrentSheetIndex());
            }
        } catch (Exception e) {
            log.error("多Sheet导出失败: {}", taskId, e);
            throw new ExcelExportException("多Sheet导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行单数据源导出
     */
    private <T> ExportResult executeSingleExport(ExportRequest<T> request, String taskId,
                                                 File tempFile, AsyncType asyncType) {
        try (EnhancedWriterWrapper writer = new EnhancedWriterWrapper(tempFile.getAbsolutePath(),
                getMaxRowsPerSheet(request))) {
            ExportContext<T> context = new ExportContext<>(taskId, request);
            ResolveContext resolveContext = new ResolveContext();
            while (context.isHasNext()) {
                checkTaskTimeout(taskId, request.getTimeout());
                ExportPageData<T> pageData = fetchPageData(request, context.getCurrentPage());
                if (CollUtil.isEmpty(pageData.getData())) {
                    break;
                }
                if (fieldResolverPipeline != null) {
                    fieldResolverPipeline.resolve(pageData.getData(), request.getDataClass(), resolveContext);
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
                        .setFailedCount(0L).setSheetCount(writer.getCurrentSheetIndex());
            }
        } catch (Exception e) {
            log.error("导出执行失败: {}", taskId, e);
            throw new ExcelExportException("导出执行失败: " + e.getMessage(), e);
        }
    }

    private <T> void updateSingleExportProgress(ExportContext<T> context, int batchSize, boolean hasNext) {
        context.addProcessedCount(batchSize);
        context.setHasNext(hasNext);
        context.nextPage();
        recordService.updateProgress(context.getTaskId(), context.getProcessedCount(), batchSize, 0);
    }

    private void startCacheCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredCaches();
            } catch (Exception e) {
                log.error("缓存清理任务执行失败", e);
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    private void cleanupExpiredCaches() {
        long expireTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(CACHE_CLEANUP_DELAY_MINUTES);
        int before = taskCache.size();
        taskCache.entrySet().removeIf(entry -> {
            ExportTask<?> task = entry.getValue();
            return task.getFinishTime() != null && task.getFinishTime() < expireTime;
        });
        if (before != taskCache.size()) {
            log.debug("缓存清理完成: 任务缓存 {}->{}", before, taskCache.size());
        }
    }

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
     * 创建临时文件
     */
    private <T> void prepareExportData(ExportRequest<T> request) {
        Optional.ofNullable(request.getDataSupplier()).ifPresent(s -> s.prepare(request.getParams()));
        Optional.ofNullable(request.getProcessors()).ifPresent(ps -> ps.forEach(DataProcessor::prepare));
    }

    private <T> void cleanupExportData(ExportRequest<T> request) {
        Optional.ofNullable(request.getDataSupplier()).ifPresent(s -> {
            try {
                s.cleanup(request.getParams());
            } catch (Exception e) {
                log.warn("数据供应器清理异常", e);
            }
        });
        Optional.ofNullable(request.getProcessors()).ifPresent(ps -> ps.forEach(p -> {
            try {
                p.cleanup();
            } catch (Exception e) {
                log.warn("数据处理器清理异常", e);
            }
        }));
    }

    private File createTempFile(String fileName) {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            String safeFileName = fileName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
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

    private <T> StorageObject uploadExportFile(File tempFile, ExportRequest<T> request) {
        StorageType storageType = request.getStorageType() == null ? defaultStorageType : request.getStorageType();
        return storageStrategyFactory.getStrategy(storageType).storeFile(tempFile, request.getFileName(), request.getFileMetadata());
    }

    private void cleanupTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists()) {
            try {
                FileUtil.del(tempFile);
            } catch (Exception e) {
                log.warn("临时文件删除异常: {}", tempFile.getAbsolutePath(), e);
            }
        }
    }

    private void cleanupExportResources(ExportRequest<?> request, File tempFile, File finalFile, String taskId) {
        cleanupTempFile(tempFile);
        if (finalFile != null && !finalFile.equals(tempFile)) {
            cleanupTempFile(finalFile);
        }
        cleanupExportData(request);
    }

    private void updateRecordOnSuccess(String taskId, String fileUrl, long fileSize, ExportResult exportResult) {
        recordService.update(taskId, record -> {
            record.setFileUrl(fileUrl);
            record.setTotalCount(exportResult.getTotalCount());
            record.setFileSize(fileSize);
            record.setStatus(ExportStatus.COMPLETED);
        });
    }

    /**
     * 校验导出请求参数
     */
    private <T> void validateExportRequest(ExportRequest<T> request) {
        if (request == null) {
            throw new IllegalArgumentException("导出请求不能为null");
        }
        if (request.getDataClass() == null && CollUtil.isEmpty(request.getSheets())) {
            throw new IllegalArgumentException("数据类类型不能为null，且sheets不能同时为空");
        }
        if (StrUtil.isBlank(request.getFileName())) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (CollUtil.isEmpty(request.getSheets()) && request.getDataSupplier() == null) {
            throw new IllegalArgumentException("数据供应器不能为null");
        }
        long maxRows = getMaxRowsPerSheet(request);
        if (maxRows <= 0) {
            throw new IllegalArgumentException("每个Sheet最大行数必须大于0");
        }
        if (maxRows > EXCEL_MAX_ROWS_PER_SHEET) {
            throw new IllegalArgumentException(
                    String.format("每个Sheet最大行数不能超过Excel限制 %d，当前值: %d",
                            EXCEL_MAX_ROWS_PER_SHEET, maxRows));
        }
    }

    /**
     * 创建并保存导出记录
     */
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
        log.debug("第{}页数据写入完成, 数据量: {}, 当前Sheet: {}, 耗时: {}ms",
                pageNum, data.size(), writer.getCurrentSheetIndex(), System.currentTimeMillis() - startTime);
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
        return request.getDataSupplier().getPageData(pageNum, request.getPageSize(), request.getParams());
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
        taskCache.clear();
        log.info("导出引擎已关闭, 统计: 总={}, 成功={}, 失败={}",
                totalProcessedTasks.get(), successTasks.get(), failedTasks.get());
    }

    public EngineStatus getEngineStatus() {
        return EngineStatus.builder()
                .engineStartTime(LocalDateTime.now())
                .totalProcessedTasks(totalProcessedTasks.get())
                .successTasks(successTasks.get())
                .failedTasks(failedTasks.get())
                .cachedTasks(taskCache.size())
                .uptime(System.currentTimeMillis() - engineStartTime)
                .build();
    }

    @Getter
    private static class ExportContext<T> {
        private final String taskId;
        private final ExportRequest<T> request;
        private int currentPage = 1;
        private long processedCount = 0;

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
    }

    @Data
    @Builder
    public static class EngineStatus {
        private LocalDateTime engineStartTime;
        private long totalProcessedTasks;
        private long successTasks;
        private long failedTasks;
        private int cachedTasks;
        private long uptime;

        public double getSuccessRate() {
            return totalProcessedTasks > 0 ? (double) successTasks / totalProcessedTasks : 0.0;
        }

        public long getAverageProcessTime() {
            return totalProcessedTasks > 0 ? uptime / totalProcessedTasks : 0;
        }
    }
}
