package com.silky.starter.redis.lock.exception;

/**
 * redisson异常类
 *
 * @author zy
 * @date 2025-10-13 15:50
 **/
public class RedissonException extends RuntimeException {

    private static final long serialVersionUID = -6526704027102784334L;

    /**
     * msg
     */
    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    public RedissonException() {
    }

    public RedissonException(String message) {
        super(message);
        this.message = message;
    }

    public RedissonException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}
