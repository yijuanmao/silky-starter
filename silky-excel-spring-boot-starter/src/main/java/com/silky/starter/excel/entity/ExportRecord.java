package com.silky.starter.excel.entity;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.ExportStatus;
import com.silky.starter.excel.enums.StorageType;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 导出记录
 *
 * @author zy
 * @date 2025-10-24 11:28
 **/
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ExportRecord implements Serializable {

    private static final long serialVersionUID = 6537176124191549709L;

    /**
     * 导出任务唯一标识
     */
    private String taskId;

    /**
     * 业务类型标识
     */
    private String businessType;

    /**
     * 导出状态
     */
    private ExportStatus status;

    /**
     * 存储类型
     */
    private StorageType storageType;

    /**
     * 异步处理类型
     */
    private AsyncType asyncType;

    /**
     * 创建用户标识
     */
    private String createUser;

    /**
     * 任务创建时间
     */
    private LocalDateTime createTime;

    /**
     * 任务开始执行时间
     */
    private LocalDateTime startTime;

    /**
     * 任务完成时间 记录任务完成（成功或失败）的时间戳
     */
    private LocalDateTime finishTime;

    /**
     * 导出文件名
     */
    private String fileName;

    /**
     * 文件存储Key
     */
    private String fileKey;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 总数据量
     */
    private Long totalCount;

    /**
     * 已处理数据量
     */
    private Long processedCount;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 查询参数
     */
    private Map<String, Object> params;

    /**
     * 任务执行耗时（毫秒）
     * 从开始到结束的总耗时
     */
    private Long costTime;

    /**
     * 进度百分比
     * 计算得出的处理进度（0-100）
     */
    private Integer progress;

    /**
     * 计算进度百分比
     *
     * @return 进度百分比（0-100）
     */
    public Integer getProgress() {
        if (totalCount == null || totalCount <= 0) {
            return 0;
        }
        if (processedCount == null) {
            return 0;
        }
        return (int) (processedCount * 100 / totalCount);
    }

    /**
     * 计算执行耗时
     *
     * @return 执行耗时（毫秒）
     */
    public Long getCostTime() {
        if (startTime == null) {
            return 0L;
        }
        LocalDateTime endTime = finishTime == null ? LocalDateTime.now() : finishTime;
        return LocalDateTimeUtil.toEpochMilli(endTime) -  LocalDateTimeUtil.toEpochMilli(startTime);
    }

    /**
     * 检查任务是否已完成
     *
     * @return 如果已完成返回true
     */
    public boolean isCompleted() {
        return status != null && status.isFinal();
    }

    /**
     * 检查任务是否成功
     *
     * @return 如果成功返回true
     */
    public boolean isSuccess() {
        return status != null && status.isSuccess();
    }

    /**
     * 检查任务是否失败
     *
     * @return 如果失败返回true
     */
    public boolean isFailed() {
        return status != null && status == ExportStatus.FAILED;
    }

    /**
     * 检查任务是否正在处理中
     *
     * @return 如果正在处理中返回true
     */
    public boolean isProcessing() {
        return status != null && status.isInProgress();
    }

    /**
     * 获取任务状态描述
     *
     * @return 状态描述信息
     */
    public String getStatusDescription() {
        return status != null ? status.getDescription() : "未知状态";
    }

    /**
     * 获取存储类型描述
     *
     * @return 存储类型描述信息
     */
    public String getStorageTypeDescription() {
        return storageType != null ? storageType.getDescription() : "未知存储";
    }

    /**
     * 获取异步类型描述
     *
     * @return 异步类型描述信息
     */
    public String getAsyncTypeDescription() {
        return asyncType != null ? asyncType.getDescription() : "未知异步方式";
    }
}
