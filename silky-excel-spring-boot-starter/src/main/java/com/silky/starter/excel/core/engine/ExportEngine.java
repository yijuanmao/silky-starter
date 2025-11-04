package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.core.model.export.ExportDataProcessor;
import com.silky.starter.excel.core.model.export.ExportPageData;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.enums.ExportStatus;
import com.silky.starter.excel.service.export.ExportRecordService;
import com.silky.starter.excel.service.storage.StorageService;
import lombok.Data;
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

    /**
     * 默认每个Sheet的最大行数
     */
    private static final int DEFAULT_MAX_ROWS_PER_SHEET = 200000;

    /**
     * 任务默认超时时间（分钟）
     */
    private static final int DEFAULT_TASK_TIMEOUT_MINUTES = 30;

    /**
     * 任务缓存清理延迟时间（分钟）
     */
    private static final int CACHE_CLEANUP_DELAY_MINUTES = 5;

    /**
     * 临时文件前缀
     */
    private static final String TEMP_FILE_PREFIX = "silky_export_";

    /**
     * 任务缓存表
     * 用于存储正在处理或已完成的任务信息，支持任务状态查询和恢复
     * key: taskId, value: ExportTask
     */
    private final ConcurrentMap<String, ExportTask<?>> taskCache = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleanupExecutor;

    // 统计信息
    private final AtomicLong totalProcessedTasks = new AtomicLong(0);

    private final AtomicLong successTasks = new AtomicLong(0);

    private final AtomicLong failedTasks = new AtomicLong(0);

    private final StorageService storageService;

    private final ExportRecordService recordService;

    public ExportEngine(StorageService storageService,
                        ExportRecordService recordService) {
        this.storageService = storageService;
        this.recordService = recordService;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "export-engine-cleanup")
        );
    }

    /**
     * 异步导出接口
     *
     * @param task 导出任务
     * @param <T>  导出数据类型
     * @return 导出处理结果
     */
    public <T> ExcelProcessResult exportAsync(ExportTask<T> task) {
        String taskId = task.getTaskId();
        ExportRequest<T> request = task.getRequest();

        log.info("开始处理异步导出任务: {}, 业务类型: {}", taskId, request.getBusinessType());

        validateExportRequest(request);
        task.markStart();
        cacheTask(task);

        ExportRecord record = null;
        File tempFile = null;

        try {
            // 创建并保存导出记录
            record = createAndSaveExportRecord(taskId, request);

            prepareExportData(request);

            // 创建临时文件
            tempFile = createTempFile(request.getFileName());

            // 执行导出
            ExportResult exportResult = executeExport(request, taskId, tempFile);

            // 上传导出文件
            String fileUrl = uploadExportFile(tempFile, request, taskId);

            updateRecordOnSuccess(record, fileUrl, exportResult.getTotalCount(), exportResult.getFileSize());
            task.markFinish();
            successTasks.incrementAndGet();

            log.info("导出任务处理完成: {}, 文件URL: {}, 总数据量: {}, 耗时: {}ms",
                    taskId, fileUrl, exportResult.getTotalCount(), task.getExecuteTime());

            return ExcelProcessResult.exportSuccess(taskId, fileUrl, exportResult.getTotalCount(), exportResult.getFileSize())
                    .withCostTime(task.getExecuteTime())
                    .withSheetCount(exportResult.getSheetCount());

        } catch (Exception e) {
            log.error("导出任务处理失败: {}", taskId, e);
            handleExportFailure(task, record, taskId, e);
            return ExcelProcessResult.fail(taskId, "导出失败: " + e.getMessage());

        } finally {
            cleanupExportResources(request, tempFile, taskId);
        }
    }

    /**
     * 同步导出接口
     *
     * @param request 导出请求
     * @param <T>     导出数据类型
     * @return 导出处理结果
     */
    public <T> ExcelProcessResult exportSync(ExportRequest<T> request) {
        String taskId = generateTaskId(request.getBusinessType());
        long startTime = System.currentTimeMillis();

        log.info("开始同步导出任务: {}, 业务类型: {}", taskId, request.getBusinessType());

        try {
            validateExportRequest(request);
            ExportRecord record = createAndSaveExportRecord(taskId, request);
            recordService.updateStatus(taskId, ExportStatus.PROCESSING);
            prepareExportData(request);

            File tempFile = createTempFile(request.getFileName());
            ExportResult exportResult = executeExport(request, taskId, tempFile);
            String fileUrl = uploadExportFile(tempFile, request, taskId);
            updateRecordOnSuccess(record, fileUrl, exportResult.getTotalCount(), exportResult.getFileSize());
            successTasks.incrementAndGet();

            long costTime = System.currentTimeMillis() - startTime;
            log.info("同步导出任务完成: {}, 文件URL: {}, 耗时: {}ms", taskId, fileUrl, costTime);

            return ExcelProcessResult.exportSuccess(taskId, fileUrl, exportResult.getTotalCount(), exportResult.getFileSize())
                    .withCostTime(costTime)
                    .withSheetCount(exportResult.getSheetCount());

        } catch (Exception e) {
            log.error("同步导出任务失败: {}, 业务类型: {}", taskId, request.getBusinessType(), e);
            recordService.updateFailed(taskId, "同步导出失败: " + e.getMessage());
            failedTasks.incrementAndGet();
            return ExcelProcessResult.fail(taskId, "同步导出失败: " + e.getMessage());
        } finally {
            cleanupExportData(request);
        }
    }

    /**
     * 执行导出逻辑
     *
     * @param request  导出请求
     * @param taskId   任务ID
     * @param tempFile 临时文件
     * @param <T>      导出数据类型
     * @return 导出结果
     */
    private <T> ExportResult executeExport(ExportRequest<T> request, String taskId, File tempFile) {
        try (EnhancedExcelWriter writer = new EnhancedExcelWriter(tempFile.getAbsolutePath(),
                getMaxRowsPerSheet(request))) {

            ExportContext<T> context = new ExportContext<>(taskId, request);

            while (context.hasNext()) {
                checkTaskTimeout(taskId);
                ExportPageData<T> pageData = fetchPageData(request, context.getCurrentPage());
                if (CollUtil.isEmpty(pageData.getData())) {
                    break;
                }

                List<T> processedData = processPageData(pageData.getData(), request.getProcessors(), context.getCurrentPage());
                writePageData(writer, processedData, request, context.getCurrentPage());
                updateExportProgress(context, processedData.size(), pageData.isHasNext());
            }

            return new ExportResult(context.getProcessedCount(),
                    tempFile.length(), writer.getCurrentSheet());
        }
    }


    /**
     * 获取分页数据
     *
     * @param request 导出请求
     * @param pageNum 页码
     * @param <T>     导出数据类型
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
     * 处理分页数据
     *
     * @param data       分页数据
     * @param processors 数据处理器列表
     * @param pageNum    页码
     * @param <T>        导出数据类型
     * @return 处理后的数据
     */
    private <T> List<T> processPageData(List<T> data, List<ExportDataProcessor<T>> processors, int pageNum) {
        if (CollUtil.isEmpty(processors)) {
            return data;
        }

        List<T> processedData = data;
        for (ExportDataProcessor<T> processor : processors) {
            long startTime = System.currentTimeMillis();
            processedData = processor.process(processedData, pageNum);
            long costTime = System.currentTimeMillis() - startTime;
            log.debug("数据处理器执行完成: {}, 页码: {}, 耗时: {}ms",
                    processor.getClass().getSimpleName(), pageNum, costTime);
        }
        return processedData;
    }

    /**
     * 写入分页数据到Excel
     *
     * @param writer  Excel写入器
     * @param data    分页数据
     * @param request 导出请求
     * @param pageNum 页码
     * @param <T>     导出数据类型
     */
    private <T> void writePageData(EnhancedExcelWriter writer, List<T> data,
                                   ExportRequest<T> request, int pageNum) {
        long startTime = System.currentTimeMillis();

        if (pageNum == 1) {
            writer.write(data, request.getDataClass(), request.getHeaderMapping());
        } else {
            writer.write(data, request.getDataClass());
        }

        long costTime = System.currentTimeMillis() - startTime;
        log.debug("第{}页数据写入完成, 数据量: {}, 当前Sheet: {}, 耗时: {}ms",
                pageNum, data.size(), writer.getCurrentSheet(), costTime);
    }

    /**
     * 更新导出进度
     *
     * @param context   导出上下文
     * @param batchSize 本次处理数据量
     * @param hasNext   是否有下一页
     * @param <T>       导出数据类型
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
     * 准备导出数据
     *
     * @param request 导出请求
     * @param <T>     导出数据类型
     */
    private <T> void prepareExportData(ExportRequest<T> request) {
        Optional.ofNullable(request.getDataSupplier())
                .ifPresent(supplier -> supplier.prepare(request.getParams()));

        Optional.ofNullable(request.getProcessors())
                .ifPresent(processors -> processors.forEach(ExportDataProcessor::prepare));
    }

    /**
     * 清理导出数据相关资源
     *
     * @param request 导出请求
     * @param <T>     导出数据类型
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
     * 清除临时文件
     *
     * @param fileName 临时文件名
     * @return 临时文件
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

            log.debug("临时文件创建成功: {}", filePath);
            return file;

        } catch (IOException e) {
            throw new ExcelExportException("创建临时文件异常", e);
        }
    }

    /**
     * 更新导出文件上传
     *
     * @param tempFile 临时文件
     * @param request  导出请求
     * @param taskId   任务ID
     * @param <T>      导出数据类型
     * @return 文件访问URL
     */
    private <T> String uploadExportFile(File tempFile, ExportRequest<T> request, String taskId) {
        long startTime = System.currentTimeMillis();
        String fileUrl = storageService.upload(tempFile, request.getFileName(),
                request.getStorageType(), taskId);

        log.debug("文件上传完成: {}, 耗时: {}ms", fileUrl, System.currentTimeMillis() - startTime);
        return fileUrl;
    }

    /**
     * 处理导出失败
     *
     * @param task   导出任务
     * @param record 导出记录
     * @param taskId 任务ID
     * @param e      异常信息
     */
    private void handleExportFailure(ExportTask<?> task, ExportRecord record, String taskId, Exception e) {
        task.markFinish();
        failedTasks.incrementAndGet();
        if (record != null) {
            recordService.updateFailed(taskId, e.getMessage());
        }
    }

    /**
     * 清理导出相关资源
     *
     * @param request  导出请求
     * @param tempFile 临时文件
     * @param taskId   任务ID
     */
    private void cleanupExportResources(ExportRequest<?> request, File tempFile, String taskId) {
        cleanupExportData(request);
        cleanupTempFile(tempFile);
        scheduleTaskCacheCleanup(taskId);
    }

    /**
     * 清理临时文件
     *
     * @param tempFile 临时文件
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
     * 检查任务是否超时
     *
     * @param taskId 任务ID
     */
    private void checkTaskTimeout(String taskId) {
        ExportTask<?> task = taskCache.get(taskId);
        if (task != null && task.isTimeout(DEFAULT_TASK_TIMEOUT_MINUTES)) {
            throw new ExcelExportException("任务执行超时，已中断");
        }
    }

    /**
     * 创建并保存导出记录
     *
     * @param taskId  任务ID
     * @param request 导出请求
     * @param <T>     导出数据类型
     * @return 导出记录
     */
    private <T> ExportRecord createAndSaveExportRecord(String taskId, ExportRequest<T> request) {
        ExportRecord record = ExportRecord.builder()
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

        recordService.save(record);
        return record;
    }

    /**
     * 更新导出记录为成功
     *
     * @param record     导出记录
     * @param fileUrl    文件访问URL
     * @param totalCount 总记录数
     * @param fileSize   文件大小
     */
    private void updateRecordOnSuccess(ExportRecord record, String fileUrl, long totalCount, long fileSize) {
        recordService.update(record.getTaskId(), r -> {
            r.setFileUrl(fileUrl);
            r.setTotalCount(totalCount);
            r.setFileSize(fileSize);
            r.setStatus(ExportStatus.COMPLETED);
        });
    }

    /**
     * 缓存任务
     *
     * @param task 导出任务
     * @param <T>  导出数据类型
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
     * 获取每个Sheet的最大行数
     *
     * @param request 导出请求
     * @param <T>     导出数据类型
     * @return 每个Sheet的最大行数
     */
    private <T> long getMaxRowsPerSheet(ExportRequest<T> request) {
        return Optional.of(request.getMaxRowsPerSheet())
                .orElse((long) DEFAULT_MAX_ROWS_PER_SHEET);
    }

    /**
     * 校验导出请求
     *
     * @param request 请求参数
     * @return 任务ID
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

    private String generateTaskId(String businessType) {
        String prefix = StrUtil.isNotBlank(businessType) ?
                businessType.replaceAll("[^a-zA-Z0-9]", "_") : "TASK";
        return prefix + "_" + System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID();
    }


    private static class ExportContext<T> {
        @Getter
        private final String taskId;
        @Getter
        private final ExportRequest<T> request;
        @Getter
        private int currentPage = 1;
        @Getter
        private long processedCount = 0;
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

        public void setHasNext(boolean hasNext) {
            this.hasNext = hasNext;
        }

        public boolean hasNext() {
            return hasNext;
        }

    }

    @Data
    private static class ExportResult {
        private final long totalCount;
        private final long fileSize;
        private final int sheetCount;

        public ExportResult(long totalCount, long fileSize, int sheetCount) {
            this.totalCount = totalCount;
            this.fileSize = fileSize;
            this.sheetCount = sheetCount;
        }
    }
}
