package com.silky.starter.excel.core.storage.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.storage.StorageObject;
import com.silky.starter.excel.core.storage.StorageStrategy;
import com.silky.starter.excel.enums.StorageType;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import org.slf4j.Logger;

import java.io.File;
import java.util.Map;

/**
 * 本地存储策略
 *
 * @author zy
 * @date 2025-10-24 11:44
 **/
public class LocalStorageStrategy implements StorageStrategy {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(LocalStorageStrategy.class);

    private final SilkyExcelProperties properties;

    public LocalStorageStrategy(SilkyExcelProperties properties) {
        this.properties = properties;
    }

    /**
     * 存储文件
     *
     * @param file     文件对象
     * @param fileName 文件名称
     * @param metadata 文件元数据
     * @return 存储对象
     */
    @Override
    public StorageObject storeFile(File file, String fileName, Map<String, Object> metadata) {
        try {
            String exportPath = properties.getStorage().getLocal().getBasePath();
            String fileKey = generateFileKey(fileName);
            File targetFile = new File(exportPath, fileKey);

            // 确保目录存在
            FileUtil.mkdir(exportPath);
            // 复制文件
            FileUtil.copy(file, targetFile, true);

            long fileSize = targetFile.length();
            String fileUrl = exportPath + File.separator + fileKey;

            log.info("文件已保存到本地: {}, 大小: {} bytes", targetFile.getAbsolutePath(), fileSize);

            return StorageObject.builder()
                    .key(fileKey)
                    .url(fileUrl)
                    .size(fileSize)
                    .build();
        } catch (Exception e) {
            log.error("本地文件存储失败", e);
            throw new ExcelExportException("本地文件存储失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param key 文件唯一标识
     */
    @Override
    public File downloadFile(String key) {
        try {
            String exportPath = properties.getStorage().getLocal().getBasePath();
            File sourceFile = new File(exportPath, key);
            if (!sourceFile.exists()) {
                throw new ExcelExportException("文件不存在: " + key);
            }
            String originalFileName = FileUtil.getName(key);
            String safeFileName = FileUtil.cleanInvalid(originalFileName);
            String tempFileName = "silky_import_" + System.currentTimeMillis() + "_" + safeFileName;

            File tempFile = new File(System.getProperty("java.io.tmpdir"), tempFileName);
            FileUtil.copy(sourceFile, tempFile, true);

            return tempFile;
        } catch (ExcelExportException e) {
            throw e;
        } catch (Exception e) {
            log.error("从本地下载文件失败", e);
            throw new ExcelExportException("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件访问URL
     *
     * @param key 文件唯一标识
     */
    @Override
    public String getFileUrl(String key) {
        return properties.getStorage().getLocal().getBasePath() + File.separator + key;
    }

    /**
     * 删除文件
     *
     * @param key 文件唯一标识
     */
    @Override
    public boolean deleteFile(String key) {
        try {
            String exportPath = properties.getStorage().getLocal().getBasePath();
            File file = new File(exportPath, key);
            return file.exists() && file.delete();
        } catch (Exception e) {
            log.error("删除本地文件失败", e);
            return false;
        }
    }

    /**
     * 存储类型
     */
    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    /**
     * 检查文件是否存在
     */
    @Override
    public boolean exists(String key) {
        String exportPath = properties.getStorage().getLocal().getBasePath();
        File file = new File(exportPath, key);
        return file.exists();
    }

    /**
     * 获取文件大小
     */
    @Override
    public long getFileSize(String key) {
        String exportPath = properties.getStorage().getLocal().getBasePath();
        File file = new File(exportPath, key);
        return file.exists() ? file.length() : -1;
    }

    /**
     * 生成文件唯一标识
     */
    private String generateFileKey(String fileName) {
        String uuid = IdUtil.fastSimpleUUID();
        return uuid + "_" + fileName;
    }
}
