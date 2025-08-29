package com.silky.starter.oss.model.param;

import java.io.Serializable;
import java.util.Map;

/**
 * 分片上传参数类
 *
 * @author zy
 * @date 2025-08-12 10:42
 **/
public class InitiateMultipartUploadParam implements Serializable {

    private static final long serialVersionUID = 6218868692163995724L;

    /**
     * oss存取的相对路径，不必填，比如存储在oss路径为："upload/2023/01/11/cake.jpg"，相对路径为："upload/2023/01/11/"
     */
    private String path;

    /**
     * 保存oss文件名，不给会默认生成一个，上传文件相对路径 + 文件名，如："upload/2023/01/11/cake.jpg"，文件名为："cake.jpg"
     */
    private String fileName;

    /**
     * 自定义元数据
     */
    private Map<String, String> metadata;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public InitiateMultipartUploadParam() {
    }

    public InitiateMultipartUploadParam(String path, String fileName, Map<String, String> metadata) {
        this.path = path;
        this.fileName = fileName;
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "InitiateMultipartUploadParam{" +
                "path='" + path + '\'' +
                ", fileName='" + fileName + '\'' +
                ", metadata=" + metadata +
                '}';
    }
}
