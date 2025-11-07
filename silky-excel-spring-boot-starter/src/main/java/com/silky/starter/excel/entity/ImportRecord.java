package com.silky.starter.excel.entity;

import com.silky.starter.excel.enums.CompressionType;
import com.silky.starter.excel.enums.ImportStatus;
import com.silky.starter.excel.enums.StorageType;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 导入记录
 *
 * @author zy
 * @date 2025-10-27 15:33
 **/
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ImportRecord implements Serializable {

    private static final long serialVersionUID = 4915624779309835179L;

    /**
     * 导出任务唯一标识
     */
    private String taskId;

    /**
     * 业务类型标识
     */
    private String businessType;

    /**
     * 导出文件名
     */
    private String fileName;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 存储类型
     */
    private StorageType storageType;

    /**
     * 创建用户标识
     */
    private String createUser;

    /**
     * 导入状态
     */
    private ImportStatus status;


    /**
     * 任务创建时间
     */
    private LocalDateTime createTime;

    /**
     * 查询参数
     */
    private Map<String, Object> params;

    /**
     * 总数据量
     */
    private Long totalCount;

    /**
     * 成功导入数据量
     */
    private Long successCount;

    /**
     * 失败数据量
     */
    private Long failCount;

    /**
     * 压缩类型
     */
    private CompressionType compressionType;

    /**
     * 压缩开关
     */
    private Boolean compressionEnabled;
}
