package com.silky.starter.rabbitmq.persistence.impl;

import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import com.silky.starter.rabbitmq.enums.MessageStatus;
import com.silky.starter.rabbitmq.enums.SendMode;
import com.silky.starter.rabbitmq.persistence.MessagePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public boolean saveMessageBeforeSend(BaseMassageSend message, String exchange, String routingKey, SendMode sendMode, String businessType, String description) {
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
     * 重试发送失败的消息
     *
     * @param messageId 消息ID
     */
    @Override
    public boolean retryFailedMessage(String messageId) {
        logger.debug("NoOp persistence: Retry failed message - messageId: {}", messageId);
        return false;
    }
}
