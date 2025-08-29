package com.silky.starter.oss.core.enums;

/**
 * 文件类型枚举类
 *
 * @author zy
 * @date 2025-04-24 10:08
 **/
public enum OssFileTypeEnum {

    BMP(".bmp", "image/bmp"),
    GIF(".gif", "image/gif"),
    JPEG(".jpeg", "image/jpeg"),
    JPG(".jpg", "image/jpeg"),
    PNG(".png", "image/png"),
    HTML(".html", "text/html"),
    XML(".xml", "text/xml"),
    TXT(".txt", "application/octet-stream"),
    SQL(".sql", "application/octet-stream"),
    VSD(".vsd", "application/vnd.visio"),
    PDF(".pdf", "application/pdf"),
    PPT(".ppt", "application/vnd.ms-powerpoint"),
    PPTX(".pptx", "application/vnd.ms-powerpoint"),
    DOC(".doc", "application/msword"),
    DOCX(".docx", "application/msword"),
    XLS(".xls", "application/vnd.ms-excel"),
    XLSX(".xlsx", "application/vnd.ms-excel"),
    CSV(".csv", "application/vnd.ms-excel");


    /**
     * 枚举值
     */
    private final String value;

    /**
     * 文件的contentType类型
     */
    private final String contentType;

    public String getValue() {
        return value;
    }

    public String getContentType() {
        return contentType;
    }

    OssFileTypeEnum(String value, String contentType) {
        this.value = value;
        this.contentType = contentType;
    }

    /**
     * 通过文件名判断并获取OSS服务文件上传时文件的contentType
     *
     * @param fileName 文件名
     * @return 文件的contentType
     */
    public static String getContentType(String fileName) {
        // 文件的后缀名
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        for (OssFileTypeEnum e : OssFileTypeEnum.values()) {
            if (e.getValue().equalsIgnoreCase(fileExtension)) {
                return e.getContentType();
            }
        }
        // 默认返回类型
        return OssFileTypeEnum.JPG.getValue();
    }

    @Override
    public String toString() {
        return "OssFileTypeEnum{" +
                "value='" + value + '\'' +
                ", contentType='" + contentType + '\'' +
                "} " + super.toString();
    }
}
