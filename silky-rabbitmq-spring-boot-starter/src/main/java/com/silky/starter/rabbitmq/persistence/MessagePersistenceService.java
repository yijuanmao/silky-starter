package com.silky.starter.rabbitmq.persistence;

import com.silky.starter.rabbitmq.core.BaseMassageSend;
import com.silky.starter.rabbitmq.enums.MessageStatus;
import com.silky.starter.rabbitmq.persistence.entity.RabbitmqMessageRecord;

import java.util.List;

/**
 * 消息持久化接口
 *
 * @author zy
 * @date 2025-10-13 17:50
 **/
public interface MessagePersistenceService {

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
    boolean saveMessageBeforeSend(BaseMassageSend message, String exchange, String routingKey,
                                  String sendMode, String businessType, String description);

    /**
     * 更新消息发送结果
     *
     * @param messageId 消息ID
     * @param status    消息状态
     * @param costTime  消息发送耗时
     * @param exception 异常信息
     * @return 是否保存成功
     */
    boolean updateMessageAfterSend(String messageId, MessageStatus status, Long costTime, String exception);

    /**
     * 记录消息消费
     *
     * @param messageId 消息ID
     * @param costTime  消息消费耗时
     * @return 是否保存成功
     */
    boolean recordMessageConsume(String messageId, Long costTime);

    /**
     * 记录消息消费失败
     *
     * @param messageId 消息ID
     * @param exception 异常信息
     * @param costTime  消息消费耗时
     * @return 是否保存成功
     */
    boolean recordMessageConsumeFailure(String messageId, String exception, Long costTime);

    /**
     * 根据消息ID查询消息记录
     *
     * @param messageId 消息ID
     */
    RabbitmqMessageRecord findMessageById(String messageId);

    /**
     * 查询失败的消息记录
     *
     * @param limit 查询条数
     */
    List<RabbitmqMessageRecord> findFailedMessages(int limit);

    /**
     * 重试发送失败的消息
     *
     * @param recordId 消息记录ID
     */
    boolean retryFailedMessage(Long recordId);

    /**
     * 是否启用持久化
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 获取持久化类型
     */
    default String getPersistenceType() {
        return "CUSTOM";
    }

    /**
     * 初始化持久化服务
     */
    default void initialize() {
        // 默认空实现，子类可以重写
    }

    /**
     * 销毁持久化服务
     */
    default void destroy() {
        // 默认空实现，子类可以重写
    }
}
