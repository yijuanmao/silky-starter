package com.silky.starter.redis.ratelimiter.exception;

/**
 * Redis限流器异常
 *
 * @author zy
 * @date 2025-10-13 15:50
 **/
public class RateLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = -5036320871764220831L;

    /**
     * msg
     */
    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    public RateLimitExceededException() {
    }

    public RateLimitExceededException(String message) {
        super(message);
        this.message = message;
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}
