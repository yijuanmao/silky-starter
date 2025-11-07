package com.silky.starter.excel.service.compression.impl;

import com.silky.starter.excel.properties.SilkyExcelProperties;
import com.silky.starter.excel.service.compression.CompressionService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;

/**
 * 压缩服务实现类
 *
 * @author zy
 * @date 2025-11-07 16:01
 **/
@Slf4j
public class CompressionServiceImpl implements CompressionService {

    private final SilkyExcelProperties properties;

    private final SilkyExcelProperties.CompressionConfig compressionConfig;

    public CompressionServiceImpl(SilkyExcelProperties properties) {
        this.properties = properties;
        this.compressionConfig = properties.getCompression();
    }


    /**
     * 压缩文件
     *
     * @param sourceFile 源文件
     * @param config     压缩配置
     * @param targetPath 目标路径
     * @return 压缩后的文件
     */
    @Override
    public File compressFile(File sourceFile, SilkyExcelProperties.CompressionConfig config, String targetPath) throws IOException {
        if (!config.isEnabled()) {
            return sourceFile;
        }
        switch (config.getType().name().toUpperCase()) {
            case "ZIP":
                return compressZip(sourceFile, config, targetPath);
            case "GZIP":
                return compressGzip(sourceFile, config, targetPath);
            default:
                throw new IllegalArgumentException("不支持的压缩类型: " + config.getType());
        }
    }

    /**
     * 解压文件
     *
     * @param compressedFile 压缩文件
     * @param config         压缩配置
     * @param targetPath     目标路径
     */
    @Override
    public File decompressFile(File compressedFile, SilkyExcelProperties.CompressionConfig config, String targetPath) throws IOException {
        if (!config.isEnabled()) {
            return compressedFile;
        }
        switch (config.getType().name().toUpperCase()) {
            case "ZIP":
                return decompressZip(compressedFile, targetPath);
            case "GZIP":
                return decompressGzip(compressedFile, targetPath);
            default:
                throw new IllegalArgumentException("不支持的压缩类型: " + config.getType());
        }
    }

    private File compressZip(File sourceFile, SilkyExcelProperties.CompressionConfig config, String targetPath) throws IOException {
        String zipFilePath = targetPath + ".zip";
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            zos.setLevel(config.getCompressionLevel());

            ZipEntry zipEntry = new ZipEntry(sourceFile.getName());
            zos.putNextEntry(zipEntry);

            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            }
            zos.closeEntry();
        }

        File zipFile = new File(zipFilePath);
        log.info("文件压缩完成: {} -> {}, 压缩前: {} bytes, 压缩后: {} bytes",
                sourceFile.getName(), zipFile.getName(), sourceFile.length(), zipFile.length());

        return zipFile;
    }

    /**
     * 压缩文件
     *
     * @param sourceFile 源文件
     * @param config     压缩配置
     * @param targetPath 目标路径
     * @return 压缩后的文件
     */
    private File compressGzip(File sourceFile, SilkyExcelProperties.CompressionConfig config, String targetPath) throws IOException {
        String gzipFilePath = targetPath + ".gz";
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(gzipFilePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, length);
            }
        }

        File gzipFile = new File(gzipFilePath);
        log.info("文件GZIP压缩完成: {} -> {}, 压缩前: {} bytes, 压缩后: {} bytes",
                sourceFile.getName(), gzipFile.getName(), sourceFile.length(), gzipFile.length());

        return gzipFile;
    }

    /**
     * 解压文件
     *
     * @param compressedFile 压缩文件
     * @param targetPath     目标路径
     * @return 解压后的文件
     */
    private File decompressZip(File compressedFile, String targetPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(compressedFile.toPath()))) {
            ZipEntry zipEntry = zis.getNextEntry();
            if (zipEntry == null) {
                throw new IOException("ZIP文件为空或损坏");
            }

            File outputFile = new File(targetPath);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }
            zis.closeEntry();

            log.info("ZIP文件解压完成: {} -> {}", compressedFile.getName(), outputFile.getName());
            return outputFile;
        }
    }

    private File decompressGzip(File compressedFile, String targetPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(compressedFile);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(targetPath)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }

        File outputFile = new File(targetPath);
        log.info("GZIP文件解压完成: {} -> {}", compressedFile.getName(), outputFile.getName());
        return outputFile;
    }

    /**
     * 检查文件是否需要分割
     */
    public boolean needsSplitting(File file, SilkyExcelProperties.CompressionConfig config) {
        return config.isSplitLargeFiles() && file.length() > config.getSplitSize();
    }

    /**
     * 分割大文件
     */
    public List<File> splitLargeFile(File sourceFile, SilkyExcelProperties.CompressionConfig config, String outputDir) throws IOException {
        List<File> parts = new ArrayList<>();
        long partSize = config.getSplitSize();
        long fileSize = sourceFile.length();
        int partCount = (int) Math.ceil((double) fileSize / partSize);

        try (FileInputStream fis = new FileInputStream(sourceFile)) {
            byte[] buffer = new byte[8192];

            for (int i = 0; i < partCount; i++) {
                String partFileName = String.format("%s.part%03d", sourceFile.getName(), i + 1);
                File partFile = new File(outputDir, partFileName);

                try (FileOutputStream fos = new FileOutputStream(partFile)) {
                    long bytesRemaining = partSize;
                    int bytesRead;

                    while (bytesRemaining > 0 && (bytesRead = fis.read(buffer, 0,
                            (int) Math.min(buffer.length, bytesRemaining))) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        bytesRemaining -= bytesRead;
                    }
                }

                parts.add(partFile);
                log.debug("文件分割完成: {} -> {}", sourceFile.getName(), partFile.getName());
            }
        }

        log.info("大文件分割完成: {} -> {}个部分", sourceFile.getName(), partCount);
        return parts;
    }
}
