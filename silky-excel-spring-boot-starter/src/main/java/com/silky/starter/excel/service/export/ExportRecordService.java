package com.silky.starter.excel.service.export;

import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.enums.ExportStatus;

import java.util.List;
import java.util.function.Consumer;

/**
 * 导出记录服务接口
 *
 * @author zy
 * @date 2025-10-24 15:18
 **/
public interface ExportRecordService {

    /**
     * 保存导出记录
     *
     * @param record 导出记录
     */
    void save(ExportRecord record);

    /**
     * 根据任务ID查询导出记录
     *
     * @param taskId 任务ID
     * @return 导出记录，如果不存在返回null
     */
    ExportRecord getByTaskId(String taskId);

    /**
     * 更新导出状态
     *
     * @param taskId 任务ID
     * @param status 新的状态
     */
    void updateStatus(String taskId, ExportStatus status);

    /**
     * 更新导出进度
     *
     * @param taskId         任务ID
     * @param processedCount 已处理数据量
     * @param successCount   成功数据量
     * @param failedCount    失败数据量
     */
    void updateProgress(String taskId, long processedCount, long successCount, long failedCount);

    /**
     * 更新导出成功
     *
     * @param taskId  任务ID
     * @param fileUrl 文件URL
     */
    void updateSuccess(String taskId, String fileUrl);

    /**
     * 更新导出失败
     *
     * @param taskId   任务ID
     * @param errorMsg 错误信息
     */
    void updateFailed(String taskId, String errorMsg);

    /**
     * 更新导出记录
     *
     * @param taskId  任务ID
     * @param updater 更新函数
     */
    void update(String taskId, Consumer<ExportRecord> updater);

    /**
     * 删除导出记录
     *
     * @param taskId 任务ID
     */
    void delete(String taskId);

    /**
     * 根据状态查询导出记录列表
     *
     * @param status 状态
     * @return 导出记录列表
     */
    List<ExportRecord> listByStatus(ExportStatus status);

    /**
     * 根据业务类型查询导出记录列表
     *
     * @param businessType 业务类型
     * @return 导出记录列表
     */
    List<ExportRecord> listByBusinessType(String businessType);

    /**
     * 清理过期的导出记录
     *
     * @param expireDays 过期天数
     * @return 清理的记录数量
     */
    int cleanExpiredRecords(int expireDays);
}
