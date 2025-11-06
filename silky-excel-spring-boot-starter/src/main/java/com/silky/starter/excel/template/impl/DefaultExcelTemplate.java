package com.silky.starter.excel.template.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.async.ExportAsyncProcessor;
import com.silky.starter.excel.core.async.ImportAsyncProcessor;
import com.silky.starter.excel.core.async.factory.AsyncProcessorFactory;
import com.silky.starter.excel.core.async.model.ProcessorStatus;
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

/**
 * Excel导出模板默认实现
 *
 * @author zy
 * @date 2025-10-24 16:46
 **/
public class DefaultExcelTemplate implements ExcelTemplate, InitializingBean {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultExcelTemplate.class);

    private final AsyncProcessorFactory processorFactory;

    private final SilkyExcelProperties properties;

    private final AsyncType defaultAsyncType;

    public DefaultExcelTemplate(AsyncProcessorFactory processorFactory,
                                SilkyExcelProperties properties) {

        this.processorFactory = processorFactory;
        this.properties = properties;
        this.defaultAsyncType = properties.getAsync().getAsyncType();
    }

    /**
     * 导出数据（使用默认异步方式）
     *
     * @param request 导出请求
     * @return 导出结果
     */
    @Override
    public <T> ExportResult exportAsync(ExportRequest<T> request) {
        return this.export(request, AsyncType.THREAD_POOL);
    }

    /**
     * 同步导出数据（适合小数据量）
     *
     * @param request 导出请求
     * @return 导出结果
     */
    @Override
    public <T> ExportResult exportSync(ExportRequest<T> request) {
        return this.export(request, AsyncType.SYNC);
    }

    /**
     * 导出数据
     *
     * @param request   导出请求
     * @param asyncType 异步类型
     * @return 导出结果
     */
    @Override
    public <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType) {
        //校验参数
        this.validateExportRequest(request, asyncType);
        ExportTask<T> task = createExportTask(request, asyncType);
        try {
            ExportAsyncProcessor processor = processorFactory.getExportProcessor(asyncType);
            if (!processor.isAvailable() || !properties.getAsync().isEnabled()) {
                log.debug("异步导出被禁用，使用同步方式执行任务: {}", task.getTaskId());
                ExportAsyncProcessor syncProcessor = processorFactory.getExportProcessor(AsyncType.SYNC);
                return syncProcessor.process(task);
            }
            ExportResult result = processor.submit(task);
            log.debug("导出任务提交成功: {}, 处理器: {}", task.getTaskId(), asyncType);
            return result;
        } catch (IllegalArgumentException e) {
            log.warn("导出参数校验失败: {}", e.getMessage());
            return ExportResult.fail(asyncType.name() + "_" + System.currentTimeMillis(), "参数校验失败");
        } catch (Exception e) {
            log.error("导出数据异常，异步类型: {}", asyncType, e);
            return ExportResult.fail(asyncType.name() + "_" + System.currentTimeMillis(), "silky excel导出失败: " + e.getMessage());
        }
    }

    /**
     * 同步导入数据（适合小数据量）
     *
     * @param request 导出请求
     * @return 导入结果
     */
    @Override
    public <T> ImportResult importSync(ImportRequest<T> request) {
        return this.imports(request, AsyncType.SYNC);
    }

    /**
     * 导入数据,异步方法
     *
     * @param request 导出请求
     * @param <T>     数据类型
     * @return 导入结果
     */
    @Override
    public <T> ImportResult importAsync(ImportRequest<T> request) {
        return this.imports(request, AsyncType.THREAD_POOL);
    }

    /**
     * 导入数据
     *
     * @param request   导出请求
     * @param asyncType 异步类型
     * @return 导入结果
     */
    @Override
    public <T> ImportResult imports(ImportRequest<T> request, AsyncType asyncType) {
        this.validateImportRequest(request, asyncType);
        try {
            ImportTask<T> task = createImportTask(request);
            ImportAsyncProcessor processor = processorFactory.getImportProcessor(asyncType);
            if (!processor.isAvailable() || !properties.getAsync().isEnabled()) {
                log.debug("异步导入处理器不可用，使用同步方式执行任务: {}", task.getTaskId());
                ImportAsyncProcessor syncProcessor = processorFactory.getImportProcessor(AsyncType.SYNC);
                return syncProcessor.process(task);
            }
            ImportResult result = processor.submit(task);
            log.debug("导入任务提交成功: {}, 处理器: {}", task.getTaskId(), asyncType);
            return result;
        } catch (IllegalArgumentException e) {
            log.warn("导入参数校验失败: {}", e.getMessage());
            return ImportResult.fail(asyncType.name() + "_IMPORT_" + System.currentTimeMillis(), "参数校验失败");
        } catch (Exception e) {
            log.error("导入数据异常，异步类型: {}", asyncType, e);
            return ImportResult.fail(asyncType.name() + "_IMPORT_" + System.currentTimeMillis(), "导入失败，请稍后重试");
        }
    }

    /**
     * 获取处理器状态
     *
     * @param asyncType 处理器类型
     * @return 处理器状态
     */
    @Override
    public ProcessorStatus getProcessorStatus(String asyncType) {
        if (StrUtil.isBlank(asyncType)) {
            log.warn("处理器类型不能为空");
            return null;
        }
        try {
            ExportAsyncProcessor exportProcessor = processorFactory.getExportProcessor(asyncType);
            return exportProcessor.getStatus();
        } catch (Exception ignored) {
        }
        try {
            ImportAsyncProcessor importProcessor = processorFactory.getImportProcessor(asyncType);
            return importProcessor.getStatus();
        } catch (Exception e) {
            log.warn("获取处理器状态失败: {}", asyncType, e);
            return null;
        }
    }

    /**
     * 设置处理器可用状态
     *
     * @param asyncType 处理器类型
     * @param available 是否可用
     */
    @Override
    public void setAvailable(String asyncType, boolean available) {
        if (StrUtil.isBlank(asyncType)) {
            log.warn("处理器类型不能为空");
            return;
        }
        try {
            ExportAsyncProcessor exportProcessor = processorFactory.getExportProcessor(asyncType);
            exportProcessor.setAvailable(available);
            log.info("设置导出处理器可用状态成功: {}, {}", asyncType, available);
            return;
        } catch (Exception ignored) {
        }
        try {
            ImportAsyncProcessor importProcessor = processorFactory.getImportProcessor(asyncType);
            importProcessor.setAvailable(available);
            log.info("设置导入处理器可用状态成功: {}, {}", asyncType, available);
        } catch (Exception e) {
            log.warn("设置处理器可用状态失败: {}, {}", asyncType, available, e);
        }
    }

    /**
     * 创建导出任务
     */
    private <T> ExportTask<T> createExportTask(ExportRequest<T> request, AsyncType asyncType) {
        ExportTask<T> task = new ExportTask<>();
        task.setRequest(request);
        task.setTaskId(generateTaskId(request.getBusinessType()));
        task.setTaskType(TaskType.EXPORT);
        task.setBusinessType(request.getBusinessType());
        task.setAsyncType(asyncType);
        return task;
    }

    /**
     * 创建导入任务
     */
    private <T> ImportTask<T> createImportTask(ImportRequest<T> request) {
        ImportTask<T> task = new ImportTask<>();
        task.setRequest(request);
        task.setTaskId(generateTaskId(request.getBusinessType()));
        task.setTaskType(TaskType.IMPORT);
        task.setBusinessType(request.getBusinessType());
        return task;
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId(String businessType) {
        String prefix = StrUtil.isNotBlank(businessType) ?
                businessType.replaceAll("[^a-zA-Z0-9]", "_") : "TASK";
        return prefix + "_" + IdUtil.fastSimpleUUID();
    }

    /**
     * 验证导出任务
     */
    private <T> void validateExportRequest(ExportRequest<T> request, AsyncType asyncType) {
        if (request == null) {
            throw new IllegalArgumentException("导出请求参数不能为null");
        }
        if (asyncType == null) {
            throw new IllegalArgumentException("导出异步类型不能为空");
        }
        if (StrUtil.isBlank(request.getFileName())) {
            throw new IllegalArgumentException("导出文件名不能为空");
        }
        if (request.getDataSupplier() == null) {
            throw new IllegalArgumentException("导出数据提供者不能为空");
        }
    }

    /**
     * 验证导入请求参数
     */
    private <T> void validateImportRequest(ImportRequest<T> request, AsyncType asyncType) {
        if (request == null) {
            throw new IllegalArgumentException("导入请求参数不能为null");
        }
        if (asyncType == null) {
            throw new IllegalArgumentException("导入异步类型不能为空");
        }
        if (StrUtil.isBlank(request.getFileName())) {
            throw new IllegalArgumentException("导出文件名不能为空");
        }
        if (StrUtil.isBlank(request.getFileUrl())) {
            throw new IllegalArgumentException("导出文件URL不能为空");
        }
        if (request.getDataImporterSupplier() == null) {
            throw new IllegalArgumentException("导入数据提供者不能为空");
        }
    }

    @Override
    public void afterPropertiesSet() {
        this.init();
    }

    /**
     * 初始化方法
     */
    private void init() {
        log.info("开始初始化异步执行器...");

        if (!processorFactory.isInitialized()) {
            log.warn("异步处理器工厂未初始化，异步执行器可能无法正常工作");
        }

        boolean defaultAvailable = checkDefaultProcessorAvailable(defaultAsyncType);
        if (!defaultAvailable) {
            log.warn("默认异步处理器不可用: {}, 将使用降级策略", defaultAsyncType);
        }
        log.info("异步执行器初始化完成，默认异步类型: {}, 可用状态: {}", defaultAsyncType, defaultAvailable);
    }

    /**
     * 检查默认处理器是否可用
     */
    private boolean checkDefaultProcessorAvailable(AsyncType defaultType) {
        try {
            processorFactory.getExportProcessor(defaultType);
            return true;
        } catch (Exception ignored) {
        }
        try {
            processorFactory.getImportProcessor(defaultType);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }
}
