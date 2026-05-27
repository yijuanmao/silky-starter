package com.silky.starter.excel.service.imports.impl;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.entity.ImportRecord;
import com.silky.starter.excel.enums.ImportStatus;
import com.silky.starter.excel.service.imports.ImportRecordService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的导入记录服务实现。
 *
 * @author zy
 * @date 2025-10-28 15:42
 **/
public class InMemoryImportRecordService implements ImportRecordService {

    private final Map<String, ImportRecord> recordMap = new ConcurrentHashMap<>();

    @Override
    public void save(ImportRecord record) {
        if (record == null || StrUtil.isBlank(record.getTaskId())) {
            throw new IllegalArgumentException("导入记录和任务ID不能为空");
        }
        recordMap.put(record.getTaskId(), record);
    }

    @Override
    public ImportRecord getByTaskId(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        return recordMap.get(taskId);
    }

    @Override
    public void markProcessing(String taskId) {
        update(taskId, record -> record.setStatus(ImportStatus.PROCESSING));
    }

    @Override
    public void updateProgress(String taskId, long totalCount, long successCount, long failedCount) {
        update(taskId, record -> {
            record.setTotalCount(totalCount);
            record.setSuccessCount(successCount);
            record.setFailCount(failedCount);
        });
    }

    @Override
    public void markSuccess(String taskId, ImportResult result) {
        update(taskId, record -> {
            record.setStatus(ImportStatus.COMPLETED);
            record.setTotalCount(result.getTotalCount());
            record.setSuccessCount(result.getSuccessCount());
            record.setFailCount(result.getFailedCount());
        });
    }

    @Override
    public void markFailed(String taskId, String errorMsg) {
        update(taskId, record -> record.setStatus(ImportStatus.FAILED));
    }

    private void update(String taskId, java.util.function.Consumer<ImportRecord> updater) {
        ImportRecord record = getByTaskId(taskId);
        if (record != null) {
            updater.accept(record);
        }
    }
}
