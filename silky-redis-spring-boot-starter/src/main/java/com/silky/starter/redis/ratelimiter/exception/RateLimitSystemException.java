package com.silky.starter.redis.ratelimiter.exception;

/**
 * 限流系统异常（如 Redis/Lua 执行失败、配置非法等），
 * 与正常的限流拒绝场景区分开。
 *
 * @author zy
 * @date 2026-05-13
 */
public class RateLimitSystemException extends RuntimeException {

    private static final long serialVersionUID = 8731736523796874949L;

    public RateLimitSystemException(String message) {
        super(message);
    }

    public RateLimitSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}

