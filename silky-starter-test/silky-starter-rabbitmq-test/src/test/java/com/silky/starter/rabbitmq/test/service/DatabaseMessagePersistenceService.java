package com.silky.starter.rabbitmq.test.service;

import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import com.silky.starter.rabbitmq.enums.SendStatus;
import com.silky.starter.rabbitmq.enums.SendMode;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 基于数据库的消息持久化服务实现
 *
 * @author zy
 * @date 2025-10-16 18:03
 **/
@Service
public class DatabaseMessagePersistenceService implements MessagePersistenceService {

    /**
     * 保存发送前的消息记录
     *
     * @param message      消息对象
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param sendMode     发送模式
     * @param businessType 业务类型
     * @param description  描述
     * @return 是否保存成功
     */
    @Override
    public boolean saveMessageBeforeSend(BaseMassageSend message, String exchange, String routingKey, SendMode sendMode, String businessType, String description) {
        LocalDateTime now = LocalDateTime.now();

        //这里比如是数据库持久化操作
//        RabbitmqMessageRecord record = RabbitmqMessageRecord.builder()
//                //主键id，根据自己业务生成
//                .id(IdUtil.getWorkerId(0L, 30L))
//                .createTime(now)
//                .updateTime(now)
//                .messageId(message.getMessageId())
//                .exchange(exchange)
//                .routingKey(routingKey)
//                .messageBody(JSONObject.toJSONString(message))
//                .msgStatus(MessageStatus.PENDING)
//                .businessType(businessType)
//                .description(description)
//                .retryCount(0)
//                .sendMode(sendMode)
//                .costTime(0L)
//                .build();

        //调用自己的持久化方法保存到数据库
        return true;
    }

    /**
     * 更新消息发送结果
     *
     * @param messageId 消息ID
     * @param status    消息状态
     * @param costTime  消息发送耗时
     * @param exception 异常信息
     * @return 是否保存成功
     */
    @Override
    public boolean updateMessageAfterSend(String messageId, SendStatus status, Long costTime, String exception) {

        //调用自己的持久化方法保存到数据库
        return false;
    }

    /**
     * 记录消息消费
     *
     * @param messageId 消息ID
     * @param costTime  消息消费耗时
     * @return 是否保存成功
     */
    @Override
    public boolean recordMessageConsume(String messageId, Long costTime) {

        //操作数据库
        return false;
    }

    /**
     * 记录消息消费失败
     *
     * @param messageId 消息ID
     * @param exception 异常信息
     * @param costTime  消息消费耗时
     * @return 是否保存成功
     */
    @Override
    public boolean recordMessageConsumeFailure(String messageId, String exception, Long costTime) {

        //操作数据库
        return false;
    }

    /**
     * 重试发送失败的消息
     *
     * @param messageId 消息ID
     */
    @Override
    public boolean retryFailedMessage(String messageId) {

        //操作数据库
        return false;
    }
}
