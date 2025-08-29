package com.silky.starter.oss.model.result;

import java.io.Serializable;

/**
 * 完成分片上传结果
 *
 * @author zy
 * @date 2025-08-12 11:42
 **/
public class CompleteMultipartUploadResult implements Serializable {

    /**
     * 上传id
     */
    private String uploadId;

    /**
     * 文件下载url
     */
    private String downloadUrl;

    /**
     * 分片ETag
     */
    private String etag;

    /**
     * Bucket下的文件的路径名+文件名 如："upload/2023/01/11/cake.jpg"
     */
    private String objectKey;

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public CompleteMultipartUploadResult() {
    }

    public CompleteMultipartUploadResult(String uploadId, String downloadUrl, String etag, String objectKey) {
        this.uploadId = uploadId;
        this.downloadUrl = downloadUrl;
        this.etag = etag;
        this.objectKey = objectKey;
    }

    @Override
    public String toString() {
        return "CompleteMultipartUploadResult{" +
                "uploadId='" + uploadId + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", eTag='" + etag + '\'' +
                ", objectKey='" + objectKey + '\'' +
                '}';
    }
}
