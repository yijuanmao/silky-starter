package com.silky.starter.excel.core.storage.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.storage.StorageObject;
import com.silky.starter.excel.core.storage.StorageStrategy;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Map;

/**
 * 本地文件存储策略。
 *
 * @author zy
 * @date 2025-10-24 11:44
 **/
@Slf4j
public class LocalStorageStrategy implements StorageStrategy {

    public static final String TYPE = "local";

    private final SilkyExcelProperties properties;

    public LocalStorageStrategy(SilkyExcelProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public StorageObject store(File file, String fileName, Map<String, Object> metadata) {
        try {
            String exportPath = properties.getStorage().getLocal().getBasePath();
            String fileKey = generateFileKey(fileName);
            File targetFile = new File(exportPath, fileKey);

            FileUtil.mkdir(exportPath);
            FileUtil.copy(file, targetFile, true);

            long fileSize = targetFile.length();
            String fileUrl = targetFile.getAbsolutePath();

            log.info("Excel 文件已保存到本地: {}, 大小: {} bytes", fileUrl, fileSize);

            return StorageObject.builder()
                    .key(fileKey)
                    .url(fileUrl)
                    .size(fileSize)
                    .build();
        } catch (Exception e) {
            log.error("本地文件存储失败", e);
            throw new ExcelExportException("本地文件存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public File download(String keyOrUrl) {
        try {
            File sourceFile = resolveSourceFile(keyOrUrl);
            if (!sourceFile.exists()) {
                throw new ExcelExportException("文件不存在: " + keyOrUrl);
            }

            String safeFileName = FileUtil.cleanInvalid(sourceFile.getName());
            String tempFileName = "silky_import_" + System.currentTimeMillis() + "_" + safeFileName;
            File tempFile = new File(System.getProperty("java.io.tmpdir"), tempFileName);
            FileUtil.copy(sourceFile, tempFile, true);
            return tempFile;
        } catch (ExcelExportException e) {
            throw e;
        } catch (Exception e) {
            log.error("从本地下载文件失败", e);
            throw new ExcelExportException("文件下载失败: " + e.getMessage(), e);
        }
    }

    private File resolveSourceFile(String keyOrUrl) {
        File directFile = new File(keyOrUrl);
        if (directFile.isAbsolute() || directFile.exists()) {
            return directFile;
        }
        return new File(properties.getStorage().getLocal().getBasePath(), keyOrUrl);
    }

    private String generateFileKey(String fileName) {
        return IdUtil.fastSimpleUUID() + "_" + FileUtil.cleanInvalid(fileName);
    }
}
