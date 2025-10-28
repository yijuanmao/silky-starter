package com.silky.starter.excel.service.imports.impl;

import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.entity.ImportRecord;
import com.silky.starter.excel.enums.ImportStatus;
import com.silky.starter.excel.service.imports.ImportRecordService;

/**
 * 基于内存的导入记录服务实现,注意：此实现仅用于示例和测试，生产环境应该使用数据库存储
 *
 * @author zy
 * @date 2025-10-28 15:42
 **/
public class InMemoryImportRecordService implements ImportRecordService {

    /**
     * 添加导入记录
     *
     * @param record 导入记录对象
     */
    @Override
    public void addImportRecord(ImportRecord record) {

    }

    /**
     * 更新导入记录状态
     *
     * @param taskId 导入记录ID
     * @param status 新状态
     */
    @Override
    public void updateStatus(String taskId, ImportStatus status) {

    }

    /**
     * 更新导入记录为成功
     *
     * @param taskId 导入记录ID
     * @param result 导入结果
     */
    @Override
    public void updateSuccess(String taskId, ImportResult result) {

    }

    /**
     * 更新导入记录为失败
     *
     * @param taskId   导入记录ID
     * @param errorMsg 错误信息
     */
    @Override
    public void updateFail(String taskId, String errorMsg) {

    }

    /**
     * 更新导入进度
     *
     * @param taskId       导入记录ID
     * @param totalCount   总数
     * @param successCount 成功数
     * @param failedCount  失败数
     */
    @Override
    public void updateProgress(String taskId, Long totalCount, Long successCount, Long failedCount) {

    }
}
