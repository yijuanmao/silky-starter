package com.silky.starter.mongodb.core.exception;

/**
 * MongoDB operation exception
 *
 * @author zy
 * @date 2025-08-08 15:50
 **/
public class MongoOperationException extends RuntimeException {

    private static final long serialVersionUID = -5697446417761848139L;

    /**
     * msg
     */
    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    public MongoOperationException() {
    }

    public MongoOperationException(String message) {
        super(message);
        this.message = message;
    }

    public MongoOperationException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}
