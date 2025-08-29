package com.silky.starter.oss.model.result;

import java.io.Serializable;

/**
 * 分片上传结果
 *
 * @author zy
 * @date 2025-08-12 11:18
 **/
public class UploadPartResult implements Serializable {

    private static final long serialVersionUID = -2212326347054831174L;

    /**
     * 分片号
     */
    private Integer partNumber;

    /**
     * 分片的ETag值
     */
    private String etag;

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public UploadPartResult() {
    }

    public UploadPartResult(Integer partNumber, String etag) {
        this.partNumber = partNumber;
        this.etag = etag;
    }

    @Override
    public String toString() {
        return "UploadPartResult{" +
                "partNumber=" + partNumber +
                ", eTag='" + etag + '\'' +
                '}';
    }
}
