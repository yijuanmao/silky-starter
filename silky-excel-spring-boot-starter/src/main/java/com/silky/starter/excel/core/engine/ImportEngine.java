package com.silky.starter.excel.core.engine;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.AnalysisListenersContext;
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
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 导入引擎核心类，负责协调整个导入流程，包括文件下载、数据读取、数据处理和数据导入
 *
 * @author zy
 * @since 1.0.0
 */
@Slf4j
public class ImportEngine extends AbstractExcelEngine {

    /**
     * 导入任务缓存
     */
    private final ConcurrentMap<String, ImportTask<?>> taskCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 导入记录服务
     */
    private final ImportRecordService recordService;
    /**
     * 异步任务线程池
     */
    private final ThreadPoolTaskExecutor taskExecutor;
    /**
     * 压缩服务
     */
    private final CompressionService compressionService;
    /**
     * 存储策略工厂
     */
    private final StorageStrategyFactory storageStrategyFactory;
    /**
     * 配置属性
     */
    private final SilkyExcelProperties silkyExcelProperties;

    /**
     * 默认存储类型
     */
    private final StorageType defaultStorageType;

    /**
     * 构造函数（使用共享清理执行器）
     */
    public ImportEngine(ImportRecordService recordService,
                        ThreadPoolTaskExecutor taskExecutor,
                        CompressionService compressionService,
                        StorageStrategyFactory storageStrategyFactory,
                        SilkyExcelProperties properties,
                        ScheduledExecutorService sharedCleanupExecutor) {
        super("导入引擎", sharedCleanupExecutor);
        this.recordService = recordService;
        this.taskExecutor = taskExecutor;
        this.compressionService = compressionService;
        this.storageStrategyFactory = storageStrategyFactory;
        this.silkyExcelProperties = properties;
        this.defaultStorageType = properties.getStorage().getStorageType();
    }

    /**
     * 异步导入单个任务
     *
     * @param task 导入任务
     * @param <T>  数据类型
     * @return 提交结果
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
     *
     * @param request 导入请求
     * @param <T>     数据类型
     * @return 导入结果
     */
    public <T> ImportResult importSync(ImportRequest<T> request) {
        ImportTask<T> task = createImportTask(request);
        return processImportTask(task);
    }

    /**
     * 处理单个导入任务的主流程
     *
     * @param task 导入任务
     * @param <T>  数据类型
     * @return 导入结果
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
            taskCache.put(taskId, task);

            ImportRecord record = createImportRecord(taskId, request);
            recordService.addImportRecord(record);

            // 下载文件
            downloadedFile = downloadImportFile(request);
            // 解压
            decompressedFile = decompressFile(downloadedFile, request);
            // 准备导入器
            prepareDataImporter(request);

            // 执行导入
            ImportResult result = doImport(request, taskId, decompressedFile, task.getAsyncType());
            long costTime = System.currentTimeMillis() - startTime;

            log.info("导入任务完成: {}, 结果: {}, 总耗时: {}ms", taskId, result.getSummary(), costTime);

            if (result.isSuccess()) {
                incrementSuccess();
            } else {
                incrementFailed();
            }

            return result.withCostTime(costTime);

        } catch (Exception e) {
            log.error("导入任务失败: {}, 业务类型: {}", taskId, request.getBusinessType(), e);
            incrementFailed();
            recordService.updateFail(taskId, "导入失败: " + e.getMessage());
            return ImportResult.fail(taskId, "导入失败: " + e.getMessage());
        } finally {
            cleanupDataImporter(request);
            cleanupTempFiles(downloadedFile, decompressedFile);
            incrementTotalProcessed();
        }
    }

    /**
     * 解压导入文件
     *
     * @param sourceFile 源文件
     * @param request    导入请求
     * @return 解压后的文件
     */
    private <T> File decompressFile(File sourceFile, ImportRequest<T> request) throws IOException {
        if (!request.isCompressionEnabled()) {
            return sourceFile;
        }
        SilkyExcelProperties.CompressionConfig config = SilkyExcelProperties.CompressionConfig.builder()
                .enabled(true)
                .type(request.getCompressionType())
                .compressionLevel(request.getCompressionLevel())
                .splitLargeFiles(request.isSplitLargeFiles())
                .splitSize(request.getSplitSize())
                .build();
        String decompressedPath = sourceFile.getAbsolutePath() + "_decompressed";
        return compressionService.decompressFile(sourceFile, config, decompressedPath);
    }

    /**
     * 执行数据导入（读取Excel并写入目标系统）
     *
     * @param request   导入请求
     * @param taskId    任务ID
     * @param tempFile  临时文件
     * @param asyncType 异步类型
     * @param <T>       数据类型
     * @return 导入结果
     */
    private <T> ImportResult doImport(ImportRequest<T> request, String taskId, File tempFile, AsyncType asyncType) {
        AnalysisListenersContext<T> context = AnalysisListenersContext.<T>builder()
                .maxErrorCount(getMaxErrorCount(request.getMaxErrorCount()))
                .pageSize(getPageSize(request.getPageSize()))
                .request(request)
                .TotalCount(0)
                .SuccessCount(0)
                .FailCount(0)
                .build();

        try (ExcelReaderWrapper<T> reader = new ExcelReaderWrapper<>(tempFile.getAbsolutePath(), request.isSkipHeader(), context)) {
            long startImportTime = System.currentTimeMillis();
            long skippedCount = 0;

            // 开始事务
            if (request.isEnableTransaction()) {
                request.getDataImporterSupplier().beginTransaction();
            }

            // 检查超时
            if (isTaskTimeout(taskId)) {
                throw new ExcelExportException("任务执行超时，已中断");
            }

            // 读取Excel数据
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
            log.debug("数据处理完成: 数据量={}, 成功={}, 失败={}, 耗时={}ms",
                    totalCount, successCount, failedCount, System.currentTimeMillis() - startImportTime);

            // 提交事务
            if (request.isEnableTransaction()) {
                request.getDataImporterSupplier().commitTransaction();
            }

            // 构建结果
            if (failedCount == 0) {
                return ImportResult.success(taskId, totalCount, successCount)
                        .withCostTime(System.currentTimeMillis() - startImportTime);
            } else {
                return ImportResult.partialSuccess(taskId, totalCount, successCount, failedCount, reader.getAllErrors())
                        .withCostTime(System.currentTimeMillis() - startImportTime)
                        .withSkippedCount(skippedCount);
            }
        } catch (Exception e) {
            // 回滚事务
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
     * 清理数据导入器资源
     *
     * @param request 导入请求
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

    /**
     * 校验导入请求参数
     *
     * @param request 导入请求
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
     * 创建导入记录
     *
     * @param taskId  任务ID
     * @param request 导入请求
     * @return 导入记录
     */
    private <T> ImportRecord createImportRecord(String taskId, ImportRequest<T> request) {
        return ImportRecord.builder()
                .taskId(taskId)
                .businessType(request.getBusinessType())
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .storageType(Objects.isNull(request.getStorageType()) ? defaultStorageType : request.getStorageType())
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

    /**
     * 生成任务ID
     *
     * @param businessType 业务类型
     * @return 任务ID
     */
    private String generateTaskId(String businessType) {
        String prefix = StrUtil.isNotBlank(businessType) ?
                businessType.replaceAll("[^a-zA-Z0-9]", "_") : "IMPORT";
        return prefix + "_" + System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID();
    }

    /**
     * 创建导入任务
     *
     * @param request 导入请求
     * @param <T>     数据类型
     * @return 导入任务
     */
    private <T> ImportTask<T> createImportTask(ImportRequest<T> request) {
        ImportTask<T> task = new ImportTask<>();
        task.setRequest(request);
        task.setTaskId(generateTaskId(request.getBusinessType()));
        return task;
    }

    /**
     * 获取最大错误数量
     *
     * @param maxErrorCount 请求中的最大错误数
     * @return 最大错误数量
     */
    private int getMaxErrorCount(Integer maxErrorCount) {
        return Objects.isNull(maxErrorCount) ? silkyExcelProperties.getImports().getMaxErrorCount() : maxErrorCount;
    }

    /**
     * 获取分页大小
     *
     * @param pageSize 请求中的分页大小
     * @return 分页大小
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
        return task != null && task.isTimeout();
    }

    @Override
    protected void cleanupExpiredCaches() {
        long expireTime = System.currentTimeMillis() - java.util.concurrent.TimeUnit.MINUTES.toMillis(5);
        cleanExpiredEntries(taskCache, task -> {
            Long finishTime = ((ImportTask<?>) task).getFinishTime();
            return finishTime != null && finishTime < expireTime;
        });
    }

    @Override
    protected int getCacheSize() {
        return taskCache.size();
    }

    @Override
    public void shutdown() {
        log.info("开始关闭导入引擎...");
        shutdownCleanupExecutor();
        taskCache.clear();
        log.info("导入引擎已关闭, 统计: 总={}, 成功={}, 失败={}",
                totalProcessed.get(), successCount.get(), failedCount.get());
    }

    /**
     * 获取引擎状态信息
     *
     * @return 引擎状态
     */
    public ImportEngineStatus getEngineStatus() {
        Object[] data = buildStatusData();
        return ImportEngineStatus.builder()
                .engineStartTime((LocalDateTime) data[0])
                .totalProcessedTasks((long) data[1])
                .successTasks((long) data[2])
                .failedTasks((long) data[3])
                .cachedTasks((int) data[4])
                .uptime((long) data[5])
                .build();
    }

    /**
     * 导入引擎状态
     */
    @Data
    @Builder
    public static class ImportEngineStatus {
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
