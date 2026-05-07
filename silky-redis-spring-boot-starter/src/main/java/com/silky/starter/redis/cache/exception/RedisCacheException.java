package com.silky.starter.redis.cache.exception;

/**
 * redis缓存异常类
 *
 * @author zy
 * @date 2025-10-13 15:50
 **/
public class RedisCacheException extends RuntimeException {

    private static final long serialVersionUID = -4191728267081509646L;

    public RedisCacheException() {
    }

    public RedisCacheException(String message) {
        super(message);
    }

    public RedisCacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
