package com.silky.starter.excel.core.model;

import com.silky.starter.excel.core.exception.ExcelExportException;

import java.util.Map;

/**
 * 导出数据提供者,负责分页获取要导出的数据，支持大数据量的分页处理
 *
 * @author zy
 * @date 2025-10-24 11:24
 **/
@FunctionalInterface
public interface ExportDataSupplier<T> {

    /**
     * 分页获取数据
     * 此方法会被多次调用，直到返回的数据为空或hasNext为false
     *
     * @param pageNum  当前页码，从1开始
     * @param pageSize 每页数据大小，由ExportRequest中的pageSize参数指定
     * @param params   查询参数，来自ExportRequest中的params参数
     * @return 分页数据对象，包含当前页数据和是否有下一页的标识
     */
    ExportPageData<T> getPageData(int pageNum, int pageSize, Map<String, Object> params) throws ExcelExportException;

    /**
     * 获取数据总条数（可选实现）
     * 如果实现此方法，可以提供更准确的进度信息
     *
     * @param params 查询参数
     * @return 数据总条数，如果无法获取返回-1
     */
    default long getTotalCount(Map<String, Object> params) throws ExcelExportException {
        return -1L;
    }

    /**
     * 数据供应器准备方法（可选实现）
     * 在导出开始前调用，用于初始化资源或验证参数
     *
     * @param params 查询参数
     */
    default void prepare(Map<String, Object> params) throws ExcelExportException {
        // 默认空实现
    }

    /**
     * 数据供应器清理方法（可选实现）
     * 在导出结束后调用，用于释放资源
     *
     * @param params 查询参数
     */
    default void cleanup(Map<String, Object> params) throws ExcelExportException {
        // 默认空实现
    }
}
