package com.silky.starter.excel.core.model.export;

import com.silky.starter.excel.enums.ExportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 导出结果
 *
 * @author zy
 * @date 2025-10-24 11:19
 **/
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExportResult {

    /**
     * 导出是否成功
     */
    private boolean success;

    /**
     * 导出任务ID
     */
    private String taskId;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 状态码
     */
    private ExportStatus code;

    /**
     * 导出文件URL/路径
     */
    private String fileUrl;

    /**
     * 导出文件大小（字节）
     */
    private Long fileSize;

    /**
     * 总数据量
     */
    private Long totalCount;

    /**
     * 成功导出数量
     */
    private Long successCount;

    /**
     * 失败数量
     */
    private Long failedCount;

    /**
     * Sheet数量
     */
    private Integer sheetCount;

    /**
     * 导出耗时（毫秒）
     */
    private Long costTime;

    /**
     * 导出完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 创建成功结果
     */
    public static ExportResult success(String taskId) {
        ExportResult result = new ExportResult();
        result.setSuccess(true)
                .setTaskId(taskId)
                .setCode(ExportStatus.COMPLETED)
                .setMessage("导出成功")
                .setFinishTime(LocalDateTime.now());
        return result;
    }

    /**
     * 创建成功结果（含文件信息）
     */
    public static ExportResult success(String taskId, String fileUrl, Long totalCount, Long fileSize, Long costTime) {
        ExportResult result = success(taskId);
        result.setFileUrl(fileUrl)
                .setTotalCount(totalCount)
                .setSuccessCount(totalCount)
                .setFileSize(fileSize)
                .setCostTime(costTime);
        return result;
    }

    /**
     * 创建异步处理结果
     */
    public static ExportResult asyncSuccess(String taskId) {
        ExportResult result = new ExportResult();
        result.setSuccess(true)
                .setTaskId(taskId)
                .setCode(ExportStatus.PROCESSING)
                .setMessage("异步导出任务已提交，请稍后查询结果");
        return result;
    }

    /**
     * 创建失败结果
     */
    public static ExportResult fail(String taskId, String message) {
        ExportResult result = new ExportResult();
        result.setSuccess(false)
                .setTaskId(taskId)
                .setCode(ExportStatus.FAILED)
                .setMessage(message)
                .setFinishTime(LocalDateTime.now());
        return result;
    }

}
