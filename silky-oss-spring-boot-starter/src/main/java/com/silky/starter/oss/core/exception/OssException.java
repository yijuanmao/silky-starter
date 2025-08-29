package com.silky.starter.oss.core.exception;

/**
 * OSS异常类
 *
 * @author zy
 * @date 2025-08-08 15:50
 **/
public class OssException extends RuntimeException {

    private static final long serialVersionUID = -6581915792411430588L;

    /**
     * msg
     */
    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    public OssException() {
    }

    public OssException(String message) {
        super(message);
        this.message = message;
    }

    public OssException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}
