package com.silky.starter.excel.core.async;

import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.ExcelProcessResult;
import com.silky.starter.excel.enums.TaskType;

/**
 * 统一异步处理器接口,同时支持导入和导出任务
 *
 * @author zy
 * @date 2025-10-27 21:43
 **/
public interface UnifiedAsyncProcessor extends AsyncProcessor {

    /**
     * 检查是否支持该任务类型
     */
    boolean supports(TaskType taskType);

    /**
     * 提交任务
     *
     * @param task 任务
     * @return 任务处理结果
     */
    ExcelProcessResult submit(AsyncTask task) throws ExcelExportException;

    /**
     * 处理任务
     *
     * @param task 任务
     * @return 任务处理结果
     */
    ExcelProcessResult process(AsyncTask task) throws ExcelExportException;
}
