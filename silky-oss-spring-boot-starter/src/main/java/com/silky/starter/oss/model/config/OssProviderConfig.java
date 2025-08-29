package com.silky.starter.oss.model.config;

import java.io.Serializable;

/**
 * OSS配置参数类
 *
 * @author zy
 * @date 2025-08-11 16:07
 **/
public class OssProviderConfig implements Serializable {

    private static final long serialVersionUID = -866871867230049917L;

    /**
     * 访问域名，必填
     */
    private String endpoint;

    /**
     * accessKeyId，必填
     */
    private String accessKey;

    /**
     * 秘钥，必填
     */
    private String secretKey;

    /**
     * 存储空间，必填
     */
    private String bucketName;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public OssProviderConfig() {
    }

    public OssProviderConfig(String endpoint, String accessKey, String secretKey, String bucketName) {
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
    }

    @Override
    public String toString() {
        return "OssProviderConfig{" +
                "endpoint='" + endpoint + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", bucketName='" + bucketName + '\'' +
                '}';
    }
}
