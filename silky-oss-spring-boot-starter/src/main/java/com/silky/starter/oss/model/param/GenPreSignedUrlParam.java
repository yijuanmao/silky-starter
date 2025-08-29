package com.silky.starter.oss.model.param;

import cn.hutool.core.util.StrUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 生成预签名URL参数
 *
 * @author zy
 * @date 2025-04-18 17:46
 **/
public class GenPreSignedUrlParam implements Serializable {

    private static final long serialVersionUID = 5723753575024240394L;

    /**
     * objectKey，必填,Bucket下的文件的路径名+文件名 如："upload/2023/01/11/cake.jpg"
     */
    private String objectKey;

    /**
     * 预签名URL的HTTP方法，必填,比如当前时间是2025-08-21 10:00:00，过期时间是 2025-08-21 11:00:00，
     */
    private LocalDateTime expiration;

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public LocalDateTime getExpiration() {
        return expiration;
    }

    public void setExpiration(LocalDateTime expiration) {
        this.expiration = expiration;
    }

    public GenPreSignedUrlParam() {
    }

    public GenPreSignedUrlParam(String objectKey, LocalDateTime expiration) {
        this.objectKey = objectKey;
        this.expiration = expiration;
    }

    /**
     * 验证预签名参数
     */
    public void validateParam() {
        if (StrUtil.isBlank(this.objectKey)) {
            throw new IllegalArgumentException("参数[objectKey]不能为空");
        }
        if (Objects.isNull(this.expiration)) {
            throw new IllegalArgumentException("参数[expiration]不能为空");
        }

    }

    @Override
    public String toString() {
        return "GenPreSignedUrlParam{" +
                "objectKey='" + objectKey + '\'' +
                ", expiration=" + expiration +
                '}';
    }
}
