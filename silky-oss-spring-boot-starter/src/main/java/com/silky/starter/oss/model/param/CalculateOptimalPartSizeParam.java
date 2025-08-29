package com.silky.starter.oss.model.param;

import com.silky.starter.oss.adapter.OssProviderAdapter;

import java.io.Serializable;
import java.util.Objects;

/**
 * 计算最佳分片大小参数类
 *
 * @author zy
 * @date 2025-08-14 11:16
 **/
public class CalculateOptimalPartSizeParam implements Serializable {

    private static final long serialVersionUID = 4564568070170255988L;

    /**
     * 文件大小
     */
    private long fileSize;

    /**
     * 文件类型，参照枚举类:{@link com.silky.starter.oss.core.enums.OssFileTypeEnum}
     */
    private String fileType;

    /**
     * OSS提供商适配器
     */
    private OssProviderAdapter adapter;

    /**
     * 平台分片数量限制，默认值为1000,现在统一默认1000
     */
    private int partCountLimit = 1000;

    /**
     * 基础分片大小，单位为字节,默认值为10MB
     */
    private long basePartSize = 10 * 1024 * 1024;

    /**
     * 最小分片大小，单位为字节，默认值为1MB
     */
    private long minPartSize = 1024 * 1024;

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public OssProviderAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(OssProviderAdapter adapter) {
        this.adapter = adapter;
    }

    public long getBasePartSize() {
        return basePartSize;
    }

    public void setBasePartSize(long basePartSize) {
        this.basePartSize = basePartSize;
    }

    public long getMinPartSize() {
        return minPartSize;
    }

    public void setMinPartSize(long minPartSize) {
        this.minPartSize = minPartSize;
    }

    public int getPartCountLimit() {
        return partCountLimit;
    }

    public void setPartCountLimit(int partCountLimit) {
        this.partCountLimit = partCountLimit;
    }

    public CalculateOptimalPartSizeParam() {
    }

    public CalculateOptimalPartSizeParam(long fileSize, String fileType, OssProviderAdapter adapter) {
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.adapter = adapter;
    }

    public CalculateOptimalPartSizeParam(long fileSize, String fileType, OssProviderAdapter adapter, long basePartSize, long minPartSize) {
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.adapter = adapter;
        this.basePartSize = basePartSize;
        this.minPartSize = minPartSize;
    }

    /**
     * 验证计算分片参数
     */
    public void validateCalculateOptimalPart() {
//        if (StrUtil.isBlank(this.fileType)) {
//            throw new IllegalArgumentException("文件类型[fileType]不能为空");
//        }
        if (this.fileSize <= 0) {
            throw new IllegalArgumentException("文件大小[fileSize]不能小于等于0");
        }
        if (Objects.isNull(this.adapter)) {
            throw new IllegalArgumentException("OSS适配器[adapter]不能为空");
        }
    }

    @Override
    public String toString() {
        return "CalculateOptimalPartSizeParam{" +
                "fileSize=" + fileSize +
                ", fileType='" + fileType + '\'' +
                ", adapter=" + adapter +
                ", partCountLimit=" + partCountLimit +
                ", basePartSize=" + basePartSize +
                ", minPartSize=" + minPartSize +
                '}';
    }
}
