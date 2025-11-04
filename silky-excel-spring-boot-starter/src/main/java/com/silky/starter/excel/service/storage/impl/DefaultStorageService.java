package com.silky.starter.excel.service.storage.impl;

import cn.hutool.core.util.StrUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.storage.StorageStrategy;
import com.silky.starter.excel.core.storage.factory.StorageStrategyFactory;
import com.silky.starter.excel.enums.StorageType;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.service.storage.StorageService;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 存储服务实现类
 *
 * @author zy
 * @date 2025-10-24 13:54
 **/
@Slf4j
public class DefaultStorageService implements StorageService {

    private final StorageStrategyFactory strategyFactory;

    private final SilkyExcelProperties properties;

    public DefaultStorageService(StorageStrategyFactory strategyFactory,
                                 SilkyExcelProperties properties) {
        this.strategyFactory = strategyFactory;
        this.properties = properties;
    }

    /**
     * 上传文件到指定存储
     *
     * @param file        要上传的文件
     * @param fileName    原始文件名
     * @param storageType 存储类型
     * @param taskId      任务ID
     * @return 文件访问URL
     */
    @Override
    public String upload(File file, String fileName, StorageType storageType, String taskId) throws ExcelExportException {
        // 参数校验
        if (file == null || !file.exists()) {
            throw new ExcelExportException("文件不存在或为null");
        }
        if (StrUtil.isBlank(fileName)) {
            throw new ExcelExportException("文件名不能为空");
        }
        if (storageType == null) {
            storageType = getDefaultStorageType();
        }

        log.info("开始上传文件到{}存储: {}, 文件大小: {} bytes", storageType, fileName, file.length());

        // 获取对应的存储策略
        StorageStrategy strategy = strategyFactory.getStrategy(storageType);

        // 构建元数据
        Map<String, Object> metadata = new HashMap<>(8);
        metadata.put("taskId", taskId);
        metadata.put("fileName", fileName);
        metadata.put("uploadTime", LocalDateTime.now());
        metadata.put("fileSize", file.length());
        metadata.put("storageType", storageType.name());
        try {
            // 执行上传
            String fileKey = strategy.storeFile(file, fileName, metadata);
            String fileUrl = strategy.getFileUrl(fileKey);

            log.info("文件上传成功: {}, 存储Key: {}, 访问URL: {}",
                    fileName, fileKey, fileUrl);

            return fileUrl;
        } catch (Exception e) {
            log.error("文件上传失败: {}, 存储类型: {}", fileName, storageType, e);
            throw new ExcelExportException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件访问URL
     *
     * @param fileKey     文件Key
     * @param storageType 存储类型
     * @return 文件访问URL
     */
    @Override
    public String getUrl(String fileKey, StorageType storageType) throws ExcelExportException {
        // 参数校验
        if (StrUtil.isBlank(fileKey)) {
            throw new ExcelExportException("文件Key不能为空");
        }
        if (storageType == null) {
            storageType = getDefaultStorageType();
        }
        // 获取存储策略
        StorageStrategy strategy = strategyFactory.getStrategy(storageType);
        try {
            String fileUrl = strategy.getFileUrl(fileKey);
            log.debug("获取文件URL成功: {}, 存储类型: {}, URL: {}", fileKey, storageType, fileUrl);

            return fileUrl;
        } catch (Exception e) {
            log.error("获取文件URL失败: {}, 存储类型: {}", fileKey, storageType, e);
            throw new ExcelExportException("获取文件URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件
     *
     * @param fileKey     文件Key
     * @param storageType 存储类型
     * @param response    HTTP响应
     */
    @Override
    public void download(String fileKey, StorageType storageType, HttpServletResponse response) throws ExcelExportException {
        // 参数校验
        if (StrUtil.isBlank(fileKey)) {
            throw new ExcelExportException("文件Key不能为空");
        }
        if (response == null) {
            throw new ExcelExportException("HTTP响应不能为null");
        }
        if (storageType == null) {
            storageType = getDefaultStorageType();
        }
        log.info("开始下载文件: {}, 存储类型: {}", fileKey, storageType);
        // 获取存储策略
        StorageStrategy strategy = strategyFactory.getStrategy(storageType);
        try {
            // 执行下载
            strategy.downloadFile(fileKey, response);
            log.info("文件下载完成: {}, 存储类型: {}", fileKey, storageType);
        } catch (Exception e) {
            log.error("文件下载失败: {}, 存储类型: {}", fileKey, storageType, e);
            throw new ExcelExportException("文件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     *
     * @param fileKey     文件Key
     * @param storageType 存储类型
     * @return 删除是否成功
     */
    @Override
    public boolean delete(String fileKey, StorageType storageType) throws ExcelExportException {
        // 参数校验
        if (StrUtil.isBlank(fileKey)) {
            throw new ExcelExportException("文件Key不能为空");
        }
        if (storageType == null) {
            storageType = getDefaultStorageType();
        }
        log.info("开始删除文件: {}, 存储类型: {}", fileKey, storageType);
        // 获取存储策略
        StorageStrategy strategy = strategyFactory.getStrategy(storageType);

        try {
            boolean success = strategy.deleteFile(fileKey);
            if (success) {
                log.info("文件删除成功: {}, 存储类型: {}", fileKey, storageType);
            } else {
                log.warn("文件删除失败: {}, 存储类型: {}", fileKey, storageType);
            }
            return success;
        } catch (Exception e) {
            log.error("文件删除异常: {}, 存储类型: {}", fileKey, storageType, e);
            throw new ExcelExportException("文件删除异常: " + e.getMessage(), e);
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param fileKey     文件Key
     * @param storageType 存储类型
     * @return 文件是否存在
     */
    @Override
    public boolean exists(String fileKey, StorageType storageType) throws ExcelExportException {
        // 参数校验
        if (StrUtil.isBlank(fileKey)) {
            return false;
        }
        if (storageType == null) {
            storageType = getDefaultStorageType();
        }
        // 获取存储策略
        StorageStrategy strategy = strategyFactory.getStrategy(storageType);
        try {
            boolean exists = strategy.exists(fileKey);
            log.debug("文件存在检查: {}, 存储类型: {}, 存在: {}", fileKey, storageType, exists);

            return exists;

        } catch (Exception e) {
            log.error("文件存在检查异常: {}, 存储类型: {}", fileKey, storageType, e);
            return false;
        }
    }

    /**
     * 获取文件大小
     *
     * @param fileKey     文件Key
     * @param storageType 存储类型
     * @return 文件大小（字节）
     */
    @Override
    public long getFileSize(String fileKey, StorageType storageType) throws ExcelExportException {
        // 参数校验
        if (StrUtil.isBlank(fileKey)) {
            return -1;
        }
        if (storageType == null) {
            storageType = getDefaultStorageType();
        }
        // 获取存储策略
        StorageStrategy strategy = strategyFactory.getStrategy(storageType);

        try {
            long fileSize = strategy.getFileSize(fileKey);
            log.debug("获取文件大小: {}, 存储类型: {}, 大小: {} bytes",
                    fileKey, storageType, fileSize);

            return fileSize;

        } catch (Exception e) {
            log.error("获取文件大小异常: {}, 存储类型: {}", fileKey, storageType, e);
            return -1;
        }
    }

    /**
     * 获取默认存储类型
     *
     * @return 默认存储类型
     */
    @Override
    public StorageType getDefaultStorageType() {
        return properties.getStorage().getStorageType();
    }

    /**
     * 获取所有支持的存储类型
     *
     * @return 存储类型数组
     */
    public StorageType[] getSupportedStorageTypes() {
        return strategyFactory.getSupportedStorageTypes();
    }

    /**
     * 获取存储服务状态信息
     *
     * @return 状态信息描述
     */
    public String getStorageServiceStatus() {
        StorageType[] supportedTypes = getSupportedStorageTypes();
        StorageType defaultType = getDefaultStorageType();

        return String.format("存储服务状态: 默认类型=%s, 支持类型=%d个", defaultType, supportedTypes.length);
    }
}
