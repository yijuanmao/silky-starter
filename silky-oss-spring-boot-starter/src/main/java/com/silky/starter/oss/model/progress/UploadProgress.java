package com.silky.starter.oss.model.progress;

import com.fenmi.starter.oss.core.enums.UploadType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 上传进度
 *
 * @author zy
 * @date 2025-08-11 16:17
 **/
public class UploadProgress implements Serializable {

    private static final long serialVersionUID = 1656804014639621534L;

    /**
     * 上传ID
     */
    private String uploadId;

    /**
     * 上传类型
     */
    private UploadType uploadType; // 枚举：STANDARD, MULTIPART, RESUMABLE

    /**
     * 总大小，单位字节
     */
    private long totalSize;

    /**
     * 已上传大小，单位字节
     */
    private long uploadedSize;

    /**
     * 上传进度，范围0.0 - 1.0
     */
    private double progress; // 0.0 - 1.0

    /**
     * 总分片数
     */
    private int totalParts;

    /**
     * 已完成分片数
     */
    private int completedParts;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 预计完成时间
     */
    private LocalDateTime estimatedCompletionTime;


    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public UploadType getUploadType() {
        return uploadType;
    }

    public void setUploadType(UploadType uploadType) {
        this.uploadType = uploadType;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getUploadedSize() {
        return uploadedSize;
    }

    public void setUploadedSize(long uploadedSize) {
        this.uploadedSize = uploadedSize;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public int getTotalParts() {
        return totalParts;
    }

    public void setTotalParts(int totalParts) {
        this.totalParts = totalParts;
    }

    public int getCompletedParts() {
        return completedParts;
    }

    public void setCompletedParts(int completedParts) {
        this.completedParts = completedParts;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEstimatedCompletionTime() {
        return estimatedCompletionTime;
    }

    public void setEstimatedCompletionTime(LocalDateTime estimatedCompletionTime) {
        this.estimatedCompletionTime = estimatedCompletionTime;
    }

    /**
     * 获取上传进度
     *
     * @return 上传进度
     */
    public double getProgress() {
        return (double) uploadedSize / totalSize;
    }

    @Override
    public String toString() {
        return "UploadProgress{" +
                "uploadId='" + uploadId + '\'' +
                ", uploadType=" + uploadType +
                ", totalSize=" + totalSize +
                ", uploadedSize=" + uploadedSize +
                ", progress=" + progress +
                ", totalParts=" + totalParts +
                ", completedParts=" + completedParts +
                ", startTime=" + startTime +
                ", estimatedCompletionTime=" + estimatedCompletionTime +
                '}';
    }
}
