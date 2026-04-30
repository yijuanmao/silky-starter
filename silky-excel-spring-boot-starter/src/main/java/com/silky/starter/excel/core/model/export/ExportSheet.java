package com.silky.starter.excel.core.model.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多 Sheet 导出定义
 * 用于定义不同 Sheet 的数据源、表头和 Sheet 名称
 *
 * @author zy
 * @since 1.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportSheet<T> {

    /**
     * Sheet 名称
     */
    private String sheetName;

    /**
     * 数据类类型（用于表头生成）
     */
    private Class<T> dataClass;

    /**
     * 数据供应器
     */
    private ExportDataSupplier<T> dataSupplier;

    public static <T> ExportSheet<T> of(String sheetName, Class<T> dataClass, ExportDataSupplier<T> dataSupplier) {
        return ExportSheet.<T>builder()
                .sheetName(sheetName)
                .dataClass(dataClass)
                .dataSupplier(dataSupplier)
                .build();
    }
}
