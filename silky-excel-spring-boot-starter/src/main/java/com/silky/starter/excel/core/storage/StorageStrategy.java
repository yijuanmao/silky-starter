package com.silky.starter.excel.core.storage;

import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.enums.StorageType;

import java.io.File;
import java.util.Map;

/**
 * 存储策略接口
 * 定义文件存储、下载和访问URL生成的标准方法
 * 支持多种存储方式：本地、OSS、Redis、MongoDB等
 *
 * @author zy
 * @date 2025-10-24 11:41
 **/
public interface StorageStrategy {

    /**
     * 存储文件
     *
     * @param file     要存储的文件对象
     * @param fileName 原始文件名
     * @param metadata 元数据，可以包含任务ID、创建时间等信息
     * @return 文件存储后的唯一标识（文件Key）
     */
    String storeFile(File file, String fileName, Map<String, Object> metadata) throws ExcelExportException;

    /**
     * 下载文件
     *
     * @param fileKey 文件存储的唯一标识
     */
    File downloadFile(String fileKey) throws ExcelExportException;

    /**
     * 获取文件访问URL
     *
     * @param fileKey 文件存储的唯一标识
     * @return 文件访问URL，如果无法生成URL返回null
     */
    String getFileUrl(String fileKey) throws ExcelExportException;

    /**
     * 删除文件
     *
     * @param fileKey 文件存储的唯一标识
     * @return 如果删除成功返回true，否则返回false
     */
    boolean deleteFile(String fileKey) throws ExcelExportException;

    /**
     * 获取存储类型
     *
     * @return 存储类型枚举
     */
    StorageType getStorageType();

    /**
     * 检查文件是否存在
     *
     * @param fileKey 文件存储的唯一标识
     * @return 如果文件存在返回true，否则返回false
     */
    default boolean exists(String fileKey) throws ExcelExportException {
        // 默认实现，子类可以重写
        return false;
    }

    /**
     * 获取文件大小
     *
     * @param fileKey 文件存储的唯一标识
     * @return 文件大小（字节），如果无法获取返回-1
     */
    default long getFileSize(String fileKey) throws ExcelExportException {
        // 默认实现，子类可以重写
        return -1;
    }
}
