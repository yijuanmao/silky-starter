package com.silky.starter.oss.model.param;

import cn.hutool.core.util.StrUtil;

import java.io.InputStream;
import java.io.Serializable;

/**
 * 上传分片
 *
 * @author zy
 * @date 2025-04-18 17:46
 **/
public class UploadPartParam implements Serializable {

    private static final long serialVersionUID = -3783800489012725522L;

    /**
     * objectKey，必填,Bucket下的文件的路径名+文件名 如："upload/2023/01/11/cake.jpg"
     */
    private String objectKey;

    /**
     * 分片上传ID，
     */
    private String uploadId;

    /**
     * 分片号,从1开始
     */
    private int partNumber;

    /**
     * 分片数据流
     */
    private InputStream inputStream;

    /**
     * 分片大小
     */
    private long partSize;

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public long getPartSize() {
        return partSize;
    }

    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    public UploadPartParam() {
    }

    public UploadPartParam(String objectKey, String uploadId, int partNumber, InputStream inputStream, long partSize) {
        this.objectKey = objectKey;
        this.uploadId = uploadId;
        this.partNumber = partNumber;
        this.inputStream = inputStream;
        this.partSize = partSize;
    }

    public void validateParam() {
        if (StrUtil.isBlank(this.objectKey)) {
            throw new IllegalArgumentException("objectKey is null or empty");
        }
        if (StrUtil.isBlank(this.uploadId)) {
            throw new IllegalArgumentException("uploadId is null or empty");
        }
        if (partNumber <= 0) {
            throw new IllegalArgumentException("partNumber must be greater than 0");
        }
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream is null");
        }
        if (partSize <= 0) {
            throw new IllegalArgumentException("partSize must be greater than 0");
        }
    }


    @Override
    public String toString() {
        return "UploadPartParam{" +
                "objectKey='" + objectKey + '\'' +
                ", uploadId='" + uploadId + '\'' +
                ", partNumber=" + partNumber +
                ", inputStream=" + inputStream +
                ", partSize=" + partSize +
                '}';
    }
}
