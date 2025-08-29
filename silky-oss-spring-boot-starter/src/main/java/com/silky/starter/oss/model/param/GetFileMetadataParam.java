package com.silky.starter.oss.model.param;

import cn.hutool.core.util.StrUtil;

import java.io.Serializable;

/**
 * 获取文件元数据参数
 *
 * @author zy
 * @date 2025-08-12 11:20
 **/
public class GetFileMetadataParam implements Serializable {

    private static final long serialVersionUID = -4344168811786527138L;

    /**
     * objectKey，必填,Bucket下的文件的路径名+文件名 如："upload/2023/01/11/cake.jpg"
     */
    private String objectKey;


    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public GetFileMetadataParam() {
    }

    public GetFileMetadataParam(String objectKey) {
        this.objectKey = objectKey;
    }

    public void validateParam() {
        if (StrUtil.isBlank(this.objectKey)) {
            throw new IllegalArgumentException("objectKey不能为空");
        }
    }

    @Override
    public String toString() {
        return "GetFileMetadataParam{" +
                "objectKey='" + objectKey + '\'' +
                '}';
    }
}
