package com.silky.starter.excel.core.model.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导出请求参数。
 *
 * @author zy
 * @date 2025-10-24 11:35
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest<T> {

    /**
     * 数据类类型，用于生成表头和映射字段。
     */
    private Class<T> dataClass;

    /**
     * 导出文件名，如 user.xlsx。
     */
    private String fileName;

    /**
     * 单 Sheet 数据供应器。
     */
    private ExportDataSupplier<T> dataSupplier;

    /**
     * 多 Sheet 定义。设置后会忽略 dataClass/dataSupplier。
     */
    private List<ExportSheet<?>> sheets;

    /**
     * 每页从数据源读取的数据量。
     */
    private Integer pageSize;

    /**
     * 每个 Sheet 最大行数，超过后自动新建 Sheet。
     */
    private Long maxRowsPerSheet;

    /**
     * 业务类型，用于生成任务号和查询记录。
     */
    @Builder.Default
    private String businessType = "default_export";

    /**
     * 存储类型。为空时使用全局默认配置。
     */
    private String storageType;

    /**
     * 查询参数，会传给数据供应器。
     */
    private Map<String, Object> params;

    /**
     * 文件存储元数据，会传给 StorageStrategy。
     */
    private Map<String, Object> fileMetadata;

    /**
     * 是否记录导出进度。
     */
    private Boolean enableProgress;

    public static <T> ExportRequest<T> of(String fileName,
                                          Class<T> dataClass,
                                          ExportDataSupplier<T> dataSupplier) {
        return ExportRequest.<T>builder()
                .fileName(fileName)
                .dataClass(dataClass)
                .dataSupplier(dataSupplier)
                .build();
    }

    public static ExportRequest<?> sheets(String fileName, List<ExportSheet<?>> sheets) {
        return ExportRequest.builder()
                .fileName(fileName)
                .sheets(sheets)
                .build();
    }
}
