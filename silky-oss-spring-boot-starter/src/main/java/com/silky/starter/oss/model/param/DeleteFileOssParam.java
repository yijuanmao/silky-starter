package com.silky.starter.oss.model.param;

import cn.hutool.core.util.StrUtil;

import java.io.Serializable;

/**
 * 删除文件oss参数
 *
 * @author zy
 * @date 2025-04-18 17:46
 **/
public class DeleteFileOssParam implements Serializable {

    private static final long serialVersionUID = -8979472826475578992L;

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

    public DeleteFileOssParam() {
    }

    public DeleteFileOssParam(String objectKey) {
        this.objectKey = objectKey;
    }

    public void validateParam() {
        if (StrUtil.isBlank(this.objectKey)) {
            throw new IllegalArgumentException("objectKey is null or empty");
        }
    }

    @Override
    public String toString() {
        return "DeleteFileOssParam{" +
                "objectKey='" + objectKey + '\'' +
                '}';
    }
}
