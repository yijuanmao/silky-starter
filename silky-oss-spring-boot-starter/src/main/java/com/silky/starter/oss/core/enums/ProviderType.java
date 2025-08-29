package com.silky.starter.oss.core.enums;

import com.silky.starter.oss.core.constant.OssConstants;

/**
 * oss服务提供商枚举
 *
 * @author zy
 * @date 2025-08-11 15:54
 **/
public enum ProviderType {

    /**
     * 阿里云
     */
    ALIYUN(OssConstants.A_LI_YUN, "阿里云"),

    /**
     * 腾讯云
     */
    TENCENT(OssConstants.TEN_CENT, "腾讯云"),

    /**
     * 京东云
     */
    HUAWEI(OssConstants.HUA_WEI, "华为云"),

    ;

    private final String code;

    private final String msg;

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    ProviderType(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
