package com.silky.starter.excel.core.model.imports;

import com.silky.starter.excel.core.exception.ExcelExportException;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 数据导入器接口，负责将处理后的数据持久化到目标系统
 *
 * @author zy
 * @date 2025-10-27 15:12
 **/
public interface DataImporterSupplier<T> {

    /**
     * 导入数据
     * 将数据列表持久化到目标系统
     *
     * @param dataList 要导入的数据列表
     * @param params   导入参数
     * @return 导入结果，包含成功数量和错误信息
     */
    ImportBatchResult importData(List<T> dataList, Map<String, Object> params) throws ExcelExportException;

    /**
     * 导入前准备
     * 在导入开始前调用，用于初始化资源
     *
     * @param params 导入参数
     */
    default void prepare(Map<String, Object> params) throws ExcelExportException {
        // 默认空实现
    }

    /**
     * 导入后清理
     * 在导入结束后调用，用于释放资源
     *
     * @param params 导入参数
     */
    default void cleanup(Map<String, Object> params) throws ExcelExportException {
        // 默认空实现
    }

    /**
     * 开始事务（如果启用事务）
     */
    default void beginTransaction() throws ExcelExportException {
        // 默认空实现
    }

    /**
     * 提交事务
     */
    default void commitTransaction() throws ExcelExportException {
        // 默认空实现
    }

    /**
     * 回滚事务
     */
    default void rollbackTransaction() throws ExcelExportException {
        // 默认空实现
    }

    /**
     * 批量导入结果
     */
    @Getter
    class ImportBatchResult {

        /**
         * 成功导入数量
         */
        private final long successCount;

        /**
         * 导入错误列表
         */
        private final List<ImportResult.ImportError> errors;

        public ImportBatchResult(long successCount, List<ImportResult.ImportError> errors) {
            this.successCount = successCount;
            this.errors = errors != null ? errors : new java.util.ArrayList<>();
        }

        public static ImportBatchResult success(long successCount) {
            return new ImportBatchResult(successCount, null);
        }

        public static ImportBatchResult withErrors(long successCount, List<ImportResult.ImportError> errors) {
            return new ImportBatchResult(successCount, errors);
        }

        public long getFailedCount() {
            return errors.size();
        }
    }
}
