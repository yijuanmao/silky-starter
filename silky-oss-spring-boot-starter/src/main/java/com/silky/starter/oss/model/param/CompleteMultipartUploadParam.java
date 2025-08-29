package com.silky.starter.oss.model.param;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * 完成分片上传参数
 *
 * @author zy
 * @date 2025-08-12 11:20
 **/
public class CompleteMultipartUploadParam implements Serializable {

    private static final long serialVersionUID = -183503392710600272L;

    /**
     * objectKey，必填,Bucket下的文件的路径名+文件名 如："upload/2023/01/11/cake.jpg"
     */
    private String objectKey;

    /**
     * 分片上传ID
     */
    private String uploadId;

    /**
     * 分片上传ID，
     */
    private Map<Integer, String> partETags;

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

    public Map<Integer, String> getPartETags() {
        return partETags;
    }

    public void setPartETags(Map<Integer, String> partETags) {
        this.partETags = partETags;
    }

    public CompleteMultipartUploadParam() {
    }

    public CompleteMultipartUploadParam(String objectKey, String uploadId, Map<Integer, String> partETags) {
        this.objectKey = objectKey;
        this.uploadId = uploadId;
        this.partETags = partETags;
    }

    public void validateParam() {
        if (StrUtil.isBlank(this.objectKey)) {
            throw new IllegalArgumentException("objectKey is null or empty");
        }
        if (StrUtil.isBlank(this.uploadId)) {
            throw new IllegalArgumentException("uploadId is null or empty");
        }
        if (CollUtil.isEmpty(this.partETags)) {
            throw new IllegalArgumentException("partETags is null or empty");
        }
    }

    @Override
    public String toString() {
        return "CompleteMultipartUploadParam{" +
                "objectKey='" + objectKey + '\'' +
                ", uploadId='" + uploadId + '\'' +
                ", partETags=" + partETags +
                '}';
    }
}
