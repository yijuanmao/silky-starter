package com.silky.starter.excel.core.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 存储对象
 * 封装文件存储后的完整元信息
 *
 * @author zy
 * @since 1.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageObject {

    /**
     * 文件唯一标识（存储 Key）
     */
    private String key;

    /**
     * 文件访问 URL（如果可以生成）
     */
    private String url;

    /**
     * 文件大小（字节）
     */
    private long size;

    /**
     * 文件 ETag（用于校验，可选）
     */
    private String etag;

    /**
     * 过期时间（可选）
     */
    private LocalDateTime expireAt;

    /**
     * 创建便捷方法
     *
     * @param key  存储键
     * @param size 文件大小
     * @return StorageObject
     */
    public static StorageObject of(String key, long size) {
        return StorageObject.builder()
                .key(key)
                .size(size)
                .build();
    }
}
