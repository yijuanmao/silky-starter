package com.silky.starter.excel.service.imports;

import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.entity.ImportRecord;

/**
 * 导入记录服务。
 *
 * @author zy
 * @date 2025-10-27 15:38
 **/
public interface ImportRecordService {

    void save(ImportRecord record);

    ImportRecord getByTaskId(String taskId);

    void markProcessing(String taskId);

    void updateProgress(String taskId, long totalCount, long successCount, long failedCount);

    void markSuccess(String taskId, ImportResult result);

    void markFailed(String taskId, String errorMsg);
}
