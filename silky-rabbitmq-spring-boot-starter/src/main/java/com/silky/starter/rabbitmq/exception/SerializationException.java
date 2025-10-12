package com.silky.starter.rabbitmq.exception;

/**
 * RabbitMq 序列化 exception
 *
 * @author zy
 * @date 2025-10-13 15:50
 **/
public class SerializationException extends RuntimeException {

    private static final long serialVersionUID = -3976970610682491973L;

    /**
     * msg
     */
    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    public SerializationException() {
    }

    public SerializationException(String message) {
        super(message);
        this.message = message;
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}
