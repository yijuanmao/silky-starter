package com.silky.starter.excel.core.model.imports;

import com.silky.starter.excel.core.model.export.ExportDataProcessor;
import com.silky.starter.excel.enums.StorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导入请求模型，封装Excel数据导入的所有配置参数
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
     * 数据类类型，用于Excel列到Java对象的映射
     */
    private Class<T> dataClass;

    /**
     * 导入文件名，包含文件扩展名，如："用户数据.xlsx"
     */
    private String fileName;

    /**
     * 文件访问URL或存储Key，指定要导入的Excel文件位置
     */
    private String fileUrl;

    /**
     * 数据导入器
     * 负责将处理后的数据持久化到目标系统
     */
    private DataImporterSupplier<T> dataImporterSupplier;


    // 以下参数可以为空

    /**
     * 存储类型，指定文件所在的存储系统
     */
    private StorageType storageType;

    /**
     * 业务类型，用于区分不同的导入业务场景
     */
    private String businessType;

    /**
     * 创建用户，发起导入任务的用户标识
     */
    private String createUser;

    /**
     * 查询参数，传递给数据处理器的额外参数
     */
    private Map<String, Object> params;

    /**
     * 数据处理器列表，对导入的数据进行校验、转换、过滤等处理
     */
    private List<ExportDataProcessor<T>> processors;

    /**
     * 分页大小
     * 每次处理的数据条数，默认1000条
     */
    private Integer pageSize;

    /**
     * 是否启用事务， 默认false，导入失败时回滚已处理数据
     */
    private boolean enableTransaction;

    /**
     * 是否跳过表头，默认false，跳过Excel的第一行（表头）
     */
    private boolean skipHeader;

    /**
     * 超时时间（毫秒），导入任务的最大执行时间
     */
    private Long timeout;

    /**
     * 最大错误数量，超过此数量时停止导入
     */
    private Integer maxErrorCount;
}
