package com.silky.starter.rabbitmq.core;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发送结果
 *
 * @author zy
 * @date 2025-10-09 17:49
 **/
@Data
@ToString
public class SendResult implements Serializable {

    private static final long serialVersionUID = -8253067439843824965L;

    /**
     * 是否发送成功
     */
    private boolean success;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 消费时间
     */
    private long costTime;

    /**
     * 相关数据
     */
    private Object correlationData;

    public static SendResult success(String messageId, long costTime) {
        SendResult result = new SendResult();
        result.setSuccess(true);
        result.setMessageId(messageId);
        result.setSendTime(LocalDateTime.now());
        result.setCostTime(costTime);
        return result;
    }

    public static SendResult failure(String errorMessage, long costTime) {
        SendResult result = new SendResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setSendTime(LocalDateTime.now());
        result.setCostTime(costTime);
        return result;
    }
}
