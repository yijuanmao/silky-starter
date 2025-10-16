package com.silky.starter.rabbitmq.persistence.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息记录实体类
 *
 * @author zy
 * @date 2025-10-13 17:59
 **/
@Data
@ToString
@Builder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class RabbitmqMessageRecord implements Serializable {

    private static final long serialVersionUID = 2079264408509650447L;

    /**
     * 消息主键id
     */
    @JSONField(format = "STRING")
    private Long id;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 消息Id
     */
    private String messageId;

    /**
     * 交换机名称
     */
    private String exchange;

    /**
     * 路由键
     */
    private String routingKey;

    /**
     * 消息体
     */
    private String messageBody;

    /**
     * 消息状态，参照枚举类:{@link com.silky.starter.rabbitmq.enums.MessageStatus}
     */
    private String msgStatus;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 描述
     */
    private String description;

    /**
     * 异常信息
     */
    private String exceptionMsg;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 发送模式，参照枚举类:{@link com.silky.starter.rabbitmq.enums.SendMode}
     */
    private String sendMode;

    /**
     * 耗时(毫秒)
     */
    private Long costTime;
}
