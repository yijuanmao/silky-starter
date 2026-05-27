package com.silky.starter.excel.service.export;

import com.silky.starter.excel.entity.ExportRecord;

/**
 * 导出记录服务。
 *
 * @author zy
 * @date 2025-10-24 15:18
 **/
public interface ExportRecordService {

    void save(ExportRecord record);

    ExportRecord getByTaskId(String taskId);

    void markProcessing(String taskId);

    void updateProgress(String taskId, long processedCount, long totalCount);

    void markSuccess(String taskId, String fileUrl, long fileSize);

    void markFailed(String taskId, String errorMsg);
}
