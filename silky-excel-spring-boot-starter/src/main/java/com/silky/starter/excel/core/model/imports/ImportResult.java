package com.silky.starter.excel.core.model.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 导入结果模型，封装导入操作的执行结果和统计信息
 *
 * @author zy
 * @date 2025-10-27 15:10
 **/
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult implements Serializable {

    private static final long serialVersionUID = 2870102703731870935L;

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
    public static ImportResult success(String taskId, Long totalCount, Long successCount) {
        return new ImportResult(true, taskId, "导入完成", totalCount, successCount,
                0L, 0L, null, null, null);
    }

    /**
     * 创建部分成功结果
     */
    public static ImportResult partialSuccess(String taskId, Long totalCount,
                                              Long successCount, Long failedCount,
                                              List<ImportError> errors) {
        return new ImportResult(true, taskId, "导入部分完成", totalCount, successCount,
                failedCount, 0L, errors, null, null);
    }

    /**
     * 创建失败结果
     */
    public static ImportResult fail(String taskId, String message) {
        return new ImportResult(false, taskId, message, 0L, 0L, 0L, 0L, null, null, null);
    }

    /**
     * 创建异步处理结果
     */
    public static ImportResult asyncSuccess(String taskId) {
        return new ImportResult(true, taskId, "导入任务已提交，正在后台处理",
                0L, 0L, 0L, 0L, null, null, null);
    }

    /**
     * 设置耗时
     */
    public ImportResult withCostTime(Long costTime) {
        this.costTime = costTime;
        return this;
    }

    /**
     * 设置跳过数量
     */
    public ImportResult withSkippedCount(Long skippedCount) {
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

        /**
         * 行号，从1开始
         */
        private Integer rowIndex;

        /**
         * 字段名称
         */
        private String fieldName;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 原始值
         */
        private String originalValue;

        /**
         * 所属Sheet名称
         */
        private String sheetName;

        public static ImportError of(Integer rowIndex, String fieldName, String errorMessage, String sheetName) {
            return new ImportError(rowIndex, fieldName, errorMessage, null, sheetName);
        }

        public static ImportError of(Integer rowIndex, String fieldName, String errorMessage, String originalValue, String sheetName) {
            return new ImportError(rowIndex, fieldName, errorMessage, originalValue, sheetName);
        }

        public String getErrorDetail() {
            return String.format("第%d行[%s]: %s", rowIndex, fieldName, errorMessage);
        }
    }

}
