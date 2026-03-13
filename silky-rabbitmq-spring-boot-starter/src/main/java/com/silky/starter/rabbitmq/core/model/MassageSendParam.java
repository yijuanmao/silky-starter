package com.silky.starter.rabbitmq.core.model;

import com.silky.starter.rabbitmq.enums.SendMode;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 发送消息基础对象
 *
 * @author zy
 * @date 2025-10-09 17:54
 **/
@Data
@ToString
@Builder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class MassageSendParam implements Serializable {

    private static final long serialVersionUID = -1686909309758962407L;

    /**
     * 消息内容
     */
    private Object msg;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 交换机
     */
    private String exchange;

    /**
     * 路由键
     */
    private String routingKey;

    /**
     * 是否延时消息
     */
    private boolean sendDelay;

    /**
     * 延时消息时间戳
     */
    private Long delayMillis;

    /**
     * 发送模式
     */
    private SendMode sendMode;

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
    private Map<String, Object> extData;

    public MassageSendParam(Object msg) {
        this.msg = msg;
    }
}
