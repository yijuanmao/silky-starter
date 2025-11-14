package com.silky.starter.excel.template.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.engine.ExportEngine;
import com.silky.starter.excel.core.engine.ImportEngine;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.core.model.imports.ImportTask;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.TaskType;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.template.ExcelTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Excel导出模板默认实现
 *
 * @author zy
 * @date 2025-10-24 16:46
 **/
public class DefaultExcelTemplate implements ExcelTemplate, InitializingBean {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultExcelTemplate.class);

    private final ExportEngine exportEngine;

    private final ImportEngine importEngine;

    private final ThreadPoolTaskExecutor silkyExcelTaskExecutor;

    public DefaultExcelTemplate(ExportEngine exportEngine,
                                ImportEngine importEngine,
                                ThreadPoolTaskExecutor silkyExcelTaskExecutor) {
        this.exportEngine = exportEngine;
        this.importEngine = importEngine;
        this.silkyExcelTaskExecutor = silkyExcelTaskExecutor;

        log.info("DefaultExcelTemplate initialized with exportEngine: {}, importEngine: {}",
                exportEngine.getClass().getSimpleName(),
                importEngine.getClass().getSimpleName());
    }

    /**
     * 异步导出数据（使用默认异步方式）
     *
     * @param request 导出请求
     */
    @Override
    public <T> ExportResult exportAsync(ExportRequest<T> request) {
        return exportInternal(request, AsyncType.THREAD_POOL, null);
    }

    /**
     * 同步导出数据（使用默认异步方式）
     *
     * @param request 导出请求
     */
    @Override
    public <T> ExportResult exportSync(ExportRequest<T> request) {
        return exportInternal(request, AsyncType.SYNC, null);
    }

    /**
     * 异步导出数据（使用指定异步方式）
     *
     * @param request   导出请求
     * @param asyncType 异步方式
     */
    @Override
    public <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType) {
        return exportInternal(request, asyncType, null);
    }

    /**
     * 异步导出数据（使用指定异步方式）
     *
     * @param request        导出请求
     * @param asyncType      异步方式
     * @param taskConfigurer 任务配置器
     */
    @Override
    public <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType,
                                   Consumer<ExportTask<T>> taskConfigurer) {
        return exportInternal(request, asyncType, taskConfigurer);
    }

    /**
     * 异步导出数据（使用默认异步方式）
     *
     * @param request 导出请求
     */
    @Override
    public <T> CompletableFuture<ExportResult> exportFuture(ExportRequest<T> request) {
        return CompletableFuture.supplyAsync(() -> exportSync(request), silkyExcelTaskExecutor);
    }


    /**
     * 同步导入数据（适合小数据量）
     *
     * @param request 导入请求
     */
    @Override
    public <T> ImportResult importSync(ImportRequest<T> request) {
        return importInternal(request, AsyncType.SYNC, null);
    }

    /**
     * 异步导入数据
     *
     * @param request 导入请求
     */
    @Override
    public <T> ImportResult importAsync(ImportRequest<T> request) {
        return importInternal(request, AsyncType.THREAD_POOL, null);
    }

    /**
     * 导入数据（指定异步类型）
     *
     * @param request   导入请求
     * @param asyncType 异步类型
     */
    @Override
    public <T> ImportResult imports(ImportRequest<T> request, AsyncType asyncType) {
        return importInternal(request, asyncType, null);
    }

    /**
     * 导入数据（支持自定义任务配置）
     *
     * @param request        导入请求
     * @param asyncType      异步类型
     * @param taskConfigurer 任务配置器
     */
    @Override
    public <T> ImportResult imports(ImportRequest<T> request, AsyncType asyncType,
                                    Consumer<ImportTask<T>> taskConfigurer) {
        return importInternal(request, asyncType, taskConfigurer);
    }

    /**
     * 异步导入（返回CompletableFuture）
     *
     * @param request 导入请求
     */
    @Override
    public <T> CompletableFuture<ImportResult> importFuture(ImportRequest<T> request) {
        return CompletableFuture.supplyAsync(() -> importSync(request), silkyExcelTaskExecutor);
    }

    /**
     * 获取导出引擎状态
     */
    @Override
    public ExportEngine.EngineStatus getExportEngineStatus() {
        try {
            return exportEngine.getEngineStatus();
        } catch (Exception e) {
            log.error("获取导出引擎状态失败", e);
            return ExportEngine.EngineStatus.builder()
                    .totalProcessedTasks(0L)
                    .successTasks(0L)
                    .failedTasks(0L)
                    .cachedTasks(0)
                    .batchTasks(0)
                    .uptime(0L)
                    .build();
        }
    }

    /**
     * 获取导入引擎状态
     */
    @Override
    public ImportEngine.ImportEngineStatus getImportEngineStatus() {
        try {
            return importEngine.getEngineStatus();
        } catch (Exception e) {
            log.error("获取导入引擎状态失败", e);
            return ImportEngine.ImportEngineStatus.builder()
                    .totalProcessedTasks(0L)
                    .successTasks(0L)
                    .failedTasks(0L)
                    .cachedTasks(0)
                    .batchTasks(0)
                    .uptime(0L)
                    .build();
        }
    }

    /**
     * 关闭Excel模板
     */
    @Override
    public void shutdown() {
        log.info("开始关闭Excel模板...");
        try {
            exportEngine.shutdown();
            log.info("导出引擎已关闭");
        } catch (Exception e) {
            log.error("关闭导出引擎失败", e);
        }

        try {
            importEngine.shutdown();
            log.info("导入引擎已关闭");
        } catch (Exception e) {
            log.error("关闭导入引擎失败", e);
        }
        log.info("Excel模板关闭完成");
    }


    /**
     * 内部导出方法
     *
     * @param request        导出请求
     * @param asyncType      异步类型
     * @param taskConfigurer 任务配置器
     * @return 导出结果
     */
    private <T> ExportResult exportInternal(ExportRequest<T> request, AsyncType asyncType,
                                            Consumer<ExportTask<T>> taskConfigurer) {
        log.debug("开始处理导出请求，业务类型: {}, 异步类型: {}",
                request.getBusinessType(), asyncType);

        validateExportRequest(request);

        ExportTask<T> task = createExportTask(request, asyncType);
        if (taskConfigurer != null) {
            try {
                taskConfigurer.accept(task);
                log.debug("任务配置器执行完成");
            } catch (Exception e) {
                log.warn("任务配置器执行异常", e);
            }
        }
        try {
            ExportResult result;
            switch (asyncType) {
                case SYNC:
                    result = exportEngine.exportSync(task);
                    break;
                case THREAD_POOL:
                    result = exportEngine.exportAsync(task);
                    break;
                default:
                    log.warn("不支持的异步类型: {}, 使用同步方式", asyncType);
                    result = exportEngine.exportSync(task);
            }

            log.debug("导出处理完成，任务ID: {}, 结果: {}", task.getTaskId(), result.isSuccess());
            return result;

        } catch (Exception e) {
            log.error("导出数据异常，业务类型: {}", request.getBusinessType(), e);
            return ExportResult.fail(task.getTaskId(), "导出失败: " + e.getMessage());
        }
    }

    /**
     * 内部导入方法
     */
    private <T> ImportResult importInternal(ImportRequest<T> request, AsyncType asyncType,
                                            Consumer<ImportTask<T>> taskConfigurer) {
        log.debug("开始处理导入请求，业务类型: {}, 异步类型: {}", request.getBusinessType(), asyncType);

        validateImportRequest(request);
        ImportTask<T> task = createImportTask(request, asyncType);
        if (taskConfigurer != null) {
            try {
                taskConfigurer.accept(task);
                log.debug("导入任务配置器执行完成");
            } catch (Exception e) {
                log.warn("导入任务配置器执行异常", e);
            }
        }

        try {
            ImportResult result;
            switch (asyncType) {
                case SYNC:
                    result = importEngine.importSync(request);
                    break;
                case THREAD_POOL:
                    result = importEngine.importAsync(task);
                    break;
                default:
                    log.warn("不支持的异步类型: {}, 使用同步方式", asyncType);
                    result = importEngine.importSync(request);
            }

            log.debug("导入处理完成，任务ID: {}, 结果: {}", task.getTaskId(), result.isSuccess());
            return result;

        } catch (Exception e) {
            log.error("导入数据异常，业务类型: {}", request.getBusinessType(), e);
            return ImportResult.fail(task.getTaskId(), "导入失败: " + e.getMessage());
        }
    }


    /**
     * 创建导出任务
     */
    private <T> ExportTask<T> createExportTask(ExportRequest<T> request, AsyncType asyncType) {
        ExportTask<T> task = new ExportTask<>();
        task.setRequest(request);
        task.setTaskId(generateTaskId(request.getBusinessType(), "EXPORT"));
        task.setTaskType(TaskType.EXPORT);
        task.setBusinessType(request.getBusinessType());
        task.setAsyncType(asyncType);
        task.setCreateTime(System.currentTimeMillis());

        log.debug("创建导出任务: {}, 业务类型: {}", task.getTaskId(), request.getBusinessType());
        return task;
    }

    /**
     * 创建导入任务
     */
    private <T> ImportTask<T> createImportTask(ImportRequest<T> request, AsyncType asyncType) {
        ImportTask<T> task = new ImportTask<>();
        task.setRequest(request);
        task.setTaskId(generateTaskId(request.getBusinessType(), "IMPORT"));
        task.setTaskType(TaskType.IMPORT);
        task.setBusinessType(request.getBusinessType());
        task.setCreateTime(System.currentTimeMillis());
        task.setAsyncType(asyncType);

        log.debug("创建导入任务: {}, 业务类型: {}", task.getTaskId(), request.getBusinessType());
        return task;
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId(String businessType, String prefix) {
        String safeBusinessType = StrUtil.isNotBlank(businessType) ?
                businessType.replaceAll("[^a-zA-Z0-9]", "_") : "UNKNOWN";
        String taskPrefix = StrUtil.isNotBlank(prefix) ? prefix : "TASK";

        return String.format("%s_%s_%s_%s",
                taskPrefix,
                safeBusinessType,
                System.currentTimeMillis(),
                IdUtil.fastSimpleUUID());
    }

    /**
     * 验证导出请求
     */
    private <T> void validateExportRequest(ExportRequest<T> request) {
        if (request == null) {
            throw new IllegalArgumentException("导出请求参数不能为null");
        }
        if (StrUtil.isBlank(request.getFileName())) {
            throw new IllegalArgumentException("导出文件名不能为空");
        }
        if (request.getDataSupplier() == null) {
            throw new IllegalArgumentException("导出数据提供者不能为空");
        }
        if (request.getDataClass() == null) {
            throw new IllegalArgumentException("数据类类型不能为null");
        }
        if (request.getPageSize() <= 0) {
            throw new IllegalArgumentException("分页大小必须大于0");
        }

        log.debug("导出请求验证通过，业务类型: {}", request.getBusinessType());
    }

    /**
     * 验证导入请求
     */
    private <T> void validateImportRequest(ImportRequest<T> request) {
        if (request == null) {
            throw new IllegalArgumentException("导入请求参数不能为null");
        }
        if (StrUtil.isBlank(request.getFileName())) {
            throw new IllegalArgumentException("导入文件名不能为空");
        }
        if (StrUtil.isBlank(request.getFileUrl())) {
            throw new IllegalArgumentException("导入文件URL不能为空");
        }
        if (request.getDataImporterSupplier() == null) {
            throw new IllegalArgumentException("导入数据提供者不能为空");
        }
        if (request.getDataClass() == null) {
            throw new IllegalArgumentException("数据类类型不能为null");
        }
        if (request.getPageSize() <= 0) {
            throw new IllegalArgumentException("分页大小必须大于0");
        }

        log.debug("导入请求验证通过，业务类型: {}", request.getBusinessType());
    }

    @Override
    public void afterPropertiesSet() {
        init();
    }

    /**
     * 初始化方法
     */
    private void init() {
        log.info("开始初始化Excel模板...");

        // 检查引擎状态
        try {
            ExportEngine.EngineStatus exportStatus = exportEngine.getEngineStatus();
            log.info("导出引擎状态: 运行时间={}ms, 处理任务={}",
                    exportStatus.getUptime(), exportStatus.getTotalProcessedTasks());
        } catch (Exception e) {
            log.warn("获取导出引擎状态失败", e);
        }

        try {
            ImportEngine.ImportEngineStatus importStatus = importEngine.getEngineStatus();
            log.info("导入引擎状态: 运行时间={}ms, 处理任务={}",
                    importStatus.getUptime(), importStatus.getTotalProcessedTasks());
        } catch (Exception e) {
            log.warn("获取导入引擎状态失败", e);
        }

        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("接收到JVM关闭信号，开始清理Excel模板资源...");
            shutdown();
        }));

        log.info("Excel模板初始化完成");
    }
}
