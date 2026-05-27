package com.silky.starter.excel.core.model.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 导入请求参数。
 *
 * @author zy
 * @date 2025-10-27 15:05
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRequest<T> {

    /**
     * 数据类类型，用于 Excel 行到 Java 对象的映射。
     */
    private Class<T> dataClass;

    /**
     * 导入文件名。
     */
    private String fileName;

    /**
     * 文件访问 URL 或存储 key。
     */
    private String fileUrl;

    /**
     * 数据导入器，负责批量写入业务系统。
     */
    private DataImporterSupplier<T> dataImporterSupplier;

    /**
     * 存储类型。为空时使用全局默认配置。
     */
    private String storageType;

    /**
     * 业务类型，用于生成任务号和查询记录。
     */
    @Builder.Default
    private String businessType = "default_import";

    /**
     * 业务参数，会传给数据导入器。
     */
    private Map<String, Object> params;

    /**
     * 每批读取并提交给业务导入器的数据量。
     */
    private Integer pageSize;

    /**
     * 表头行数。
     */
    private Integer headRowNumber;

    /**
     * 最大错误数量。
     */
    private Integer maxErrorCount;

    public static <T> ImportRequest<T> of(String fileUrl,
                                          Class<T> dataClass,
                                          DataImporterSupplier<T> dataImporterSupplier) {
        return ImportRequest.<T>builder()
                .fileUrl(fileUrl)
                .fileName(fileUrl)
                .dataClass(dataClass)
                .dataImporterSupplier(dataImporterSupplier)
                .build();
    }
}
