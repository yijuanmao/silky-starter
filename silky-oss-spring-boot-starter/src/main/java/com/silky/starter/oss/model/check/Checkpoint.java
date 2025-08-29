package com.silky.starter.oss.model.check;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 断点续传检查点
 *
 * @author zy
 * @date 2025-08-11 15:34
 **/
public class Checkpoint implements Serializable {

    private static final long serialVersionUID = -4589517140021303588L;

    /**
     * 检查点的唯一标识
     * 用于在系统中唯一标识此断点续传状态
     */
    private String id;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 序列化ID
     */
    private String uploadId;

    /**
     * Bucket名称
     */
    private String bucketName;

    /**
     * Object名称
     */
    private String objectKey;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小
     */
    private long fileSize;

    /**
     * 已上传大小
     */
    private long uploadedSize;

    /**
     * 分片大小（字节）
     * 用于分片上传的分片大小
     */
    private long partSize;

    /**
     * 已上传分片的ETag映射
     */
    private Map<Integer, String> partETags;

    /**
     * 待上传分片序号
     */
    private List<Integer> remainingParts;

    /**
     * 服务商标识
     */
    private String provider;

    /**
     * 用户自定义数据
     * 用于存储与断点续传相关的用户自定义信息
     */
    private Map<String, String> userData = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getUploadedSize() {
        return uploadedSize;
    }

    public void setUploadedSize(long uploadedSize) {
        this.uploadedSize = uploadedSize;
    }

    public long getPartSize() {
        return partSize;
    }

    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    public Map<Integer, String> getPartETags() {
        return partETags;
    }

    public void setPartETags(Map<Integer, String> partETags) {
        this.partETags = partETags;
    }

    public List<Integer> getRemainingParts() {
        return remainingParts;
    }

    public void setRemainingParts(List<Integer> remainingParts) {
        this.remainingParts = remainingParts;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, String> getUserData() {
        return userData;
    }

    public void setUserData(Map<String, String> userData) {
        this.userData = userData;
    }

    /**
     * 添加已完成分片
     *
     * @param partNumber 分片号
     * @param eTag       ETag
     */
    public void addCompletedPart(int partNumber, String eTag) {
        partETags.put(partNumber, eTag);
        remainingParts.remove(Integer.valueOf(partNumber));
    }

    @Override
    public String toString() {
        return "Checkpoint{" +
                "id='" + id + '\'' +
                ", createdTime=" + createdTime +
                ", uploadId='" + uploadId + '\'' +
                ", bucketName='" + bucketName + '\'' +
                ", objectKey='" + objectKey + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileSize=" + fileSize +
                ", uploadedSize=" + uploadedSize +
                ", partSize=" + partSize +
                ", partETags=" + partETags +
                ", remainingParts=" + remainingParts +
                ", provider='" + provider + '\'' +
                ", userData=" + userData +
                '}';
    }
}
