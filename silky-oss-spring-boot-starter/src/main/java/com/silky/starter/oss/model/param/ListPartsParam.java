package com.silky.starter.oss.model.param;

import cn.hutool.core.util.StrUtil;

import java.io.Serializable;

/**
 * 列出分片上传参数
 *
 * @author zy
 * @date 2025-08-12 11:20
 **/
public class ListPartsParam implements Serializable {

    private static final long serialVersionUID = -1482649203664653899L;

    /**
     * objectKey，必填,Bucket下的文件的路径名+文件名 如："upload/2023/01/11/cake.jpg"
     */
    private String objectKey;

    /**
     * 上传ID，必填, 标识本次分片上传的ID
     */
    private String uploadId;

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

    public ListPartsParam() {
    }

    public ListPartsParam(String objectKey, String uploadId) {
        this.objectKey = objectKey;
        this.uploadId = uploadId;
    }

    public void validateParam() {
        if (StrUtil.isBlank(this.objectKey)) {
            throw new IllegalArgumentException("objectKey is null");
        }
        if (StrUtil.isBlank(this.uploadId)) {
            throw new IllegalArgumentException("uploadId is null");
        }
    }

    @Override
    public String toString() {
        return "ListPartsParam{" +
                "objectKey='" + objectKey + '\'' +
                ", uploadId='" + uploadId + '\'' +
                '}';
    }
}
