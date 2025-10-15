package com.silky.starter.rabbitmq.persistence.impl;

import com.silky.starter.rabbitmq.core.BaseMassageSend;
import com.silky.starter.rabbitmq.enums.MessageStatus;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import com.silky.starter.rabbitmq.persistence.entity.RabbitmqMessageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于JPA的消息持久化服务实现
 *
 * @author zy
 * @date 2025-10-15 14:18
 **/
public class NoOpMessagePersistenceService implements MessagePersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(NoOpMessagePersistenceService.class);

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
    public boolean saveMessageBeforeSend(BaseMassageSend message, String exchange, String routingKey, String sendMode, String businessType, String description) {
        logger.debug("NoOp persistence: Save message before send - messageId: {}, exchange: {}, routingKey: {}",
                message.getMessageId(), exchange, routingKey);
        return true;
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
    public boolean updateMessageAfterSend(String messageId, MessageStatus status, Long costTime, String exception) {
        logger.debug("NoOp persistence: Update message after send - messageId: {}, status: {}",
                messageId, status);
        return true;
    }

    /**
     * 记录消息消费
     *
     * @param messageId 消息ID
     * @param costTime  消息消费耗时
     */
    @Override
    public boolean recordMessageConsume(String messageId, Long costTime) {
        logger.debug("NoOp persistence: Record message consume - messageId: {}", messageId);
        return true;
    }

    /**
     * 记录消息消费失败
     *
     * @param messageId 消息ID
     * @param exception 异常信息
     * @param costTime  消息消费耗时
     */
    @Override
    public boolean recordMessageConsumeFailure(String messageId, String exception, Long costTime) {
        logger.debug("NoOp persistence: Record message consume failure - messageId: {}, exception: {}",
                messageId, exception);
        return true;
    }

    /**
     * 根据消息ID查询消息记录
     *
     * @param messageId 消息ID
     */
    @Override
    public RabbitmqMessageRecord findMessageById(String messageId) {
        logger.debug("NoOp persistence: Find message by id - messageId: {}", messageId);
        return null;
    }

    /**
     * 查询失败的消息记录
     *
     * @param limit 查询条数
     */
    @Override
    public List<RabbitmqMessageRecord> findFailedMessages(int limit) {
        logger.debug("NoOp persistence: Find failed messages - limit: {}", limit);
        return null;
    }

    /**
     * 重试发送失败的消息
     *
     * @param recordId 消息记录ID
     */
    @Override
    public boolean retryFailedMessage(Long recordId) {
        logger.debug("NoOp persistence: Retry failed message - recordId: {}", recordId);
        return false;
    }
}
