package com.silky.starter.oss.model.result;

import java.io.Serializable;

/**
 * 预签名url上传结果
 *
 * @author zy
 * @date 2025-04-18 18:00
 **/
public class GenPreSignedUrlResult implements Serializable {

    private static final long serialVersionUID = 7179466104857689049L;

    /**
     * 预签名url
     */
    private String signedUrl;

    public String getSignedUrl() {
        return signedUrl;
    }

    public void setSignedUrl(String signedUrl) {
        this.signedUrl = signedUrl;
    }

    public GenPreSignedUrlResult() {
    }

    public GenPreSignedUrlResult(String signedUrl) {
        this.signedUrl = signedUrl;
    }

    @Override
    public String toString() {
        return "GenPreSignedUrlResult{" +
                "signedUrl='" + signedUrl + '\'' +
                '}';
    }
}
