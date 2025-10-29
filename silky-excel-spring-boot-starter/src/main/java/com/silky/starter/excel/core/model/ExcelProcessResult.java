package com.silky.starter.excel.core.model;

import lombok.AllArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
public class ExcelProcessResult implements Serializable {

    private static final long serialVersionUID = 3164749477679693130L;

    /**
     * 导入是否成功
     */
    private boolean success;

    /**
     * 导入任务ID
     */
    private String taskId;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 总数据量
     */
    private Long totalCount;

    /**
     * 成功导入数量
     */
    private Long successCount;

    /**
     * 失败导入数量
     */
    private Long failedCount;

    /**
     * 跳过数量
     */
    private Long skippedCount;

    /**
     * 错误详情列表
     */
    private List<ImportError> errors;

    /**
     * 导入耗时（毫秒）
     */
    private Long costTime;

    /**
     * 统计信息
     */
    private Map<String, Object> statistics;

    /**
     * 创建成功结果
     */
    public static ExcelProcessResult success(String taskId, Long totalCount, Long successCount, Long costTime) {
        return new ExcelProcessResult(true, taskId, "导入完成", totalCount, successCount,
                0L, 0L, Collections.emptyList(), null, null);
    }

    /**
     * 创建部分成功结果
     */
    public static ExcelProcessResult partialSuccess(String taskId, Long totalCount,
                                                    Long successCount, Long failedCount,
                                                    List<ImportError> errors) {
        return new ExcelProcessResult(true, taskId, "导入部分完成", totalCount, successCount,
                failedCount, 0L, errors, null, null);
    }

    /**
     * 创建失败结果
     */
    public static ExcelProcessResult fail(String taskId, String message) {
        return new ExcelProcessResult(false, taskId, message, 0L, 0L, 0L, 0L, null, null, null);
    }

    /**
     * 创建异步处理结果
     */
    public static ExcelProcessResult asyncSuccess(String taskId, String message, Long totalCount) {
        return new ExcelProcessResult(true, taskId, message,
                totalCount, null, null, 0L, null, null, null);
    }

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
     * 设置跳过数量
     */
    public ExcelProcessResult withSkippedCount(Long skippedCount) {
        this.skippedCount = skippedCount;
        return this;
    }

    /**
     * 计算成功率
     */
    public double getSuccessRate() {
        return totalCount > 0 ? (double) successCount / totalCount : 0.0;
    }

    /**
     * 计算失败率
     */
    public double getFailureRate() {
        return totalCount > 0 ? (double) failedCount / totalCount : 0.0;
    }

    /**
     * 获取结果摘要
     */
    public String getSummary() {
        return String.format("导入结果: 总数=%d, 成功=%d, 失败=%d, 跳过=%d, 成功率=%.1f%%",
                totalCount, successCount, failedCount, skippedCount, getSuccessRate() * 100);
    }

    /**
     * 导入错误信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError {
        private Integer rowIndex;
        private String fieldName;
        private String errorMessage;
        private String originalValue;

        public static ImportError of(Integer rowIndex, String fieldName, String errorMessage) {
            return new ImportError(rowIndex, fieldName, errorMessage, null);
        }

        public static ImportError of(Integer rowIndex, String fieldName, String errorMessage, String originalValue) {
            return new ImportError(rowIndex, fieldName, errorMessage, originalValue);
        }

        public String getErrorDetail() {
            return String.format("第%d行[%s]: %s", rowIndex, fieldName, errorMessage);
        }
    }

}
