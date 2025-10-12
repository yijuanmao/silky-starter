package com.silky.starter.rabbitmq.core;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发送消息基础对象
 *
 * @author zy
 * @date 2025-10-09 17:54
 **/
@Data
@ToString
public class BaseMassageSend implements Serializable {

    private static final long serialVersionUID = -1686909309758962407L;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 消息描述
     */
    private String description;

    /**
     * 消息来源
     */
    private String source;

    /**
     * 扩展属性
     */
    private Object extData;
}
