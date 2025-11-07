package com.silky.starter.excel.core.model.export;

import cn.hutool.core.collection.CollectionUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

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
     * 任务ID
     */
    private String taskId;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 总耗时（毫秒）
     */
    private Long costTime;

    /**
     * 总数
     */
    private Long totalCount;

    /**
     * 成功数
     */
    private Long successCount;

    /**
     * 失败数
     */
    private Long failedCount;

    /**
     * 跳过数
     */
    private Long skippedCount;

    /**
     * 跳过数
     */
    private Integer sheetCount;

    /**
     * 错误列表
     */
    private List<ExportError> errors;


    /**
     * 是否启用压缩
     */
    private Boolean compressionEnabled;

    /**
     * 压缩类型
     */
    private String compressionType;

    /**
     * 压缩比
     */
    private Double compressionRatio;

    @Data
    @Accessors(chain = true)
    public static class ExportError {

        /**
         * 行号
         */
        private Integer rowNumber;

        /**
         * 字段名
         */
        private String fieldName;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 错误码
         */
        private String errorCode;

        /**
         * 原始值
         */
        private Object originalValue;

        public static ExportError of(Integer rowNumber, String fieldName, String errorMessage) {
            return new ExportError()
                    .setRowNumber(rowNumber)
                    .setFieldName(fieldName)
                    .setErrorMessage(errorMessage);
        }

        public static ExportError of(Integer rowNumber, String errorMessage) {
            return new ExportError()
                    .setRowNumber(rowNumber)
                    .setErrorMessage(errorMessage);
        }
    }


    /**
     * 导出成功
     *
     * @param taskId 任务ID
     * @return
     */
    public static ExportResult success(String taskId) {
        return new ExportResult()
                .setTaskId(taskId)
                .setSuccess(true)
                .setMessage("导出成功")
                .setSuccessCount(0L)
                .setFailedCount(0L)
                .setSkippedCount(0L)
                .setErrors(Collections.emptyList());
    }

    /**
     * 导出成功
     *
     * @param taskId       任务ID
     * @param totalCount   总数
     * @param successCount 成功数
     * @return ExportResult
     */
    public static ExportResult success(String taskId, Long totalCount, Long successCount) {
        return new ExportResult()
                .setTaskId(taskId)
                .setSuccess(true)
                .setMessage("导出成功")
                .setTotalCount(totalCount)
                .setSuccessCount(successCount)
                .setFailedCount(0L)
                .setSkippedCount(0L)
                .setErrors(Collections.emptyList());
    }

    /**
     * 异步导出成功
     *
     * @param taskId 任务ID
     * @return ExportResult
     */
    public static ExportResult asyncSuccess(String taskId) {
        return new ExportResult()
                .setTaskId(taskId)
                .setSuccess(true)
                .setMessage("异步导出任务已提交")
                .setErrors(Collections.emptyList());
    }

    public static ExportResult fail(String taskId, String message) {
        return new ExportResult()
                .setTaskId(taskId)
                .setSuccess(false)
                .setMessage(message)
                .setErrors(Collections.emptyList());
    }

    public static ExportResult partialSuccess(String taskId) {
        return new ExportResult()
                .setTaskId(taskId)
                .setSuccess(false)
                .setMessage("部分数据导出成功")
                .setErrors(Collections.emptyList());
    }

    /**
     * 部分数据导出成功
     *
     * @param taskId       任务ID
     * @param totalCount   总数
     * @param successCount 成功数
     * @param failedCount  失败数
     * @return ExportResult
     */
    public static ExportResult partialSuccess(String taskId, Long totalCount, Long successCount, Long failedCount) {
        return partialSuccess(taskId, totalCount, successCount, failedCount, Collections.emptyList());
    }

    public static ExportResult partialSuccess(String taskId, Long totalCount, Long successCount,
                                              Long failedCount, List<ExportError> errors) {
        return new ExportResult()
                .setTaskId(taskId)
                .setSuccess(false)
                .setMessage(String.format("部分数据导出成功: 总数=%d, 成功=%d, 失败=%d, 错误=%d",
                        totalCount, successCount, failedCount, errors.size()))
                .setTotalCount(totalCount)
                .setSuccessCount(successCount)
                .setFailedCount(failedCount)
                .setSkippedCount(0L)
                .setErrors(CollectionUtil.isEmpty(errors) ? Collections.emptyList() : errors);
    }


    /**
     * 获取导出结果摘要
     *
     * @return String
     */
    public String getSummary() {
        if (success) {
            if (totalCount != null) {
                return String.format("导出成功: 总数=%d, 成功=%d", totalCount, successCount);
            } else {
                return "导出成功";
            }
        } else {
            if (totalCount != null) {
                return String.format("导出失败: 总数=%d, 成功=%d, 失败=%d, 错误=%d",
                        totalCount, successCount, failedCount, errors.size());
            } else {
                return String.format("导出失败: %s", message);
            }
        }
    }

    public Double getSuccessRate() {
        if (totalCount == null || totalCount == 0) {
            return 0.0;
        }
        return successCount != null ? (double) successCount / totalCount : 0.0;
    }

    public Double getFailureRate() {
        if (totalCount == null || totalCount == 0) {
            return 0.0;
        }
        return failedCount != null ? (double) failedCount / totalCount : 0.0;
    }

    public ExportResult addError(ExportError error) {
        if (this.errors == null) {
            this.errors = Collections.emptyList();
        }
        this.errors.add(error);
        return this;
    }

    public ExportResult addError(Integer rowNumber, String fieldName, String errorMessage) {
        return addError(ExportError.of(rowNumber, fieldName, errorMessage));
    }

    public ExportResult addError(Integer rowNumber, String errorMessage) {
        return addError(ExportError.of(rowNumber, errorMessage));
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }

    // 设置压缩信息
    public ExportResult withCompressionInfo(boolean enabled, String type, Double ratio) {
        this.compressionEnabled = enabled;
        this.compressionType = type;
        this.compressionRatio = ratio;
        return this;
    }

    // 计算压缩比
    public void calculateCompressionRatio(Long originalSize) {
        if (originalSize != null && fileSize != null && originalSize > 0) {
            this.compressionRatio = 1 - (double) fileSize / originalSize;
        }
    }

}
