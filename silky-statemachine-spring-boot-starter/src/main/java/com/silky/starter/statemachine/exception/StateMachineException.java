package com.silky.starter.statemachine.exception;

/**
 * 状态机异常类
 *
 * @author zy
 * @date 2025-08-08 15:50
 **/
public class StateMachineException extends RuntimeException {

    private static final long serialVersionUID = -7577581297517093638L;

    /**
     * msg
     */
    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    public StateMachineException() {
    }

    public StateMachineException(String message) {
        super(message);
        this.message = message;
    }

    public StateMachineException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }
}
