package com.silky.starter.excel.core.model;

import com.silky.starter.excel.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 导入、导出结果模型，封装导入操作的执行结果和统计信息
 *
 * @author zy
 * @date 2025-10-27 15:10
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelProcessResult implements Serializable {

    private static final long serialVersionUID = 3164749477679693130L;

    // ==================== 基础信息 ====================
    /**
     * 处理是否成功
     */
    private boolean success;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 处理类型：IMPORT/EXPORT
     */
    private TaskType processType;

    /**
     * 处理耗时（毫秒）
     */
    private Long costTime;

    // ==================== 数据统计信息 ====================
    /**
     * 总数据量
     */
    private Long totalCount;

    /**
     * 成功处理数量
     */
    private Long successCount;

    /**
     * 失败处理数量
     */
    private Long failedCount;

    /**
     * 跳过数量
     */
    private Long skippedCount;

    // ==================== 导出相关字段 ====================
    /**
     * 导出文件URL
     */
    private String fileUrl;

    /**
     * 导出文件大小（字节）
     */
    private Long fileSize;

    /**
     * 导出Sheet数量
     */
    private Integer sheetCount;

    // ==================== 导入相关字段 ====================
    /**
     * 错误详情列表
     */
    private List<ImportError> errors;

    /**
     * 统计信息
     */
    private Map<String, Object> statistics;

    /**
     * 创建导出成功结果
     */
    public static ExcelProcessResult exportSuccess(String taskId, String message, Long totalCount) {
        return ExcelProcessResult.builder()
                .success(true)
                .taskId(taskId)
                .message(message)
                .processType(TaskType.EXPORT)
                .totalCount(totalCount)
                .successCount(totalCount)
                .failedCount(0L)
                .skippedCount(0L)
                .errors(Collections.emptyList())
                .build();
    }

    /**
     * 创建导出成功结果（包含文件信息）
     */
    public static ExcelProcessResult exportSuccess(String taskId, String fileUrl, Long totalCount, Long fileSize) {
        return ExcelProcessResult.builder()
                .success(true)
                .taskId(taskId)
                .message("导出完成")
                .processType(TaskType.EXPORT)
                .fileUrl(fileUrl)
                .totalCount(totalCount)
                .successCount(totalCount)
                .failedCount(0L)
                .skippedCount(0L)
                .fileSize(fileSize)
                .errors(Collections.emptyList())
                .build();
    }

    /**
     * 创建异步导出结果
     */
    public static ExcelProcessResult exportAsyncSuccess(String taskId, String message, Long totalCount) {
        return ExcelProcessResult.builder()
                .success(true)
                .taskId(taskId)
                .message(message)
                .processType(TaskType.EXPORT)
                .totalCount(totalCount)
                .build();
    }

    // ==================== 静态工厂方法 - 导入场景 ====================

    /**
     * 创建导入成功结果
     */
    public static ExcelProcessResult importSuccess(String taskId, Long totalCount, Long successCount) {
        return ExcelProcessResult.builder()
                .success(true)
                .taskId(taskId)
                .message("导入完成")
                .processType(TaskType.IMPORT)
                .totalCount(totalCount)
                .successCount(successCount)
                .failedCount(0L)
                .skippedCount(totalCount - successCount)
                .errors(Collections.emptyList())
                .build();
    }

    /**
     * 创建导入部分成功结果
     */
    public static ExcelProcessResult importPartialSuccess(String taskId, Long totalCount,
                                                          Long successCount, Long failedCount,
                                                          List<ImportError> errors) {
        return ExcelProcessResult.builder()
                .success(true)
                .taskId(taskId)
                .message("导入部分完成")
                .processType(TaskType.IMPORT)
                .totalCount(totalCount)
                .successCount(successCount)
                .failedCount(failedCount)
                .skippedCount(totalCount - successCount - failedCount)
                .errors(errors != null ? errors : Collections.emptyList())
                .build();
    }

    /**
     * 创建导入失败结果
     */
    public static ExcelProcessResult importFail(String taskId, String message, List<ImportError> errors) {
        return ExcelProcessResult.builder()
                .success(false)
                .taskId(taskId)
                .message(message)
                .processType(TaskType.IMPORT)
                .failedCount(errors != null ? (long) errors.size() : 0L)
                .errors(errors != null ? errors : Collections.emptyList())
                .build();
    }

    // ==================== 通用静态工厂方法 ====================

    /**
     * 创建成功结果
     */
    public static ExcelProcessResult success(String taskId, String message, Long totalCount) {
        return ExcelProcessResult.builder()
                .success(true)
                .taskId(taskId)
                .message(message)
                .totalCount(totalCount)
                .successCount(totalCount)
                .failedCount(0L)
                .skippedCount(0L)
                .errors(Collections.emptyList())
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ExcelProcessResult fail(String taskId, String message) {
        return ExcelProcessResult.builder()
                .success(false)
                .taskId(taskId)
                .message(message)
                .failedCount(1L)
                .errors(Collections.emptyList())
                .build();
    }

    // ==================== 链式调用方法 ====================

    /**
     * 设置耗时
     */
    public ExcelProcessResult withCostTime(Long costTime) {
        this.costTime = costTime;
        return this;
    }

    /**
     * 设置统计信息
     */
    public ExcelProcessResult withStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
        return this;
    }

    /**
     * 设置文件URL
     */
    public ExcelProcessResult withFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
        return this;
    }

    /**
     * 设置文件大小
     */
    public ExcelProcessResult withFileSize(Long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    /**
     * 设置Sheet数量
     */
    public ExcelProcessResult withSheetCount(Integer sheetCount) {
        this.sheetCount = sheetCount;
        return this;
    }

    /**
     * 设置处理类型
     */
    public ExcelProcessResult withProcessType(TaskType processType) {
        this.processType = processType;
        return this;
    }

    // ==================== 业务方法 ====================

    /**
     * 计算成功率
     */
    public double getSuccessRate() {
        return totalCount != null && totalCount > 0 ?
                (double) (successCount != null ? successCount : 0) / totalCount : 0.0;
    }

    /**
     * 计算失败率
     */
    public double getFailureRate() {
        return totalCount != null && totalCount > 0 ?
                (double) (failedCount != null ? failedCount : 0) / totalCount : 0.0;
    }

    /**
     * 计算跳过率
     */
    public double getSkipRate() {
        return totalCount != null && totalCount > 0 ?
                (double) (skippedCount != null ? skippedCount : 0) / totalCount : 0.0;
    }

    /**
     * 获取结果摘要
     */
    public String getSummary() {
        if (TaskType.EXPORT.equals(processType)) {
            return String.format("导出结果: 总数=%d, 文件=%s, 大小=%d字节, 耗时=%dms",
                    totalCount != null ? totalCount : 0,
                    fileUrl != null ? fileUrl : "无",
                    fileSize != null ? fileSize : 0,
                    costTime != null ? costTime : 0);
        } else {
            return String.format("导入结果: 总数=%d, 成功=%d, 失败=%d, 跳过=%d, 成功率=%.1f%%, 耗时=%dms",
                    totalCount != null ? totalCount : 0,
                    successCount != null ? successCount : 0,
                    failedCount != null ? failedCount : 0,
                    skippedCount != null ? skippedCount : 0,
                    getSuccessRate() * 100,
                    costTime != null ? costTime : 0);
        }
    }

    /**
     * 是否有错误信息
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * 获取错误数量
     */
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }

    /**
     * 是否为导出结果
     */
    public boolean isExport() {
        return TaskType.EXPORT.equals(processType);
    }

    /**
     * 是否为导入结果
     */
    public boolean isImport() {
        return TaskType.IMPORT.equals(processType);
    }

    /**
     * 导入错误信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError implements Serializable {
        private static final long serialVersionUID = 1933498946663693778L;

        /**
         * 行索引（从1开始）
         */
        private Integer rowIndex;

        /**
         * 字段名称
         */
        private String fieldName;

        /**
         * 错误消息
         */
        private String errorMessage;

        /**
         * 原始值
         */
        private String originalValue;

        /**
         * 错误代码
         */
        private String errorCode;

        /**
         * 错误级别：ERROR/WARNING
         */
        private ErrorLevel errorLevel;

        public static ImportError of(Integer rowIndex, String fieldName, String errorMessage) {
            return ImportError.builder()
                    .rowIndex(rowIndex)
                    .fieldName(fieldName)
                    .errorMessage(errorMessage)
                    .errorLevel(ErrorLevel.ERROR)
                    .build();
        }

        public static ImportError of(Integer rowIndex, String fieldName, String errorMessage, String originalValue) {
            return ImportError.builder()
                    .rowIndex(rowIndex)
                    .fieldName(fieldName)
                    .errorMessage(errorMessage)
                    .originalValue(originalValue)
                    .errorLevel(ErrorLevel.ERROR)
                    .build();
        }

        public static ImportError warning(Integer rowIndex, String fieldName, String warningMessage) {
            return ImportError.builder()
                    .rowIndex(rowIndex)
                    .fieldName(fieldName)
                    .errorMessage(warningMessage)
                    .errorLevel(ErrorLevel.WARNING)
                    .build();
        }

        public String getErrorDetail() {
            return String.format("第%d行[%s]: %s", rowIndex, fieldName, errorMessage);
        }

        public boolean isError() {
            return ErrorLevel.ERROR.equals(errorLevel);
        }

        public boolean isWarning() {
            return ErrorLevel.WARNING.equals(errorLevel);
        }
    }

    /**
     * 错误级别枚举
     */
    public enum ErrorLevel {
        ERROR, WARNING
    }

}
