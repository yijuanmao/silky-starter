package com.silky.starter.oss.model.result;

import com.silky.starter.oss.model.metadata.OssFileMetadata;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 阿里云oss上传结果
 *
 * @author zy
 * @date 2025-04-18 18:00
 **/
public class OssUploadResult implements Serializable {

    private static final long serialVersionUID = -525449806837273797L;

    /**
     * 上传id
     */
    private String uploadId;

    /**
     * 文件下载url
     */
    private String downloadUrl;

    /**
     * 上传到oss后的文件名称
     */
    private String fileName;

    /**
     * Bucket下的文件的路径名+文件名 如："upload/2023/01/11/cake.jpg"
     */
    private String objectKey;

    /**
     * 上传结果.true:成功,false:失败
     */
    private boolean uploadState;

    /**
     * 响应msg
     */
    private String returnMsg;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 上传完成时间
     */
    private LocalDateTime completionTime;

    /**
     * 文件元数据
     */
    private OssFileMetadata metadata;

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public boolean isUploadState() {
        return uploadState;
    }

    public void setUploadState(boolean uploadState) {
        this.uploadState = uploadState;
    }

    public String getReturnMsg() {
        return returnMsg;
    }

    public void setReturnMsg(String returnMsg) {
        this.returnMsg = returnMsg;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }

    public OssFileMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(OssFileMetadata metadata) {
        this.metadata = metadata;
    }

    public OssUploadResult() {
    }

    public OssUploadResult(String downloadUrl, String fileName, String objectKey, boolean uploadState, String returnMsg) {
        this(downloadUrl, fileName, objectKey, uploadState, returnMsg, uploadState ? LocalDateTime.now() : null);
    }

    public OssUploadResult(String downloadUrl, String fileName, String objectKey, boolean uploadState, String returnMsg, LocalDateTime completionTime) {
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
        this.objectKey = objectKey;
        this.uploadState = uploadState;
        this.returnMsg = returnMsg;
        this.completionTime = completionTime;
    }

    @Override
    public String toString() {
        return "OssUploadResult{" +
                "uploadId='" + uploadId + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", fileName='" + fileName + '\'' +
                ", objectKey='" + objectKey + '\'' +
                ", uploadState=" + uploadState +
                ", returnMsg='" + returnMsg + '\'' +
                ", fileSize=" + fileSize +
                ", completionTime=" + completionTime +
                ", metadata=" + metadata +
                '}';
    }
}
