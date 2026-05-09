package com.silky.starter.redis.stream.core;

/**
 * 消息监听器接口（函数式接口）
 *
 * @author zy
 * @date 2026-05-09
 */
@FunctionalInterface
public interface MessageListener {

    /**
     * 处理消息
     *
     * @param message 消息体
     * @return 处理结果（可 null，会传递给 {@link MessageRepository#onSuccess}）
     * @throws Exception 处理失败时抛出，框架将触发重试或死信流程
     */
    Object handle(StreamMessage message) throws Exception;
}
