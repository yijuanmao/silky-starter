package com.silky.starter.oss.model.result;

import java.io.Serializable;

/**
 * 分片上传初始化结果
 *
 * @author zy
 * @date 2025-08-12 10:55
 **/
public class InitiateMultipartResult implements Serializable {

    private static final long serialVersionUID = -2569127972332200210L;

    /**
     * 文件objectKey
     */
    private String objectKey;

    /**
     * 分片上传的唯一标识符
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

    public InitiateMultipartResult() {
    }

    public InitiateMultipartResult(String objectKey, String uploadId) {
        this.objectKey = objectKey;
        this.uploadId = uploadId;
    }

    @Override
    public String toString() {
        return "InitiateMultipartResult{" +
                "objectKey='" + objectKey + '\'' +
                ", uploadId='" + uploadId + '\'' +
                '}';
    }
}
