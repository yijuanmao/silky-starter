package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.DataProcessor;
import com.silky.starter.excel.core.model.export.*;
import com.silky.starter.excel.core.resolve.ExcelFieldResolverPipeline;
import com.silky.starter.excel.core.resolve.ResolveCellWriteHandler;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 导出引擎，负责协调导出任务的整个生命周期
 *
 * @author zy
 * @since 1.1.0
 */
@Slf4j
public class ExportEngine extends AbstractExcelEngine {

    private static final String TEMP_FILE_PREFIX = "silky_export_";
    private static final long EXCEL_MAX_ROWS_PER_SHEET = 1048576;

    /**
     * 导出任务缓存
     */
    private final ConcurrentMap<String, ExportTask<?>> taskCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 存储策略工厂
     */
    private final StorageStrategyFactory storageStrategyFactory;
    /**
     * 导出记录服务
     */
    private final ExportRecordService recordService;
    /**
     * 配置属性
     */
    private final SilkyExcelProperties properties;
    /**
     * 异步任务线程池
     */
    private final ThreadPoolTaskExecutor taskExecutor;
    /**
     * 压缩服务
     */
    private final CompressionService compressionService;
    /**
     * 字段解析管道
     */
    private final ExcelFieldResolverPipeline fieldResolverPipeline;

    /**
     * 默认存储类型
     */
    private final StorageType defaultStorageType;
    /**
     * 默认异步类型
     */
    private final AsyncType defaultAsyncType;
    /**
     * 默认超时时间（分钟）
     */
    private final long defaultTimeout;

    /**
     * 构造函数（使用共享清理执行器）
     */
    public ExportEngine(StorageStrategyFactory storageStrategyFactory,
                        ExportRecordService recordService,
                        SilkyExcelProperties properties,
                        ThreadPoolTaskExecutor taskExecutor,
                        CompressionService compressionService,
                        ExcelFieldResolverPipeline fieldResolverPipeline,
                        ScheduledExecutorService sharedCleanupExecutor) {
        super("导出引擎", sharedCleanupExecutor);
        this.storageStrategyFactory = storageStrategyFactory;
        this.recordService = recordService;
        this.properties = properties;
        this.taskExecutor = taskExecutor;
        this.compressionService = compressionService;
        this.fieldResolverPipeline = fieldResolverPipeline;
        this.defaultStorageType = properties.getStorage().getStorageType();
        this.defaultAsyncType = properties.getAsync().getAsyncType();
        this.defaultTimeout = properties.getExport().getTimeoutMinutes();
    }

    /**
     * 同步导出
     *
     * @param task 导出任务
     * @param <T>  数据类型
     * @return 导出结果
     */
    public <T> ExportResult exportSync(ExportTask<T> task) {
        return processExportTask(task);
    }

    /**
     * 异步导出（提交到线程池执行）
     *
     * @param task 导出任务
     * @param <T>  数据类型
     * @return 提交结果
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
     * 处理导出任务主流程
     *
     * @param task 导出任务
     * @param <T>  数据类型
     * @return 导出结果
     */
    private <T> ExportResult processExportTask(ExportTask<T> task) {
        ExportRequest<T> request = task.getRequest();
        String taskId = task.getTaskId();
        long startTime = System.currentTimeMillis();
        File tempFile = null;
        File finalFile = null;
        try {
            validateExportRequest(request);
            taskCache.put(taskId, task);
            createAndSaveExportRecord(task);
            prepareExportData(request);
            tempFile = createTempFile(request.getFileName());

            // 统一导出：单Sheet和多Sheet共用同一方法
            ExportResult exportResult = executeExport(request, taskId, tempFile, task.getAsyncType());

            // 处理压缩
            if (request.isCompressionEnabled()) {
                finalFile = compressFile(tempFile, request);
            }
            StorageObject storageObject = uploadExportFile(finalFile != null ? finalFile : tempFile, request);
            String fileUrl = storageObject.getUrl();
            long fileSize = storageObject.getSize();
            updateRecordOnSuccess(taskId, fileUrl, fileSize, exportResult);
            long costTime = System.currentTimeMillis() - startTime;
            incrementSuccess();
            log.debug("导出任务完成: {}, 文件URL: {}, 耗时: {}ms", taskId, fileUrl, costTime);
            return exportResult.setFileUrl(fileUrl).setFileSize(fileSize).setCostTime(costTime);
        } catch (Exception e) {
            log.error("导出任务失败: {}", taskId, e);
            incrementFailed();
            recordService.updateFailed(taskId, "导出失败: " + e.getMessage());
            return ExportResult.fail(taskId, "导出失败: " + e.getMessage());
        } finally {
            cleanupExportResources(request, tempFile, finalFile);
            incrementTotalProcessed();
        }
    }

    /**
     * 统一执行导出（合并单Sheet和多Sheet逻辑）
     *
     * @param request   导出请求
     * @param taskId    任务ID
     * @param tempFile  临时文件
     * @param asyncType 异步类型
     * @param <T>       数据类型
     * @return 导出结果
     */
    private <T> ExportResult executeExport(ExportRequest<T> request, String taskId,
                                           File tempFile, AsyncType asyncType) {
        ResolveCellWriteHandler resolveHandler = (fieldResolverPipeline != null)
                ? new ResolveCellWriteHandler() : null;
        try (EnhancedWriterWrapper writer = new EnhancedWriterWrapper(tempFile.getAbsolutePath(),
                getMaxRowsPerSheet(request), resolveHandler)) {
            ResolveContext resolveContext = new ResolveContext();

            // 构建统一的Sheet列表
            List<SheetExportContext<T>> sheetContexts = buildSheetContexts(request);
            long totalRows = 0;

            for (SheetExportContext<T> sheetCtx : sheetContexts) {
                int pageNum = 1;
                while (true) {
                    checkTaskTimeout(taskId, request.getTimeout());
                    ExportPageData<T> pageData = sheetCtx.dataSupplier.getPageData(
                            pageNum, request.getPageSize(), request.getParams());
                    if (CollUtil.isEmpty(pageData.getData())) {
                        break;
                    }
                    // 字段解析
                    if (fieldResolverPipeline != null) {
                        fieldResolverPipeline.resolve(pageData.getData(), sheetCtx.dataClass, resolveContext);
                        resolveHandler.setCurrentPageData(pageData.getData(), fieldResolverPipeline);
                    }
                    // 数据处理器
                    List<T> processedData = processPageData(pageData.getData(), request.getProcessors());
                    // 写入Excel
                    writer.write(processedData, sheetCtx.dataClass, sheetCtx.sheetName);
                    // 清理旁路存储
                    if (fieldResolverPipeline != null) {
                        fieldResolverPipeline.clearResolvedValues(processedData);
                    }
                    totalRows += processedData.size();

                    // 更新进度
                    if (request.isEnableProgress()) {
                        recordService.updateProgress(taskId, totalRows, processedData.size(), 0);
                    }
                    pageNum++;
                    if (!pageData.isHasNext()) {
                        break;
                    }
                }
            }

            // 返回结果
            if (asyncType != null && asyncType.isAsync()) {
                return ExportResult.asyncSuccess(taskId);
            }
            return ExportResult.success(taskId)
                    .setTotalCount(totalRows).setSuccessCount(totalRows)
                    .setFailedCount(0L).setSheetCount(writer.getCurrentSheetIndex());
        } catch (Exception e) {
            log.error("导出执行失败: {}", taskId, e);
            throw new ExcelExportException("导出执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建Sheet导出上下文列表（统一单Sheet和多Sheet）
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return Sheet上下文列表
     */
    private <T> List<SheetExportContext<T>> buildSheetContexts(ExportRequest<T> request) {
        List<SheetExportContext<T>> contexts = new ArrayList<>();
        if (CollUtil.isNotEmpty(request.getSheets())) {
            // 多Sheet模式
            for (ExportSheet<T> sheet : request.getSheets()) {
                contexts.add(new SheetExportContext<>(
                        sheet.getSheetName(), sheet.getDataClass(), sheet.getDataSupplier()));
            }
        } else {
            // 单Sheet模式
            contexts.add(new SheetExportContext<>(
                    "数据", request.getDataClass(), request.getDataSupplier()));
        }
        return contexts;
    }

    /**
     * Sheet导出上下文
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

    /**
     * 处理页面数据（执行数据处理器链）
     *
     * @param data       原始数据
     * @param processors 处理器列表
     * @param <T>        数据类型
     * @return 处理后的数据
     */
    private <T> List<T> processPageData(List<T> data, List<DataProcessor<T>> processors) {
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
     * 压缩导出文件
     *
     * @param sourceFile 源文件
     * @param request    导出请求
     * @return 压缩后的文件
     */
    private <T> File compressFile(File sourceFile, ExportRequest<T> request) throws IOException {
        SilkyExcelProperties.CompressionConfig config = SilkyExcelProperties.CompressionConfig.builder()
                .enabled(true)
                .type(request.getCompressionType())
                .compressionLevel(request.getCompressionLevel())
                .splitLargeFiles(request.isSplitLargeFiles())
                .splitSize(request.getSplitSize())
                .build();
        String compressedPath = sourceFile.getAbsolutePath() + "_compressed";
        return compressionService.compressFile(sourceFile, config, compressedPath);
    }

    /**
     * 准备导出数据（调用数据供应器和处理器的prepare方法）
     *
     * @param request 导出请求
     */
    private <T> void prepareExportData(ExportRequest<T> request) {
        Optional.ofNullable(request.getDataSupplier()).ifPresent(s -> s.prepare(request.getParams()));
        Optional.ofNullable(request.getProcessors()).ifPresent(ps -> ps.forEach(DataProcessor::prepare));
    }

    /**
     * 清理导出数据资源
     *
     * @param request 导出请求
     */
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

    /**
     * 创建临时导出文件
     *
     * @param fileName 文件名
     * @return 临时文件
     */
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

    /**
     * 上传导出文件到存储
     *
     * @param tempFile 临时文件
     * @param request  导出请求
     * @return 存储对象
     */
    private <T> StorageObject uploadExportFile(File tempFile, ExportRequest<T> request) {
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
     * 清理所有导出资源
     *
     * @param request   导出请求
     * @param tempFile  临时文件
     * @param finalFile 最终文件（压缩后）
     */
    private void cleanupExportResources(ExportRequest<?> request, File tempFile, File finalFile) {
        cleanupTempFile(tempFile);
        if (finalFile != null && !finalFile.equals(tempFile)) {
            cleanupTempFile(finalFile);
        }
        cleanupExportData(request);
    }

    /**
     * 更新导出记录为成功状态
     *
     * @param taskId       任务ID
     * @param fileUrl      文件URL
     * @param fileSize     文件大小
     * @param exportResult 导出结果
     */
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
     *
     * @param request 导出请求
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
        // 单Sheet模式下数据供应器不能为空
        if (CollUtil.isEmpty(request.getSheets()) && request.getDataSupplier() == null) {
            throw new IllegalArgumentException("数据供应器不能为null");
        }
        // 多Sheet模式下各Sheet数据供应器不能为空
        if (CollUtil.isNotEmpty(request.getSheets())) {
            for (ExportSheet<T> sheet : request.getSheets()) {
                if (sheet.getDataSupplier() == null) {
                    throw new IllegalArgumentException("Sheet[" + sheet.getSheetName() + "]的数据供应器不能为null");
                }
            }
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
     *
     * @param task 导出任务
     * @return 导出记录
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
     * 获取最大行数限制（优先使用请求中的值，其次使用全局配置）
     *
     * @param request 导出请求
     * @return 每Sheet最大行数
     */
    private <T> long getMaxRowsPerSheet(ExportRequest<T> request) {
        return Objects.isNull(request.getMaxRowsPerSheet()) ? properties.getExport().getMaxRowsPerSheet() : request.getMaxRowsPerSheet();
    }

    /**
     * 检查任务是否超时
     *
     * @param taskId  任务ID
     * @param timeout 超时时间（分钟）
     */
    private void checkTaskTimeout(String taskId, Long timeout) {
        ExportTask<?> task = taskCache.get(taskId);
        if (task != null && task.isTimeout(Objects.isNull(timeout) ? defaultTimeout : timeout)) {
            throw new ExcelExportException("导出任务执行超时，已中断");
        }
    }

    @Override
    protected void cleanupExpiredCaches() {
        long expireTime = System.currentTimeMillis() - java.util.concurrent.TimeUnit.MINUTES.toMillis(5);
        cleanExpiredEntries(taskCache, task -> {
            Long finishTime = ((ExportTask<?>) task).getFinishTime();
            return finishTime != null && finishTime < expireTime;
        });
    }

    @Override
    protected int getCacheSize() {
        return taskCache.size();
    }

    @Override
    public void shutdown() {
        log.info("开始关闭导出引擎...");
        shutdownCleanupExecutor();
        taskCache.clear();
        log.info("导出引擎已关闭, 统计: 总={}, 成功={}, 失败={}",
                totalProcessed.get(), successCount.get(), failedCount.get());
    }

    /**
     * 获取引擎状态信息
     *
     * @return 引擎状态
     */
    public EngineStatus getEngineStatus() {
        Object[] data = buildStatusData();
        return EngineStatus.builder()
                .engineStartTime((LocalDateTime) data[0])
                .totalProcessedTasks((long) data[1])
                .successTasks((long) data[2])
                .failedTasks((long) data[3])
                .cachedTasks((int) data[4])
                .uptime((long) data[5])
                .build();
    }

    /**
     * 导出上下文（单Sheet模式翻页控制）
     */
    @Getter
    private static class ExportContext<T> {
        private final String taskId;
        private final ExportRequest<T> request;
        private int currentPage = 1;
        private long processedCount = 0;

        @Setter
        private boolean hasNext = true;

        ExportContext(String taskId, ExportRequest<T> request) {
            this.taskId = taskId;
            this.request = request;
        }

        void nextPage() {
            currentPage++;
        }

        void addProcessedCount(int count) {
            processedCount += count;
        }
    }

    /**
     * 引擎状态
     */
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
         * 成功任务数
         */
        private long successTasks;
        /**
         * 失败任务数
         */
        private long failedTasks;
        /**
         * 当前缓存任务数
         */
        private int cachedTasks;
        /**
         * 运行时长（毫秒）
         */
        private long uptime;

        /**
         * 计算成功率
         */
        public double getSuccessRate() {
            return totalProcessedTasks > 0 ? (double) successTasks / totalProcessedTasks : 0.0;
        }

        /**
         * 计算平均处理时间
         */
        public long getAverageProcessTime() {
            return totalProcessedTasks > 0 ? uptime / totalProcessedTasks : 0;
        }
    }
}
