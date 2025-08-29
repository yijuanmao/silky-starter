package com.silky.starter.oss.model.param;

import com.silky.starter.oss.core.enums.OssFileTypeEnum;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 上传文件上下文
 *
 * @author zy
 * @date 2025-08-11 14:59
 **/
public class OssUploadParam implements Serializable {

    private static final long serialVersionUID = 8245811108012158232L;

    /**
     * 文件上传，，与stream二选一
     */
    private File file;

    /**
     * 输入流上传，与file二选一
     */
    private InputStream stream;

    /**
     * oss存取的相对路径，不必填，比如存储在oss路径为："upload/2023/01/11/cake.jpg"，相对路径为："upload/2023/01/11/"
     */
    private String path;

    /**
     * 保存oss文件名，不给会默认生成一个，上传文件相对路径 + 文件名，如："upload/2023/01/11/cake.jpg"，文件名为："cake.jpg"
     */
    private String fileName;

    /**
     * 过期时间，非必填，具体过期时间，比如当前时间是2025-04-25 10:00:00，过期时间是5分钟，那么就是2025-04-25 10:05:00
     */
    private LocalDateTime expiration;

    /**
     * 内容类型,参照枚举类:{@link com.fenmi.starter.oss.core.enums.OssFileTypeEnum}
     */
    private String contentType;

    /**
     * 自定义元数据
     */
    private Map<String, String> metadata;

    /**
     * 文件大小，单位为字节(B)，非必填
     */
    private Long fileSize;

    /**
     * 是否公开可读
     */
    private boolean publicRead = true;

    /**
     * 是否启用服务端加密
     */
    private boolean enableEncryption = false;

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

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public InputStream getStream() {
        return stream;
    }

    public void setStream(InputStream stream) {
        this.stream = stream;
    }

    public LocalDateTime getExpiration() {
        return expiration;
    }

    public void setExpiration(LocalDateTime expiration) {
        this.expiration = expiration;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean isPublicRead() {
        return publicRead;
    }

    public void setPublicRead(boolean publicRead) {
        this.publicRead = publicRead;
    }

    public boolean isEnableEncryption() {
        return enableEncryption;
    }

    public void setEnableEncryption(boolean enableEncryption) {
        this.enableEncryption = enableEncryption;
    }

    /**
     * 验证上传参数
     */
    public void validateForUploadParam() {
        if (this.stream == null && (this.file == null || !this.file.exists() || !this.file.isFile())) {
            throw new IllegalArgumentException("参数[stream]或[file]不能为空且必须是一个存在的文件");
        }
    }

    @Override
    public String toString() {
        return "OssUploadParam{" +
                "file=" + file +
                ", stream=" + stream +
                ", path='" + path + '\'' +
                ", fileName='" + fileName + '\'' +
                ", expiration=" + expiration +
                ", contentType='" + contentType + '\'' +
                ", metadata=" + metadata +
                ", fileSize=" + fileSize +
                ", publicRead=" + publicRead +
                ", enableEncryption=" + enableEncryption +
                '}';
    }
}
