package com.silky.starter.oss.properties;

import com.silky.starter.oss.core.constant.OssConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OSS配置属性类
 *
 * @author zy
 * @date 2025-08-11 17:23
 **/
@ConfigurationProperties(prefix = OssConstants.CONFIGURATION_PREFIX)
public class OssProperties {

    /**
     * 服务商类型,参照枚举类:{@link com.silky.starter.oss.core.enums.ProviderType}
     */
    private String provider;

    /**
     * 访问密钥ID
     */
    private String accessKey;

    /**
     * 访问密钥
     */
    private String secretKey;

    /**
     * 默认存储桶名称
     */
    private String bucket;

    /**
     * 自动分片上传阈值，达到此大小则使用分片上传，默认 128MB
     */
    private long multipartThreshold = 128 * 1024 * 1024;

    /**
     * 自动分片上传时每个分片大小，默认 32MB
     */
    private long multipartPartSize = 32 * 1024 * 1024;

    /**
     * 阿里云配置
     */
    private AliyunConfig aliyun;

    /**
     * 华为云配置
     */
    private HuaWeiConfig huaWei;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public long getMultipartThreshold() {
        return multipartThreshold;
    }

    public void setMultipartThreshold(long multipartThreshold) {
        this.multipartThreshold = multipartThreshold;
    }

    public long getMultipartPartSize() {
        return multipartPartSize;
    }

    public void setMultipartPartSize(long multipartPartSize) {
        this.multipartPartSize = multipartPartSize;
    }

    public AliyunConfig getAliyun() {
        return aliyun;
    }

    public void setAliyun(AliyunConfig aliyun) {
        this.aliyun = aliyun;
    }

    public HuaWeiConfig getHuaWei() {
        return huaWei;
    }

    public void setHuaWei(HuaWeiConfig huaWei) {
        this.huaWei = huaWei;
    }

    @Override
    public String toString() {
        return "OssProperties{" +
                "provider='" + provider + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", bucket='" + bucket + '\'' +
                ", multipartThreshold=" + multipartThreshold +
                ", multipartPartSize=" + multipartPartSize +
                ", aliyun=" + aliyun +
                ", huaWei=" + huaWei +
                '}';
    }

    /**
     * 阿里云配置类
     */
    public static class AliyunConfig {

        private String endpoint;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public String toString() {
            return "AliyunConfig{" +
                    "endpoint='" + endpoint + '\'' +
                    '}';
        }
    }

    /**
     * 华为云配置类
     */
    public static class HuaWeiConfig {

        private String endpoint;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public String toString() {
            return "HuaWeiConfig{" +
                    "endpoint='" + endpoint + '\'' +
                    '}';
        }
    }
}
