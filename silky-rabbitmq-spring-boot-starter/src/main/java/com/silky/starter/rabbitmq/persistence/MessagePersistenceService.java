package com.silky.starter.rabbitmq.persistence;

import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import com.silky.starter.rabbitmq.enums.SendMode;
import com.silky.starter.rabbitmq.enums.SendStatus;

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
                                  SendMode sendMode, String businessType, String description);

    /**
     * 更新消息发送结果
     *
     * @param messageId 消息ID
     * @param status    消息状态
     * @param costTime  消息发送耗时
     * @param exception 异常信息
     * @return 是否保存成功
     */
    boolean updateMessageAfterSend(String messageId, SendStatus status, Long costTime, String exception);

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
     * 记录死信队列发送成功
     *
     * @param messageId     消息ID
     * @param queueName     原始队列名称
     * @param dlxExchange   死信交换机
     * @param dlxRoutingKey 死信路由键
     */
    default void recordDLQSendSuccess(String messageId, String queueName, String dlxExchange, String dlxRoutingKey) {
        // 默认实现，可以根据需要重写
    }

    /**
     * 记录死信队列发送失败
     *
     * @param messageId    消息ID
     * @param queueName    原始队列名称
     * @param errorMessage 错误信息
     */
    default void recordDLQSendFailure(String messageId, String queueName, String errorMessage) {
        // 默认实现，可以根据需要重写
    }

    /**
     * 记录消息进入死信队列（带时间戳）
     *
     * @param messageId    消息ID
     * @param queueName    原始队列名称
     * @param errorMessage 错误信息
     */
    default void recordMessageSendToDLQ(String messageId, String queueName, String errorMessage) {
        // 默认实现，可以根据需要重写
    }

    /**
     * 重试发送失败的消息
     *
     * @param messageId 消息ID
     */
    boolean retryFailedMessage(String messageId);

    /**
     * 是否启用持久化
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 获取持久化类型
     */
//    default String getPersistenceType() {
//        return "CUSTOM";
//    }

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
