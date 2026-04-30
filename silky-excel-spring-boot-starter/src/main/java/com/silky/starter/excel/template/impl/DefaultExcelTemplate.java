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
import com.silky.starter.excel.template.ExcelTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Excel模板默认实现
 * 作为门面层统一调度导出/导入引擎，不执行业务校验（由Engine层负责）
 *
 * @author zy
 * @since 1.0.0
 */
@Slf4j
public class DefaultExcelTemplate implements ExcelTemplate, InitializingBean {

    /** 导出引擎 */
    private final ExportEngine exportEngine;
    /** 导入引擎 */
    private final ImportEngine importEngine;
    /** 异步任务线程池 */
    private final ThreadPoolTaskExecutor silkyExcelTaskExecutor;

    public DefaultExcelTemplate(ExportEngine exportEngine,
                                ImportEngine importEngine,
                                ThreadPoolTaskExecutor silkyExcelTaskExecutor) {
        this.exportEngine = exportEngine;
        this.importEngine = importEngine;
        this.silkyExcelTaskExecutor = silkyExcelTaskExecutor;

        log.info("DefaultExcelTemplate 初始化完成, 导出引擎: {}, 导入引擎: {}",
                exportEngine.getClass().getSimpleName(),
                importEngine.getClass().getSimpleName());
    }

    /**
     * 异步导出（使用默认异步方式）
     *
     * @param request 导出请求
     */
    @Override
    public <T> ExportResult exportAsync(ExportRequest<T> request) {
        return exportInternal(request, AsyncType.THREAD_POOL, null);
    }

    /**
     * 同步导出
     *
     * @param request 导出请求
     */
    @Override
    public <T> ExportResult exportSync(ExportRequest<T> request) {
        return exportInternal(request, AsyncType.SYNC, null);
    }

    /**
     * 导出数据（指定异步类型）
     *
     * @param request   导出请求
     * @param asyncType 异步类型
     */
    @Override
    public <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType) {
        return exportInternal(request, asyncType, null);
    }

    /**
     * 导出数据（支持自定义任务配置）
     *
     * @param request        导出请求
     * @param asyncType      异步类型
     * @param taskConfigurer 任务配置器
     */
    @Override
    public <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType,
                                   Consumer<ExportTask<T>> taskConfigurer) {
        return exportInternal(request, asyncType, taskConfigurer);
    }

    /**
     * 异步导出（返回CompletableFuture）
     *
     * @param request 导出请求
     */
    @Override
    public <T> CompletableFuture<ExportResult> exportFuture(ExportRequest<T> request) {
        return CompletableFuture.supplyAsync(() -> exportSync(request), silkyExcelTaskExecutor);
    }

    /**
     * 同步导入
     *
     * @param request 导入请求
     */
    @Override
    public <T> ImportResult importSync(ImportRequest<T> request) {
        return importInternal(request, AsyncType.SYNC, null);
    }

    /**
     * 异步导入
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
                    .totalProcessedTasks(0L).successTasks(0L)
                    .failedTasks(0L).cachedTasks(0).uptime(0L).build();
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
                    .totalProcessedTasks(0L).successTasks(0L)
                    .failedTasks(0L).cachedTasks(0).uptime(0L).build();
        }
    }

    /**
     * 优雅关闭Excel模板
     */
    @Override
    public void shutdown() {
        log.info("开始关闭Excel模板...");
        try {
            exportEngine.shutdown();
        } catch (Exception e) {
            log.error("关闭导出引擎失败", e);
        }
        try {
            importEngine.shutdown();
        } catch (Exception e) {
            log.error("关闭导入引擎失败", e);
        }
        log.info("Excel模板关闭完成");
    }

    /**
     * 内部导出方法
     * 校验逻辑已下沉到 ExportEngine，此层仅做任务创建和分发
     */
    private <T> ExportResult exportInternal(ExportRequest<T> request, AsyncType asyncType,
                                            Consumer<ExportTask<T>> taskConfigurer) {
        log.debug("开始处理导出请求，业务类型: {}, 异步类型: {}",
                request.getBusinessType(), asyncType);

        ExportTask<T> task = createExportTask(request, asyncType);
        if (taskConfigurer != null) {
            try {
                taskConfigurer.accept(task);
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
     * 校验逻辑已下沉到 ImportEngine，此层仅做任务创建和分发
     */
    private <T> ImportResult importInternal(ImportRequest<T> request, AsyncType asyncType,
                                            Consumer<ImportTask<T>> taskConfigurer) {
        log.debug("开始处理导入请求，业务类型: {}, 异步类型: {}",
                request.getBusinessType(), asyncType);

        ImportTask<T> task = createImportTask(request, asyncType);
        if (taskConfigurer != null) {
            try {
                taskConfigurer.accept(task);
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
        task.setAsyncType(asyncType);
        task.setCreateTime(System.currentTimeMillis());
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
                taskPrefix, safeBusinessType, System.currentTimeMillis(), IdUtil.fastSimpleUUID());
    }

    @Override
    public void afterPropertiesSet() {
        // 注册JVM关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("接收到JVM关闭信号，开始清理Excel模板资源...");
            shutdown();
        }));
        log.info("Excel模板初始化完成");
    }
}
