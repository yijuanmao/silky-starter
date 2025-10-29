package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.async.executor.AsyncExecutor;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.core.model.export.*;
import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.enums.ExportStatus;
import com.silky.starter.excel.service.export.ExportRecordService;
import com.silky.starter.excel.service.storage.StorageService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 导出引擎，负责协调导出任务的整个生命周期，包括任务创建、数据处理、文件生成和上传
 *
 * @author zy
 * @date 2025-10-24 15:25
 **/
@Slf4j
public class ExportEngine {

    /**
     * 任务缓存表
     * 用于存储正在处理或已完成的任务信息，支持任务状态查询和恢复
     * key: taskId, value: ExportTask
     */
    private final ConcurrentMap<String, ExportTask<?>> taskCache = new ConcurrentHashMap<>();

    /**
     * 引擎启动时间
     */
    private final long engineStartTime = System.currentTimeMillis();

    /**
     * 已处理任务总数
     */
    private long totalProcessedTasks = 0;

    /**
     * 成功任务数
     */
    private long successTasks = 0;

    /**
     * 失败任务数
     */
    private long failedTasks = 0;

    private final StorageService storageService;

    private final ExportRecordService recordService;

    private final AsyncExecutor asyncExecutor;

    public ExportEngine(StorageService storageService,
                        ExportRecordService recordService,
                        AsyncExecutor asyncExecutor) {
        this.storageService = storageService;
        this.recordService = recordService;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * 执行导出任务（支持多Sheet）
     *
     * @param request         导出请求
     * @param maxRowsPerSheet 每个Sheet的最大行数
     * @param <T>             数据类型
     * @return 导出结果
     */
    public <T> ExportResult executeWithSheets(ExportRequest<T> request, long maxRowsPerSheet) {
        // 参数校验
        validateExportRequest(request);

        String taskId = generateTaskId(request.getBusinessType());
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始创建多Sheet导出任务: {}, 业务类型: {}, 每Sheet最大行数: {}",
                    taskId, request.getBusinessType(), maxRowsPerSheet);

            // 创建导出记录
            ExportRecord record = this.createExportRecord(taskId, request);
            recordService.save(record);

            // 创建导出任务
            ExportTask<T> task = ExportTask.create(taskId, request, record);

            // 缓存任务信息
            cacheTask(task);

            // 提交任务到异步执行器
            asyncExecutor.submitExport(task);

            long costTime = System.currentTimeMillis() - startTime;

            log.info("多Sheet导出任务创建成功: {}, 业务类型: {}, 耗时: {}ms",
                    taskId, request.getBusinessType(), costTime);

            totalProcessedTasks++;

            return ExportResult.asyncSuccess(taskId);

        } catch (Exception e) {
            log.error("多Sheet导出任务创建失败: {}, 业务类型: {}", taskId, request.getBusinessType(), e);
            failedTasks++;

            // 更新记录状态为失败
            recordService.updateFailed(taskId, "任务创建失败: " + e.getMessage());

            return ExportResult.fail(taskId, "多Sheet导出任务创建失败: " + e.getMessage());
        }
    }

    /**
     * 处理多Sheet导出任务
     */
    private <T> String doExportWithSheets(ExportRequest<T> request, String taskId, long maxRowsPerSheet) {
        File tempFile = createTempFile(request.getFileName());

        try (EnhancedExcelWriter writer = new EnhancedExcelWriter(tempFile.getAbsolutePath(), maxRowsPerSheet)) {

            int pageNum = 1;
            boolean hasNext = true;
            long totalCount = 0;
            long startPageTime = System.currentTimeMillis();

            // 分页处理数据
            while (hasNext) {
                long pageStartTime = System.currentTimeMillis();

                // 检查任务超时
                if (isTaskTimeout(taskId)) {
                    throw new RuntimeException("任务执行超时，已中断");
                }

                // 分页查询数据
                ExportPageData<T> pageData = request.getDataSupplier().getPageData(
                        pageNum, request.getPageSize(), request.getParams()
                );

                if (pageData == null || CollectionUtil.isEmpty(pageData.getData())) {
                    log.debug("第{}页数据为空，导出完成", pageNum);
                    break;
                }

                List<T> data = pageData.getData();

                // 数据处理
                data = processData(data, request.getProcessors(), pageNum);

                // 写入Excel（支持多Sheet）
                writer.write(data, request.getDataClass(), request.getHeaderMapping());

                // 更新进度
                totalCount += data.size();
                recordService.updateProgress(taskId, totalCount);

                // 记录分页处理时间
                long pageCostTime = System.currentTimeMillis() - pageStartTime;
                log.debug("第{}页数据处理完成: 数据量={}, 当前Sheet={}, 耗时={}ms",
                        pageNum, data.size(), writer.getCurrentSheet(), pageCostTime);

                // 判断是否还有下一页
                hasNext = pageData.isHasNext();
                pageNum++;
            }

            long totalCostTime = System.currentTimeMillis() - startPageTime;

            // 上传到存储
            String fileUrl = storageService.upload(tempFile, request.getFileName(),
                    request.getStorageType(), taskId);

            // 获取文件大小
            long fileSize = storageService.getFileSize(extractFileKey(fileUrl), request.getStorageType());

            // 更新总数据量和文件大小
            long finalTotalCount = totalCount;
            recordService.update(taskId, record -> {
                record.setTotalCount(finalTotalCount);
                record.setFileSize(fileSize);
            });

            log.info("多Sheet导出任务数据处理完成: {}, 总数据量: {}, 文件大小: {} bytes, Sheet数: {}, 处理耗时: {}ms",
                    taskId, totalCount, fileSize, writer.getCurrentSheet(), totalCostTime);
            return fileUrl;
        } finally {
            // 清理临时文件
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 执行导出任务（使用配置的默认异步方式） 这是主要的导出入口方法，适用于大多数场景
     *
     * @param request 导出请求，包含所有导出参数和配置
     * @param <T>     要导出的数据类型
     * @return 导出结果，包含任务ID和状态信息
     */
/*    public <T> ExportResult execute(ExportRequest<T> request) {
        return execute(request, AsyncType.THREAD_POOL);
    }*/

    /**
     * 执行导出任务（指定异步方式）,允许调用方覆盖默认的异步处理方式
     *
     * @param request   导出请求
     * @param asyncType 异步处理类型，如果为null则使用请求中的配置
     * @param <T>       要导出的数据类型
     * @return 导出结果
     */
   /* public <T> ExportResult execute(ExportRequest<T> request, AsyncType asyncType) {
        // 参数校验
        validateExportRequest(request);

        String taskId = generateTaskId(request.getBusinessType());
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始创建导出任务: {}, 业务类型: {}", taskId, request.getBusinessType());

            // 创建导出记录
            ExportRecord record = createExportRecord(taskId, request);
            recordService.save(record);

            // 创建导出任务
            ExportTask<T> task = ExportTask.create(taskId, request, record);

            // 缓存任务信息（用于状态查询和恢复）
            cacheTask(task);

            // 确定异步方式（优先使用参数指定的方式）
            AsyncType targetAsyncType = asyncType != null ? asyncType : request.getAsyncType();

            // 提交任务到异步执行器
            asyncExecutor.submitExport(task, targetAsyncType);

            long costTime = System.currentTimeMillis() - startTime;

            log.info("导出任务创建成功: {}, 业务类型: {}, 异步方式: {}, 耗时: {}ms",
                    taskId, request.getBusinessType(), targetAsyncType, costTime);

            totalProcessedTasks++;

            return ExportResult.asyncSuccess(taskId);

        } catch (Exception e) {
            log.error("导出任务创建失败: {}, 业务类型: {}", taskId, request.getBusinessType(), e);
            failedTasks++;

            // 更新记录状态为失败
            recordService.updateFailed(taskId, "任务创建失败: " + e.getMessage());

            return ExportResult.fail(taskId, "导出任务创建失败: " + e.getMessage());
        }
    }*/

    /**
     * 处理导出任务 由异步处理器调用，执行实际的导出逻辑
     *
     * @param task 导出任务
     * @param <T>  要导出的数据类型
     */
    public <T> ExcelProcessResult processExportTask(ExportTask<T> task) {
        String taskId = task.getTaskId();
        ExportRequest<T> request = task.getRequest();

        log.info("开始处理导出任务: {}, 业务类型: {}", taskId, request.getBusinessType());

        // 标记任务开始执行
        task.markStart();

        try {
            // 创建导出记录
            ExportRecord record = createExportRecord(taskId, request);
            recordService.save(record);

            // 执行数据准备（如果有）
            prepareDataSupplier(request);

            // 执行导出逻辑
            String fileUrl = doExport(request, taskId);

            // 更新完成状态
            recordService.updateSuccess(taskId, fileUrl);

            // 标记任务完成
            task.markFinish();

            successTasks++;

            log.info("导出任务处理完成: {}, 文件URL: {}, 总耗时: {}ms", taskId, fileUrl, task.getExecuteTime());

            return ExcelProcessResult.asyncSuccess(taskId, "导出完成", record.getTotalCount());
        } catch (Exception e) {
            log.error("导出任务处理失败: {}", taskId, e);

            // 更新失败状态
            recordService.updateFailed(taskId, e.getMessage());

            // 标记任务完成（失败）
            task.markFinish();

            failedTasks++;

            return ExcelProcessResult.fail(taskId, "导出任务创建失败: " + e.getMessage());
        } finally {
            // 执行数据清理（如果有）
            cleanupDataSupplier(request);

            // 清理任务缓存（保留一段时间供查询）
            scheduleTaskCacheCleanup(taskId);
        }
    }

    /**
     * 同步导出（适合小数据量场景）  在当前线程中立即执行导出，阻塞直到完成
     *
     * @param request 导出请求
     * @param <T>     要导出的数据类型
     * @return 导出结果，包含文件URL
     */
    public <T> ExportResult exportSync(ExportRequest<T> request) {
        // 参数校验
        validateExportRequest(request);

        String taskId = generateTaskId(request.getBusinessType());
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始同步导出任务: {}, 业务类型: {}", taskId, request.getBusinessType());

            // 创建导出记录
            ExportRecord record = createExportRecord(taskId, request);
            recordService.save(record);

            // 直接执行导出
            recordService.updateStatus(taskId, ExportStatus.PROCESSING);

            // 执行数据准备
            prepareDataSupplier(request);

            // 执行导出逻辑
            String fileUrl = doExport(request, taskId);

            // 更新完成状态
            recordService.updateSuccess(taskId, fileUrl);

            long costTime = System.currentTimeMillis() - startTime;

            log.info("同步导出任务完成: {}, 文件URL: {}, 总耗时: {}ms",
                    taskId, fileUrl, costTime);

            successTasks++;

            return ExportResult.success(taskId, fileUrl, null).withCostTime(costTime);

        } catch (Exception e) {
            log.error("同步导出任务失败: {}, 业务类型: {}", taskId, request.getBusinessType(), e);

            // 更新失败状态
            recordService.updateFailed(taskId, "同步导出失败: " + e.getMessage());

            failedTasks++;

            return ExportResult.fail(taskId, "同步导出失败: " + e.getMessage());

        } finally {
            // 执行数据清理
            cleanupDataSupplier(request);
        }
    }

    /**
     * 根据任务ID获取任务信息
     *
     * @param taskId 任务ID
     * @param <T>    数据类型
     * @return 导出任务，如果不存在返回null
     */
    @SuppressWarnings("unchecked")
    public <T> ExportTask<T> getTaskById(String taskId) {
        return (ExportTask<T>) taskCache.get(taskId);
    }

    /**
     * 取消导出任务
     * 尝试停止正在执行的任务
     *
     * @param taskId 任务ID
     * @return 如果取消成功返回true，否则返回false
     */
    public boolean cancelTask(String taskId) {
        ExportTask<?> task = taskCache.get(taskId);
        if (task == null) {
            log.warn("尝试取消不存在的任务: {}", taskId);
            return false;
        }

        if (task.isFinished()) {
            log.warn("任务已完成，无法取消: {}", taskId);
            return false;
        }

        // 这里可以实现更复杂的取消逻辑
        // 比如中断执行线程等

        recordService.updateStatus(taskId, ExportStatus.CANCELLED);
        log.info("任务已取消: {}", taskId);

        return true;
    }

    /**
     * 重新执行失败的任务
     *
     * @param taskId 任务ID
     * @return 如果重新执行成功返回true，否则返回false
     */
    public boolean retryTask(String taskId) {
        ExportTask<?> task = taskCache.get(taskId);
        if (task == null) {
            log.warn("尝试重新执行不存在的任务: {}", taskId);
            return false;
        }

        if (!task.isFinished() || task.getRecord().getStatus() != ExportStatus.FAILED) {
            log.warn("任务不是失败状态，无法重新执行: {}", taskId);
            return false;
        }
        try {
            // 重新提交任务
            asyncExecutor.submitExport(task, task.getRequest().getAsyncType());

            log.info("任务重新执行已提交: {}", taskId);
            return true;

        } catch (Exception e) {
            log.error("任务重新执行失败: {}", taskId, e);
            return false;
        }
    }

    /**
     * 执行导出逻辑的核心方法
     *
     * @param request 导出请求
     * @param taskId  任务ID
     * @param <T>     数据类型
     * @return 文件访问URL
     */
    private <T> String doExport(ExportRequest<T> request, String taskId) {
        File tempFile = createTempFile(request.getFileName());

        try (EnhancedExcelWriter writer = new EnhancedExcelWriter(tempFile.getAbsolutePath())) {

            int pageNum = 1;
            boolean hasNext = true;
            long totalCount = 0;
            long startPageTime = System.currentTimeMillis();

            // 分页处理数据
            while (hasNext) {
                long pageStartTime = System.currentTimeMillis();

                // 检查任务超时
                if (isTaskTimeout(taskId)) {
                    throw new ExcelExportException("任务执行超时，已中断");
                }

                // 分页查询数据
                ExportPageData<T> pageData = request.getDataSupplier().getPageData(
                        pageNum, request.getPageSize(), request.getParams()
                );
                if (pageData == null || CollectionUtil.isEmpty(pageData.getData())) {
                    log.debug("第{}页数据为空，导出完成", pageNum);
                    break;
                }

                List<T> data = pageData.getData();

                // 数据处理
                data = processData(data, request.getProcessors(), pageNum);

                // 写入Excel
                if (pageNum == 1) {
                    writer.writeHeader(data, request.getDataClass());
                }
                writer.write(data, request.getDataClass());

                // 更新进度
                totalCount += data.size();
                recordService.updateProgress(taskId, totalCount);

                // 记录分页处理时间
                long pageCostTime = System.currentTimeMillis() - pageStartTime;
                log.debug("第{}页数据处理完成: 数据量={}, 耗时={}ms",
                        pageNum, data.size(), pageCostTime);

                // 判断是否还有下一页
                hasNext = pageData.isHasNext();
                pageNum++;
            }

            long totalCostTime = System.currentTimeMillis() - startPageTime;

            // 上传到存储
            String fileUrl = storageService.upload(tempFile, request.getFileName(),
                    request.getStorageType(), taskId);

            // 获取文件大小
            long fileSize = storageService.getFileSize(extractFileKey(fileUrl), request.getStorageType());

            // 更新总数据量和文件大小
            long finalTotalCount = totalCount;
            recordService.update(taskId, record -> {
                record.setTotalCount(finalTotalCount);
                record.setFileSize(fileSize);
            });

            log.info("导出任务数据处理完成: {}, 总数据量: {}, 文件大小: {} bytes, 处理耗时: {}ms",
                    taskId, totalCount, fileSize, totalCostTime);

            return fileUrl;

        } finally {
            // 清理临时文件
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 处理数据
     *
     * @param data       原始数据
     * @param processors 处理器列表
     * @param pageNum    页码
     * @param <T>        数据类型
     * @return 处理后的数据
     */
    private <T> List<T> processData(List<T> data, List<ExportDataProcessor<T>> processors, int pageNum) {
        if (processors == null || processors.isEmpty()) {
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
     * 准备数据供应器
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @throws Exception 准备异常
     */
    private <T> void prepareDataSupplier(ExportRequest<T> request) throws Exception {
        if (request.getDataSupplier() != null) {
            request.getDataSupplier().prepare(request.getParams());
        }

        if (request.getProcessors() != null) {
            for (ExportDataProcessor<T> processor : request.getProcessors()) {
                processor.prepare();
            }
        }
    }

    /**
     * 清理数据供应器
     *
     * @param request 导出请求
     * @param <T>     数据类型
     */
    private <T> void cleanupDataSupplier(ExportRequest<T> request) {
        try {
            if (request.getDataSupplier() != null) {
                request.getDataSupplier().cleanup(request.getParams());
            }
        } catch (Exception e) {
            log.warn("数据供应器清理异常", e);
        }

        try {
            if (request.getProcessors() != null) {
                for (ExportDataProcessor<T> processor : request.getProcessors()) {
                    processor.cleanup();
                }
            }
        } catch (Exception e) {
            log.warn("数据处理器清理异常", e);
        }
    }

    /**
     * 创建临时文件
     *
     * @param fileName 文件名
     * @return 临时文件
     */
    private File createTempFile(String fileName) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String filePath = tempDir + File.separator + "silky_export_" +
                System.currentTimeMillis() + "_" + fileName;

        File file = new File(filePath);
        File parentDir = file.getParentFile();

        // 确保目录存在
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                throw new ExcelExportException("创建临时文件目录失败: " + parentDir.getAbsolutePath());
            }
        }
        // 创建文件
        try {
            boolean created = file.createNewFile();
            if (!created) {
                throw new ExcelExportException("创建临时文件失败: " + filePath);
            }
        } catch (IOException e) {
            log.error("创建临时文件异常: {}", filePath, e);
            throw new ExcelExportException("创建临时文件异常: " + filePath, e);
        }
        log.debug("临时文件创建成功: {}", filePath);
        return file;
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
     * 校验导出请求参数
     *
     * @param request 导出请求
     * @param <T>     数据类型
     */
    private <T> void validateExportRequest(ExportRequest<T> request) {
        if (request == null) {
            throw new IllegalArgumentException("导出请求不能为null");
        }
        if (request.getDataClass() == null) {
            throw new IllegalArgumentException("数据类类型不能为null");
        }
        if (request.getFileName() == null || request.getFileName().trim().isEmpty()) {
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
     * 创建导出记录
     *
     * @param taskId  任务ID
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 导出记录
     */
    private <T> ExportRecord createExportRecord(String taskId, ExportRequest<T> request) {
        return ExportRecord.builder()
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
    }

    /**
     * 生成任务ID
     *
     * @param businessType 业务类型
     * @return 任务ID
     */
    private String generateTaskId(String businessType) {
        String prefix = StrUtil.isNotBlank(businessType) ?
                businessType.replaceAll("[^a-zA-Z0-9]", "_") : "TASK";
        return prefix + "_" + System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID();
    }

    /**
     * 缓存任务
     *
     * @param task 导出任务
     * @param <T>  数据类型
     */
    private <T> void cacheTask(ExportTask<T> task) {
        taskCache.put(task.getTaskId(), task);
        log.debug("任务已缓存: {}", task.getTaskId());
    }

    /**
     * 从文件URL中提取文件Key
     *
     * @param fileUrl 文件URL
     * @return 文件Key
     */
    private String extractFileKey(String fileUrl) {
        // 这里需要根据实际的存储策略来解析文件Key
        // 简化实现，实际项目中需要根据存储类型具体实现
        if (fileUrl.contains("/")) {
            return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        }
        return fileUrl;
    }

    /**
     * 检查任务是否超时
     *
     * @param taskId 任务ID
     * @return 如果超时返回true
     */
    private boolean isTaskTimeout(String taskId) {
        ExportTask<?> task = taskCache.get(taskId);
        if (task == null) {
            return false;
        }
        return task.isTimeout();
    }

    /**
     * 调度任务缓存清理
     *
     * @param taskId 任务ID
     */
    private void scheduleTaskCacheCleanup(String taskId) {
        // 这里可以实现延迟清理逻辑
        // 实际项目中可以使用定时任务来清理过期的任务缓存
        // 简化实现，立即清理完成的任务
        new Thread(() -> {
            try {
                // 延迟5分钟清理，确保用户可以查询到结果
                Thread.sleep(5 * 60 * 1000);
                taskCache.remove(taskId);
                log.debug("任务缓存已清理: {}", taskId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 获取引擎状态信息
     *
     * @return 引擎状态
     */
    public EngineStatus getEngineStatus() {
        return EngineStatus.builder()
                .engineStartTime(LocalDateTimeUtil.ofUTC(engineStartTime))
                .totalProcessedTasks(totalProcessedTasks)
                .successTasks(successTasks)
                .failedTasks(failedTasks)
                .cachedTasks(taskCache.size())
                .uptime(System.currentTimeMillis() - engineStartTime)
                .build();
    }

    /**
     * 引擎状态内部类
     */
    @Data
    @Builder
    public static class EngineStatus {

        /**
         * 引擎启动时间
         */
        private LocalDateTime engineStartTime;

        /**
         * 已处理任务总数
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
         * 缓存的任务数
         */
        private int cachedTasks;

        /**
         * 引擎运行时间（毫秒）
         */
        private long uptime;

        /**
         * 计算成功率
         *
         * @return 成功率（0.0 - 1.0）
         */
        public double getSuccessRate() {
            return totalProcessedTasks > 0 ? (double) successTasks / totalProcessedTasks : 0.0;
        }

        /**
         * 计算失败率
         *
         * @return 失败率（0.0 - 1.0）
         */
        public double getFailureRate() {
            return totalProcessedTasks > 0 ? (double) failedTasks / totalProcessedTasks : 0.0;
        }

        /**
         * 获取平均处理时间
         *
         * @return 平均处理时间（毫秒）
         */
        public long getAverageProcessTime() {
            return totalProcessedTasks > 0 ? uptime / totalProcessedTasks : 0;
        }
    }
}
