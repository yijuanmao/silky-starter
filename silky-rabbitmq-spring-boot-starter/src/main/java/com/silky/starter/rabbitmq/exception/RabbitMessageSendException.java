package com.silky.starter.rabbitmq.exception;

import com.silky.starter.rabbitmq.core.SendResult;

/**
 * RabbitMq operation exception
 *
 * @author zy
 * @date 2025-10-13 15:50
 **/
public class RabbitMessageSendException extends RuntimeException {

    private static final long serialVersionUID = 2605123336167132402L;

    private SendResult sendResult;

    /**
     * msg
     */
    private String message;

    @Override
    public String getMessage() {
        return this.message;
    }

    public RabbitMessageSendException() {
    }

    public RabbitMessageSendException(String message) {
        super(message);
        this.message = message;
    }

    public RabbitMessageSendException(String message, SendResult sendResult) {
        super(message);
        this.sendResult = sendResult;
    }

    public RabbitMessageSendException(String message, SendResult sendResult, Throwable cause) {
        super(message, cause);
        this.sendResult = sendResult;
    }

}
