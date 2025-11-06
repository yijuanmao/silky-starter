package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.export.*;
import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.ExportStatus;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.service.export.ExportRecordService;
import com.silky.starter.excel.service.storage.StorageService;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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

    /**
     * 缓存清理延迟分钟数
     */
    private static final int CACHE_CLEANUP_DELAY_MINUTES = 5;

    /**
     * 成功任务数
     */
    private final AtomicLong successTasks = new AtomicLong(0);

    /**
     * 失败任务数
     */
    private final AtomicLong failedTasks = new AtomicLong(0);

    private final AtomicLong totalProcessedTasks = new AtomicLong(0);

    private final ConcurrentMap<String, ExportTask<?>> taskCache = new ConcurrentHashMap<>();

    private final StorageService storageService;

    private final ExportRecordService recordService;

    private final SilkyExcelProperties properties;

    private final ScheduledExecutorService cleanupExecutor;

    public ExportEngine(StorageService storageService,
                        ExportRecordService recordService,
                        SilkyExcelProperties properties) {
        this.storageService = storageService;
        this.recordService = recordService;
        this.properties = properties;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "export-engine-cleanup")
        );
    }

    /**
     * 同步导出
     */
    public <T> ExportResult exportSync(ExportTask<T> task) {
        ExportRequest<T> request = task.getRequest();
        String taskId = task.getTaskId();
        long startTime = System.currentTimeMillis();

        log.debug("开始同步导出任务: {}, 业务类型: {}", taskId, request.getBusinessType());

        File tempFile = null;
        try {
            // 参数验证
            validateExportRequest(request);

            // 缓存任务
            cacheTask(task);

            // 创建并保存导出记录
            createAndSaveExportRecord(task);

            // 准备数据
            prepareExportData(request);

            // 创建临时文件
            tempFile = createTempFile(request.getFileName());

            // 执行导出
            ExportResult exportResult = this.executeExport(request, taskId, tempFile, task.getAsyncType());

            // 上传文件
            String fileUrl = uploadExportFile(tempFile, request, taskId);

            // 更新记录
            updateRecordOnSuccess(taskId, fileUrl, exportResult);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("同步导出任务完成: {}, 文件URL: {}, 耗时: {}ms", taskId, fileUrl, costTime);

            return exportResult.setFileUrl(fileUrl)
                    .setFileSize(tempFile.length())
                    .setCostTime(costTime);

        } catch (Exception e) {
            log.error("同步导出任务失败: {}", taskId, e);
            recordService.updateFailed(taskId, "导出失败: " + e.getMessage());
            throw new ExcelExportException(e.getMessage(), e);
        } finally {
            // 清理资源
            cleanupExportResources(request, tempFile, taskId);
        }
    }

    /**
     * 关闭引擎，释放资源
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("导出引擎已关闭");
    }

    /**
     * 执行导出逻辑
     *
     * @param request  导出请求
     * @param taskId   任务ID
     * @param tempFile 临时文件
     */
    private <T> ExportResult executeExport(ExportRequest<T> request, String taskId, File tempFile, AsyncType asyncType) {
        try (EnhancedExcelWriter writer = new EnhancedExcelWriter(tempFile.getAbsolutePath(), this.getMaxRowsPerSheet(request))) {

            ExportContext<T> context = new ExportContext<>(taskId, request);

            while (context.isHasNext()) {

                // 检查任务是否超时
                this.checkTaskTimeout(taskId);

                // 获取分页数据
                ExportPageData<T> pageData = this.fetchPageData(request, context.getCurrentPage());
                if (CollUtil.isEmpty(pageData.getData())) {
                    break;
                }
                // 处理数据
                List<T> processedData = processPageData(pageData.getData(), request.getProcessors(), context.getCurrentPage());
                // 写入数据
                writePageData(writer, processedData, request, context.getCurrentPage());
                // 更新进度
                this.updateExportProgress(context, processedData.size(), pageData.isHasNext());

                successTasks.getAndAdd(processedData.size());
            }
            if (asyncType.isAsync()) {
                return ExportResult.asyncSuccess(taskId);
            } else {
                return ExportResult.success(taskId)
                        .setTotalCount(writer.getTotalRows().get())
                        .setSuccessCount(successTasks.get())
                        .setFailedCount(failedTasks.get())
                        .setSheetCount(writer.getCurrentSheetIndex());
            }
        } catch (Exception e) {
            log.error("导出执行失败: {}", taskId, e);
            failedTasks.incrementAndGet();
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
     */
    private <T> String uploadExportFile(File tempFile, ExportRequest<T> request, String taskId) {
        return storageService.upload(tempFile, request.getFileName(),
                request.getStorageType(), taskId);
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(File tempFile) {
        if (!properties.getStorage().getLocal().isAutoClean()) {
            log.debug("本地存储临时文件不自动删除: {}", tempFile != null ? tempFile.getAbsolutePath() : "null");
            return;
        }
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
     * 创建并保存导出记录
     *
     * @param task 任务
     */
    private <T> ExportRecord createAndSaveExportRecord(ExportTask<T> task) {
        String taskId = task.getTaskId();
        ExportRequest<T> request = task.getRequest();
        ExportRecord record = ExportRecord.builder()
                .taskId(taskId)
                .businessType(request.getBusinessType())
                .fileName(request.getFileName())
                .storageType(request.getStorageType())
                .asyncType(task.getAsyncType())
                .createUser(request.getCreateUser())
                .status(ExportStatus.PROCESSING)
                .createTime(LocalDateTime.now())
                .params(request.getParams())
                .totalCount(0L)
                .processedCount(0L)
                .build();

        recordService.save(record);
        return record;
    }

    /**
     * 获取最大行数
     */
    private <T> long getMaxRowsPerSheet(ExportRequest<T> request) {
        return Objects.isNull(request.getMaxRowsPerSheet()) ? properties.getExport().getMaxRowsPerSheet() : request.getMaxRowsPerSheet();
    }

    /**
     * 检查任务是否超时
     *
     * @param taskId 任务ID
     */
    private void checkTaskTimeout(String taskId) {
        ExportTask<?> task = taskCache.get(taskId);
        if (task != null && task.isTimeout(properties.getExport().getTimeoutMinutes())) {
            throw new ExcelExportException("任务执行超时，已中断");
        }
    }

    /**
     * 缓存任务
     *
     * @param task 任务
     */
    private <T> void cacheTask(ExportTask<T> task) {
        taskCache.put(task.getTaskId(), task);
        totalProcessedTasks.incrementAndGet();
        log.debug("任务已缓存: {}", task.getTaskId());
    }

    /**
     * 调度任务缓存清理
     *
     * @param taskId 任务ID
     */
    private void scheduleTaskCacheCleanup(String taskId) {
        cleanupExecutor.schedule(() -> {
            taskCache.remove(taskId);
            log.debug("任务缓存已清理: {}", taskId);
        }, CACHE_CLEANUP_DELAY_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 更新导出进度
     *
     * @param context   导出上下文
     * @param batchSize 批量大小
     * @param hasNext   是否有下一页
     */
    private <T> void updateExportProgress(ExportContext<T> context, int batchSize, boolean hasNext) {
        context.addProcessedCount(batchSize);
        context.setHasNext(hasNext);
        context.nextPage();
        recordService.updateProgress(context.getTaskId(), context.getProcessedCount());

        log.debug("导出进度更新: 任务ID={}, 当前页={}, 已处理={}, 还有下一页={}",
                context.getTaskId(), context.getCurrentPage(), context.getProcessedCount(), hasNext);
    }

    /**
     * 写入分页数据
     *
     * @param writer  Excel写入器
     * @param data    数据
     * @param request 导出请求
     * @param pageNum 页码
     * @param <T>     数据类型
     */
    private <T> void writePageData(EnhancedExcelWriter writer, List<T> data,
                                   ExportRequest<T> request, int pageNum) {
        long startTime = System.currentTimeMillis();

        writer.write(data, request.getDataClass());

        long costTime = System.currentTimeMillis() - startTime;
        log.debug("第{}页数据写入完成, 数据量: {}, 当前Sheet: {}, 耗时: {}ms",
                pageNum, data.size(), writer.getCurrentSheetIndex(), costTime);
    }

    /**
     * 获取分页数据
     *
     * @param request 导出请求
     * @param pageNum 页码
     * @param <T>     数据类型
     * @return 分页数据
     */
    private <T> ExportPageData<T> fetchPageData(ExportRequest<T> request, int pageNum) {
        long startTime = System.currentTimeMillis();
        ExportPageData<T> pageData = request.getDataSupplier().getPageData(
                pageNum, request.getPageSize(), request.getParams());

        log.debug("第{}页数据查询完成, 数据量: {}, 耗时: {}ms",
                pageNum, CollUtil.size(pageData.getData()), System.currentTimeMillis() - startTime);

        return pageData;
    }

    /**
     * 清理导出相关资源
     */
    private void cleanupExportResources(ExportRequest<?> request, File tempFile, String taskId) {
        cleanupTempFile(tempFile);
        cleanupExportData(request);
        scheduleTaskCacheCleanup(taskId);
    }


    /**
     * 导出上下文
     */

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
        /**
         * 引擎启动时间
         */
        private LocalDateTime engineStartTime;

        /**
         * 总处理任务数
         */
        private long totalProcessedTasks;

        /**
         * 成功处理任务数
         */
        private long successTasks;

        /**
         * 失败处理任务数
         */
        private long failedTasks;

        /**
         * 缓存中的任务数
         */
        private int cachedTasks;

        /**
         * 运行时间（毫秒）
         */
        private long uptime;

        public double getSuccessRate() {
            return totalProcessedTasks > 0 ? (double) successTasks / totalProcessedTasks : 0.0;
        }

        public double getFailureRate() {
            return totalProcessedTasks > 0 ? (double) failedTasks / totalProcessedTasks : 0.0;
        }

        public long getAverageProcessTime() {
            return totalProcessedTasks > 0 ? uptime / totalProcessedTasks : 0;
        }
    }
}
