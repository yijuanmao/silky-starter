package com.silky.starter.excel.core.storage;

import com.silky.starter.excel.core.exception.ExcelExportException;

import java.io.File;
import java.util.Map;

/**
 * 文件存储策略。
 * <p>
 * 内置 local，业务方可以通过实现该接口接入 OSS、COS、S3、MinIO 等存储。
 *
 * @author zy
 * @date 2025-10-24 11:41
 **/
public interface StorageStrategy {

    /**
     * 存储类型，如 local、oss、cos。
     */
    String getType();

    /**
     * 存储文件。
     */
    StorageObject store(File file, String fileName, Map<String, Object> metadata) throws ExcelExportException;

    /**
     * 下载文件到本地临时文件。
     */
    File download(String keyOrUrl) throws ExcelExportException;
}
