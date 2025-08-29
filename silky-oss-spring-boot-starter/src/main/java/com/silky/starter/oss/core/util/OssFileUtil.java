package com.silky.starter.oss.core.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

/**
 * OSS文件工具类
 *
 * @author zy
 * @date 2025-08-22 17:32
 **/
public class OssFileUtil {

    private OssFileUtil() {
    }

    /**
     * 构建对象键
     *
     * @param path     路径
     * @param fileName 文件名
     * @return 对象键
     */
    public static String buildObjectKey(String path, String fileName) {
        StringBuilder objectKey = new StringBuilder();
        if (StrUtil.isNotBlank(path)) {
            if (!path.endsWith("/")) {
                path += "/";
            }
            objectKey.append(path);
        }
        if (StrUtil.isBlank(fileName)) {
            objectKey.append(IdUtil.fastSimpleUUID());
        } else {
            objectKey.append(fileName);
        }
        return sanitizeOssPath(objectKey.toString());
    }

    /**
     * 标准化OSS路径，防止路径穿越和非法字符
     *
     * @param path oss路径
     * @return String
     */
    public static String sanitizeOssPath(String path) {
        if (path == null) return "";
        // 替换非法字符，如../防止路径穿越
        return path.replaceAll("\\.\\./", "_").replaceAll("\\p{Cntrl}", "_");
    }
}
