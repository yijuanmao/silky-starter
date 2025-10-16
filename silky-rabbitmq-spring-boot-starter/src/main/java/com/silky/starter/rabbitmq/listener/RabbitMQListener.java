//package com.silky.starter.rabbitmq.listener;
//
//import com.rabbitmq.client.Channel;
//import com.silky.starter.rabbitmq.core.BaseMassageSend;
//import org.springframework.amqp.core.Message;
//
///**
// * RabbitMQ 消息监听器接口
// *
// * @param <T> 消息类型，必须继承 BaseMassageSend
// * @author zy
// * @date 2025-10-16 10:39
// **/
//public interface RabbitMQListener<T extends BaseMassageSend> {
//
//    /**
//     * 处理消息
//     *
//     * @param message     消息对象
//     * @param channel     RabbitMQ通道
//     * @param amqpMessage 原始AMQP消息
//     */
//    void onMessage(T message, Channel channel, Message amqpMessage);
//
//    /**
//     * 获取消息类型
//     *
//     * @return 消息类类型
//     */
//    Class<T> getMessageType();
//
//    /**
//     * 获取监听的队列名称
//     *
//     * @return 队列名称
//     */
//    String getQueueName();
//
//    /**
//     * 是否自动确认消息
//     *
//     * @return true: 自动确认, false: 手动确认
//     */
//    default boolean autoAck() {
//        return false;
//    }
//
//    /**
//     * 消费失败时的重试次数
//     *
//     * @return 重试次数，默认3次
//     */
//    default int retryTimes() {
//        return 3;
//    }
//
//    /**
//     * 消费失败时的重试间隔（毫秒）
//     *
//     * @return 重试间隔，默认1000ms
//     */
//    default long retryInterval() {
//        return 1000L;
//    }
//
//    /**
//     * 是否启用死信队列
//     *
//     * @return true: 启用, false: 不启用
//     */
//    default boolean enableDlx() {
//        return false;
//    }
//}
