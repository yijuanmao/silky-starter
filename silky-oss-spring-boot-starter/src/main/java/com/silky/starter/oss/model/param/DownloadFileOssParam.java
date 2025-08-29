package com.silky.starter.oss.model.param;

import cn.hutool.core.util.StrUtil;

import java.io.Serializable;

/**
 * 下载文件oss参数
 *
 * @author zy
 * @date 2025-04-18 17:46
 **/
public class DownloadFileOssParam implements Serializable {

    private static final long serialVersionUID = -5432977823042678534L;

    /**
     * objectKey，必填,Bucket下的文件的路径名+文件名 如："upload/2023/01/11/cake.jpg"
     */
    private String objectKey;

    /**
     * 下载本地文件存取路径，必填,
     */
    private String localFilePath;

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    public DownloadFileOssParam() {
    }

    public DownloadFileOssParam(String objectKey, String localFilePath) {
        this.objectKey = objectKey;
        this.localFilePath = localFilePath;
    }

    public void validateParam() {
        if (StrUtil.isBlank(this.objectKey)) {
            throw new IllegalArgumentException("objectKey is null or empty");
        }
        if (StrUtil.isBlank(this.localFilePath)) {
            throw new IllegalArgumentException("localFilePath is null or empty");
        }
    }

    @Override
    public String toString() {
        return "DownloadFileOssParam{" +
                "objectKey='" + objectKey + '\'' +
                ", localFilePath='" + localFilePath + '\'' +
                '}';
    }
}
