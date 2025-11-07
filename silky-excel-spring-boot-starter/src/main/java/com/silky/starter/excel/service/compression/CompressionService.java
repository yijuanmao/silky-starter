package com.silky.starter.excel.service.compression;

import com.silky.starter.excel.properties.SilkyExcelProperties;

import java.io.File;
import java.io.IOException;

/**
 * 压缩服务接口
 *
 * @author zy
 * @date 2025-11-07 16:01
 **/
public interface CompressionService {

    /**
     * 压缩文件
     *
     * @param sourceFile 源文件
     * @param config     压缩配置
     * @param targetPath 目标路径
     * @return 压缩后的文件
     */
    File compressFile(File sourceFile, SilkyExcelProperties.CompressionConfig config, String targetPath) throws IOException;

    /**
     * 解压文件
     *
     * @param compressedFile 压缩文件
     * @param config         压缩配置
     * @param targetPath     目标路径
     * @return 解压后的文件
     */
    File decompressFile(File compressedFile, SilkyExcelProperties.CompressionConfig config, String targetPath) throws IOException;
}
