package com.silky.starter.excel.core.model.export;

import com.silky.starter.excel.core.model.DataProcessor;
import com.silky.starter.excel.enums.CompressionType;
import com.silky.starter.excel.enums.StorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导出请求参数
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
     * 数据类类型
     * 用于Excel表头生成和数据映射
     * 不能为null
     */
    private Class<T> dataClass;

    /**
     * 导出文件名
     * 包含文件扩展名，如："用户列表.xlsx"
     * 不能为null或空字符串
     */
    private String fileName;

    /**
     * 数据供应器
     * 负责分页获取要导出的数据
     * 不能为null
     */
    private ExportDataSupplier<T> dataSupplier;


    // ========== 可选参数 ==========

    /**
     * 业务类型
     * 用于区分不同的导出业务，便于统计和管理
     * 默认值："default_export"
     */
    @Builder.Default
    private String businessType = "default_export";

    /**
     * 存储类型
     * 指定导出文件的存储方式
     * 默认值：LOCAL
     */
    private StorageType storageType;

    /**
     * 分页大小
     * 每次从数据供应器获取的数据条数
     * 默认值：2000
     */
    @Builder.Default
    private int pageSize = 2000;

    /**
     * 查询参数
     * 传递给数据供应器的额外参数
     * 默认值：空Map
     */
    private Map<String, Object> params;

    /**
     * 数据处理器列表
     * 对数据进行转换、过滤、脱敏等处理
     * 默认值：空列表
     */
    private List<DataProcessor<T>> processors;

    /**
     * 创建用户
     * 发起导出任务的用户标识
     * 默认值："system"
     */
    @Builder.Default
    private String createUser = "system";

    /**
     * 是否启用进度跟踪
     * 启用后会实时更新导出进度
     * 默认值：true
     */
    @Builder.Default
    private boolean enableProgress = true;

    /**
     * 超时时间（分钟）
     * 导出任务的最大执行时间，超时后任务会被取消
     * 默认值：60（1小时）
     */
    private Long timeout;

    /**
     * 每个Sheet的最大行数
     * 超过该行数会自动创建新Sheet
     * 默认值：200000
     */
    private Long maxRowsPerSheet;

    /**
     * 是否启用压缩
     */
    private boolean compressionEnabled;

    /**
     * 压缩类型
     */
    private CompressionType compressionType;

    /**
     * 压缩级别 0-9
     */
    private int compressionLevel;

    /**
     * 是否分割大文件
     */
    private boolean splitLargeFiles;

    /**
     * 分割大小（字节）
     */
    private long splitSize;

}
