package com.silky.starter.excel.template.processor;

import com.silky.starter.excel.core.async.ExportAsyncProcessor;
import com.silky.starter.excel.core.async.factory.AsyncProcessorFactory;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.export.ExportTask;
import com.silky.starter.excel.enums.AsyncType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * RabbitMQ导出异步处理器
 *
 * @author zy
 * @date 2025-11-05 17:55
 **/
@Slf4j
@Component
public class ExportRabbitMQProcessor implements ExportAsyncProcessor {
//    @Autowired
//    private AsyncProcessorFactory processorFactory;

    @PostConstruct
    public void init() {
        log.info("ExportRabbitMQProcessor bean created");
    }

    /**
     * 获取处理器类型，类型应该与AsyncType枚举值对应，用于在工厂中标识处理器
     * 参照枚举类:{@link AsyncType}
     */
    @Override
    public String getType() {
        return AsyncType.MQ.name();
    }

    /**
     * 提交导出任务
     * 将任务提交到异步处理系统，立即返回，任务会在后台执行
     * 此方法应该是非阻塞的，提交后立即返回
     *
     * @param task 要处理的导出任务，包含任务ID、请求参数和记录信息
     */
    @Override
    public ExportResult submit(ExportTask<?> task) throws ExcelExportException {
        //这里模拟发送mq导出消息，实际应用中需要集成RabbitMQ客户端发送消息
        log.info("Submitting export task to RabbitMQ: {}", task.getTaskId());
        return this.process(task);
    }

    /**
     * 处理导出任务
     * 实际执行导出任务的核心方法，包含数据查询、Excel生成和文件上传
     * 此方法会阻塞当前线程直到任务完成
     *
     * @param task 要处理的导出任务
     */
    @Override
    public ExportResult process(ExportTask<?> task) throws ExcelExportException {
//        ExportAsyncProcessor processor = processorFactory.getExportProcessor(AsyncType.MQ.name());
//        return processor.process(task);
        return ExportResult.success(task.getTaskId());
    }
}
