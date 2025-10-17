package com.silky.starter.rabbitmq.test.service;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import com.silky.starter.rabbitmq.enums.MessageStatus;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.persistence.entity.RabbitmqMessageRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
    public boolean saveMessageBeforeSend(BaseMassageSend message, String exchange, String routingKey, String sendMode, String businessType, String description) {
        LocalDateTime now = LocalDateTime.now();
        RabbitmqMessageRecord record = RabbitmqMessageRecord.builder()
                //主键id，根据自己业务生成
                .id(IdUtil.getWorkerId(0L, 30L))
                .createTime(now)
                .updateTime(now)
                .messageId(message.getMessageId())
                .exchange(exchange)
                .routingKey(routingKey)
                .messageBody(JSONObject.toJSONString(message))
                .msgStatus(MessageStatus.PENDING.name())
                .businessType(businessType)
                .description(description)
                .retryCount(0)
                .sendMode(sendMode)
                .costTime(0L)
                .build();

        //调用自己的持久化方法保存到数据库
        return false;
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
    public boolean updateMessageAfterSend(String messageId, MessageStatus status, Long costTime, String exception) {

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
     * 根据消息ID查询消息记录
     *
     * @param messageId 消息ID
     */
    @Override
    public RabbitmqMessageRecord findMessageById(String messageId) {

        //操作数据库
        return null;
    }

    /**
     * 查询失败的消息记录
     *
     * @param limit 查询条数
     */
    @Override
    public List<RabbitmqMessageRecord> findFailedMessages(int limit) {

        //操作数据库
        return null;
    }

    /**
     * 重试发送失败的消息
     *
     * @param recordId 消息记录ID
     */
    @Override
    public boolean retryFailedMessage(Long recordId) {

        //操作数据库
        return false;
    }
}
