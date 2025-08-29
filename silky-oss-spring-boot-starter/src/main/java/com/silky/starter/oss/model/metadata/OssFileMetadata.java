package com.silky.starter.oss.model.metadata;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 文件元数据
 *
 * @author zy
 * @date 2025-08-11 15:06
 **/
public class OssFileMetadata implements Serializable {

    private static final long serialVersionUID = -1682571655450976344L;

    /**
     * Bucket名称
     */
    private String bucketName;

    /**
     * 对象名称
     */
    private String objectKey;

    /**
     * 文件MIME类型
     */
    private String contentType;

    /**
     * 内容长度
     */
    private long contentLength;

    /**
     * 最后修改时间
     */
    private LocalDateTime lastUpdateTime;

    /**
     * 文件唯一标识
     */
    private String etag;

    /**
     * 存储类型
     */
    private String storageClass;

    /**
     * 用户自定义元数据
     */
    private Map<String, String> userMetadata;

    /**
     * 内容编码
     */
    private String contentEncoding;

    /**
     * 内容语言
     */
    private String contentDisposition;

    /**
     * 缓存控制
     */
    private String cacheControl;

    /**
     * 版本ID
     */
    private String versionId;

    /**
     * 服务器端加密算法
     */
    private String serverSideEncryption;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    public Map<String, String> getUserMetadata() {
        return userMetadata;
    }

    public void setUserMetadata(Map<String, String> userMetadata) {
        this.userMetadata = userMetadata;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public String getCacheControl() {
        return cacheControl;
    }

    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getServerSideEncryption() {
        return serverSideEncryption;
    }

    public void setServerSideEncryption(String serverSideEncryption) {
        this.serverSideEncryption = serverSideEncryption;
    }

    @Override
    public String toString() {
        return "OssFileMetadata{" +
                "bucketName='" + bucketName + '\'' +
                ", objectKey='" + objectKey + '\'' +
                ", contentType='" + contentType + '\'' +
                ", contentLength=" + contentLength +
                ", lastModified=" + lastUpdateTime +
                ", eTag='" + etag + '\'' +
                ", storageClass='" + storageClass + '\'' +
                ", userMetadata=" + userMetadata +
                ", contentEncoding='" + contentEncoding + '\'' +
                ", contentDisposition='" + contentDisposition + '\'' +
                ", cacheControl='" + cacheControl + '\'' +
                ", versionId='" + versionId + '\'' +
                ", serverSideEncryption='" + serverSideEncryption + '\'' +
                '}';
    }
}
