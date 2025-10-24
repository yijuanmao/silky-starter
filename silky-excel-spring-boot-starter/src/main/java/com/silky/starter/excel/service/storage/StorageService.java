package com.silky.starter.excel.service.storage;

import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.enums.StorageType;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * 存储服务接口
 *
 * @author zy
 * @date 2025-10-24 13:52
 **/
public interface StorageService {

    /**
     * 上传文件到指定存储
     *
     * @param file        要上传的文件对象
     * @param fileName    原始文件名
     * @param storageType 存储类型
     * @param taskId      任务ID（用于元数据记录）
     * @return 文件访问URL
     */
    String upload(File file, String fileName, StorageType storageType, String taskId) throws ExcelExportException;

    /**
     * 获取文件访问URL
     *
     * @param fileKey     文件存储的唯一标识
     * @param storageType 存储类型
     * @return 文件访问URL，如果无法生成返回null
     */
    String getUrl(String fileKey, StorageType storageType) throws ExcelExportException;

    /**
     * 下载文件
     *
     * @param fileKey     文件存储的唯一标识
     * @param storageType 存储类型
     * @param response    HTTP响应对象
     */
    void download(String fileKey, StorageType storageType, HttpServletResponse response) throws ExcelExportException;

    /**
     * 删除文件
     *
     * @param fileKey     文件存储的唯一标识
     * @param storageType 存储类型
     * @return 如果删除成功返回true，否则返回false
     */
    boolean delete(String fileKey, StorageType storageType) throws ExcelExportException;

    /**
     * 检查文件是否存在
     *
     * @param fileKey     文件存储的唯一标识
     * @param storageType 存储类型
     * @return 如果文件存在返回true，否则返回false
     */
    boolean exists(String fileKey, StorageType storageType) throws ExcelExportException;

    /**
     * 获取文件大小
     *
     * @param fileKey     文件存储的唯一标识
     * @param storageType 存储类型
     * @return 文件大小（字节），如果无法获取返回-1
     */
    long getFileSize(String fileKey, StorageType storageType) throws ExcelExportException;

    /**
     * 获取默认存储类型
     *
     * @return 默认存储类型
     */
    StorageType getDefaultStorageType();
}
