package com.silky.starter.rabbitmq.persistence.impl;

import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import com.silky.starter.rabbitmq.enums.SendStatus;
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
    public void saveMessageBeforeSend(BaseMassageSend message, String exchange, String routingKey, SendMode sendMode, String businessType, String description) {

        logger.debug("NoOpMessagePersistenceService: saveMessageBeforeSend called with messageId={}, exchange={}, routingKey={}, sendMode={}, businessType={}, description={}",
                message.getMessageId(), exchange, routingKey, sendMode, businessType, description);
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
    public void updateMessageAfterSend(String messageId, SendStatus status, Long costTime, String exception) {

        logger.debug("NoOpMessagePersistenceService: updateMessageAfterSend called with messageId={}, status={}, costTime={}, exception={}",
                messageId, status, costTime, exception);
    }

    /**
     * 消息消费成功
     *
     * @param messageId 消息ID
     * @param costTime  消息消费耗时
     */
    @Override
    public void consumeSuccess(String messageId, Long costTime) {

        logger.debug("NoOpMessagePersistenceService: consumeSuccess called with messageId={}, costTime={}",
                messageId, costTime);
    }

    /**
     * 消息消费失败
     *
     * @param messageId 消息ID
     * @param exception 异常信息
     * @param costTime  消息消费耗时
     */
    @Override
    public void consumeFailure(String messageId, String exception, Long costTime) {

        logger.debug("NoOpMessagePersistenceService: consumeFailure called with messageId={}, exception={}, costTime={}",
                messageId, exception, costTime);
    }
}
