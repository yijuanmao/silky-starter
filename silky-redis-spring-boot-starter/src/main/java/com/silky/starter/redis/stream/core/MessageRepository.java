package com.silky.starter.redis.stream.core;

/**
 * 消息持久化仓库接口（用户可选实现）
 *
 * @author zy
 * @date 2026-05-09
 */
public interface MessageRepository {

    /**
     * 生产者发送消息前回调
     * <p>可在此做入库记录，返回值若非空，会以 "dbId" 键存入消息 headers。</p>
     *
     * @param message 待发送的消息体
     * @return 业务 ID（可 null），框架会将非空值存入 {@code message.headers["dbId"]}
     */
    default String onSend(StreamMessage message) {
        return null;
    }

    /**
     * 消费者接收到消息、业务处理前回调
     *
     * @param message      接收到的消息体
     * @param consumerName 当前消费者名称
     */
    default void onReceive(StreamMessage message, String consumerName) {
    }

    /**
     * 业务处理成功且 ACK 之后回调
     *
     * @param message 处理成功的消息体
     * @param result  {@link MessageListener#handle(StreamMessage)} 的返回值
     */
    default void onSuccess(StreamMessage message, Object result) {
    }

    /**
     * 业务处理抛出异常后回调
     *
     * @param message    处理失败的消息体
     * @param exception  业务异常
     * @param retryCount 当前已重试次数（首次失败时为 1）
     * @return {@code true} 继续重试，{@code false} 放入死信队列
     */
    default boolean onFailure(StreamMessage message, Exception exception, int retryCount) {
        return true;
    }

    /**
     * 消息进入死信队列时回调（超过最大重试次数，或 {@link #onFailure} 返回 false）
     *
     * @param message 死信消息体
     * @param reason  死信原因
     */
    default void onDeadLetter(StreamMessage message, String reason) {
    }
}
