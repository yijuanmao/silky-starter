package com.silky.starter.excel.service.imports;

import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.entity.ImportRecord;
import com.silky.starter.excel.enums.ImportStatus;

/**
 * @author zy
 * @date 2025-10-27 15:38
 **/
public interface ImportRecordService {

    /**
     * 添加导入记录
     *
     * @param record 导入记录对象
     */
    void addImportRecord(ImportRecord record);

    /**
     * 更新导入记录状态
     *
     * @param taskId 导入记录ID
     * @param status 新状态
     */
    void updateStatus(String taskId, ImportStatus status);

    /**
     * 更新导入记录为成功
     *
     * @param taskId 导入记录ID
     * @param result 导入结果
     */
    void updateSuccess(String taskId, ImportResult result);

    /**
     * 更新导入记录为失败
     *
     * @param taskId   导入记录ID
     * @param errorMsg 错误信息
     */
    void updateFail(String taskId, String errorMsg);

    /**
     * 更新导入进度
     *
     * @param taskId       导入记录ID
     * @param totalCount   总数
     * @param successCount 成功数
     * @param failedCount  失败数
     */
    void updateProgress(String taskId, Long totalCount, Long successCount, Long failedCount);
}
