package com.silky.starter.excel.core.storage.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.storage.StorageStrategy;
import com.silky.starter.excel.enums.StorageType;
import com.silky.starter.excel.properties.SilkyExcelProperties;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
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
     * @return 文件唯一标识
     */
    @Override
    public String storeFile(File file, String fileName, Map<String, Object> metadata) {
        try {
            String exportPath = properties.getStorage().getLocal().getBasePath();
            String fileKey = generateFileKey(fileName);
            File targetFile = new File(exportPath, fileKey);

            // 确保目录存在
            FileUtil.mkdir(exportPath);
            // 复制文件
            FileUtil.copy(file, targetFile, true);

            log.info("文件已保存到本地: {}", targetFile.getAbsolutePath());
            return fileKey;
        } catch (Exception e) {
            log.error("本地文件存储失败", e);
            throw new ExcelExportException("本地文件存储失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param fileKey  文件唯一标识
     * @param response Http响应对象
     */
    @Override
    public void downloadFile(String fileKey, HttpServletResponse response) {
        try {
            String exportPath = properties.getStorage().getLocal().getBasePath();
            File file = new File(exportPath, fileKey);
            if (!file.exists()) {
                throw new ExcelExportException("文件不存在: " + fileKey);
            }
            // 设置响应头
            String fileName = getFileNameFromKey(fileKey);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");
            response.setHeader("Content-Length", String.valueOf(file.length()));

            // 流式传输
            try (FileInputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = response.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("从本地下载文件失败", e);
            throw new ExcelExportException("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件访问URL
     *
     * @param fileKey 文件唯一标识
     */
    @Override
    public String getFileUrl(String fileKey) {
        // 本地存储返回相对路径，实际项目中可能需要配置域名
        return properties.getStorage().getLocal().getBasePath() + fileKey;
    }

    /**
     * 删除文件
     *
     * @param fileKey 文件唯一标识
     */
    @Override
    public boolean deleteFile(String fileKey) {
        try {
            String exportPath = properties.getStorage().getLocal().getBasePath();
            File file = new File(exportPath, fileKey);
            return file.exists() && file.delete();
        } catch (Exception e) {
            log.error("删除本地文件失败", e);
            return false;
        }
    }

    /**
     * 存储类型
     *
     * @return 存储类型
     */
    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    /**
     * 生成文件唯一标识
     *
     * @param fileName 文件名称
     * @return 文件唯一标识
     */
    private String generateFileKey(String fileName) {
        String uuid = IdUtil.fastSimpleUUID();
        return uuid + "_" + fileName;
    }

    /**
     * 从文件唯一标识中提取原始文件名
     *
     * @param fileKey 文件唯一标识
     * @return 原始文件名
     */
    private String getFileNameFromKey(String fileKey) {
        if (fileKey.contains("_")) {
            return fileKey.substring(fileKey.lastIndexOf("_") + 1);
        }
        return fileKey;
    }
}
