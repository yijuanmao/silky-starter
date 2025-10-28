package com.silky.starter.excel.core.model.export;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导出结果
 *
 * @author zy
 * @date 2025-10-24 11:19
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportResult {

    /**
     * 导出是否成功
     * true表示操作成功，false表示操作失败
     */
    private boolean success;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 消息
     */
    private String msg;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     * 导出成功时包含的文件大小
     */
    private Long fileSize;

    /**
     * 导出耗时（毫秒）
     * 从任务提交到完成的时间
     */
    private Long costTime;

    /**
     * 创建成功结果
     *
     * @param taskId 任务ID
     * @return 成功结果实例
     */
    public static ExportResult success(String taskId) {
        return success(taskId, "");
    }

    /**
     * 创建成功结果
     *
     * @param taskId  任务ID
     * @param fileUrl 文件URL
     * @return 成功结果实例
     */
    public static ExportResult success(String taskId, String fileUrl) {
        return success(taskId, fileUrl, 0L);
    }

    /**
     * 创建成功结果（包含文件信息）
     *
     * @param taskId   任务ID
     * @param fileUrl  文件URL
     * @param fileSize 文件大小
     * @return 成功结果实例
     */
    public static ExportResult success(String taskId, String fileUrl, Long fileSize) {
        return new ExportResult(true, taskId, "导出完成", fileUrl, fileSize, null);
    }

    /**
     * 创建失败结果
     *
     * @param message 错误消息
     * @return 失败结果实例
     */
    public static ExportResult fail(String message) {
        return fail(null, message);
    }

    /**
     * 创建失败结果（包含任务ID）
     *
     * @param taskId  任务ID
     * @param message 错误消息
     * @return 失败结果实例
     */
    public static ExportResult fail(String taskId, String message) {
        return new ExportResult(false, taskId, message, "", 0L, 0L);
    }

    /**
     * 创建异步处理结果
     *
     * @param taskId 任务ID
     * @return 异步处理结果实例
     */
    public static ExportResult asyncSuccess(String taskId) {
        return new ExportResult(true, taskId, "导出任务已提交，正在后台处理", null, null, null);
    }

    /**
     * 设置耗时
     *
     * @param costTime 耗时（毫秒）
     * @return 当前实例（支持链式调用）
     */
    public ExportResult withCostTime(Long costTime) {
        this.costTime = costTime;
        return this;
    }

    /**
     * 设置文件大小
     *
     * @param fileSize 文件大小（字节）
     * @return 当前实例（支持链式调用）
     */
    public ExportResult withFileSize(Long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    /**
     * 检查是否包含文件URL
     *
     * @return 如果包含文件URL返回true，否则返回false
     */
    public boolean hasFileUrl() {
        return fileUrl != null && !fileUrl.trim().isEmpty();
    }

}
