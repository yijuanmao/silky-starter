package com.silky.starter.oss.core.enums;

/**
 * 上传类型枚举
 *
 * @author zy
 * @date 2025-08-11 15:51
 **/
public enum UploadType {

    /**
     * 普通上传
     */
    STANDARD,

    /**
     * 分片上传
     */
    MULTIPART,

    /**
     * 断点续传
     */
    RESUME
}
