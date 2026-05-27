package com.silky.starter.excel.service.export.impl;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.enums.ExportStatus;
import com.silky.starter.excel.service.export.ExportRecordService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的导出记录服务实现。
 *
 * @author zy
 * @date 2025-10-24 15:19
 **/
@Slf4j
public class InMemoryExportRecordService implements ExportRecordService {

    private final Map<String, ExportRecord> recordMap = new ConcurrentHashMap<>();

    @Override
    public void save(ExportRecord record) {
        if (record == null || StrUtil.isBlank(record.getTaskId())) {
            throw new IllegalArgumentException("导出记录和任务ID不能为空");
        }
        recordMap.put(record.getTaskId(), record);
    }

    @Override
    public ExportRecord getByTaskId(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        return recordMap.get(taskId);
    }

    @Override
    public void markProcessing(String taskId) {
        update(taskId, record -> {
            record.setStatus(ExportStatus.PROCESSING);
            if (record.getStartTime() == null) {
                record.setStartTime(LocalDateTime.now());
            }
        });
    }

    @Override
    public void updateProgress(String taskId, long processedCount, long totalCount) {
        update(taskId, record -> {
            record.setProcessedCount(processedCount);
            if (totalCount >= 0) {
                record.setTotalCount(totalCount);
            } else if (record.getTotalCount() == null || record.getTotalCount() <= 0) {
                record.setTotalCount(processedCount);
            }
        });
    }

    @Override
    public void markSuccess(String taskId, String fileUrl, long fileSize) {
        update(taskId, record -> {
            record.setStatus(ExportStatus.COMPLETED);
            record.setFileUrl(fileUrl);
            record.setFileSize(fileSize);
            record.setFinishTime(LocalDateTime.now());
        });
    }

    @Override
    public void markFailed(String taskId, String errorMsg) {
        update(taskId, record -> {
            record.setStatus(ExportStatus.FAILED);
            record.setErrorMsg(errorMsg);
            record.setFinishTime(LocalDateTime.now());
            log.error("Excel 导出任务失败: {}, {}", taskId, errorMsg);
        });
    }

    private void update(String taskId, java.util.function.Consumer<ExportRecord> updater) {
        ExportRecord record = getByTaskId(taskId);
        if (record != null) {
            updater.accept(record);
        }
    }
}
