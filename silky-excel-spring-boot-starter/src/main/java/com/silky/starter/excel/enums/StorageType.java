package com.silky.starter.excel.enums;

import lombok.Getter;

/**
 * 存储类型
 *
 * @author zy
 * @date 2025-10-24 11:30
 **/
@Getter
public enum StorageType {

    /**
     * 本地文件系统存储
     */
    LOCAL("本地存储"),

    /**
     * Redis存储（适合小文件）
     */
    REDIS("Redis存储"),

    /**
     * MongoDB GridFS存储
     */
    MONGO("MongoDB存储"),

    /**
     * 对象存储服务（需要用户自定义实现）
     */
    OSS("对象存储");


    private final String description;

    StorageType(String description) {
        this.description = description;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 枚举编码
     * @return 对应的枚举值，如果找不到返回null
     */
    public static StorageType getByCode(String code) {
        for (StorageType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
