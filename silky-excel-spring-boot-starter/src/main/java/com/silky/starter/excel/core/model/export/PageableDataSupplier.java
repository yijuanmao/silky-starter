package com.silky.starter.excel.core.model.export;

/**
 * 可分页数据供应器接口
 *
 * @author zy
 * @date 2025-11-07 16:41
 **/
public interface PageableDataSupplier<T> extends ExportDataSupplier<T> {

    /**
     * 估算总数据量
     */
    long estimateTotalCount(Object params);
}
