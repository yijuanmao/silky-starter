package com.silky.starter.rabbitmq.persistence;

import com.silky.starter.rabbitmq.core.BaseMassageSend;
import com.silky.starter.rabbitmq.enums.MessageStatus;
import com.silky.starter.rabbitmq.persistence.entity.RabbitmqMessageRecord;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于JPA的消息持久化服务实现
 *
 * @author zy
 * @date 2025-10-15 14:18
 **/
@Service
public class NoOpMessagePersistenceService implements MessagePersistenceService {

    /**
     * 保存发送前的消息记录
     *
     * @param message      消息对象
     * @param exchange     交换机
     * @param routingKey   路由键
     * @param sendMode     发送模式
     * @param businessType 业务类型
     * @param description  描述
     */
    @Override
    public void saveMessageBeforeSend(BaseMassageSend message, String exchange, String routingKey, String sendMode, String businessType, String description) {

    }

    /**
     * 更新消息发送结果
     *
     * @param messageId 消息ID
     * @param status    消息状态
     * @param costTime  消息发送耗时
     * @param exception 异常信息
     */
    @Override
    public void updateMessageAfterSend(String messageId, MessageStatus status, Long costTime, String exception) {

    }

    /**
     * 记录消息消费
     *
     * @param messageId 消息ID
     * @param costTime  消息消费耗时
     */
    @Override
    public void recordMessageConsume(String messageId, Long costTime) {

    }

    /**
     * 记录消息消费失败
     *
     * @param messageId 消息ID
     * @param exception 异常信息
     * @param costTime  消息消费耗时
     */
    @Override
    public void recordMessageConsumeFailure(String messageId, String exception, Long costTime) {

    }

    /**
     * 根据消息ID查询消息记录
     *
     * @param messageId 消息ID
     */
    @Override
    public RabbitmqMessageRecord findMessageById(String messageId) {
        return null;
    }

    /**
     * 查询失败的消息记录
     *
     * @param limit 查询条数
     */
    @Override
    public List<RabbitmqMessageRecord> findFailedMessages(int limit) {
        return null;
    }

    /**
     * 重试发送失败的消息
     *
     * @param recordId 消息记录ID
     */
    @Override
    public boolean retryFailedMessage(Long recordId) {
        return false;
    }
}
