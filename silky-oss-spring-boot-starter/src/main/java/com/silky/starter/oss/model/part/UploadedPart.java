package com.silky.starter.oss.model.part;

import java.time.LocalDateTime;

/**
 * 已上传的分片
 *
 * @author zy
 * @date 2025-08-11 15:46
 **/
public class UploadedPart {

    /**
     * 分片序号
     */
    private int partNumber;

    /**
     * 分片ETag
     */
    private String etag;

    /**
     * 分片大小
     */
    private long size;

    /**
     * 最后修改时间
     */
    private LocalDateTime lastUpdateTime;

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public UploadedPart() {
    }

    public UploadedPart(int partNumber, String etag, long size, LocalDateTime lastUpdateTime) {
        this.partNumber = partNumber;
        this.etag = etag;
        this.size = size;
        this.lastUpdateTime = lastUpdateTime;
    }

    @Override
    public String toString() {
        return "UploadedPart{" +
                "partNumber=" + partNumber +
                ", eTag='" + etag + '\'' +
                ", size=" + size +
                ", lastModified=" + lastUpdateTime +
                '}';
    }
}
