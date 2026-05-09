package com.silky.starter.redis.stream.core;

/**
 * 空实现的消息仓库（默认注入，不做任何持久化）
 * <p>
 * 当用户未提供 {@link MessageRepository} 实现时，框架自动注入此空实现，
 * 确保功能可用且不会因缺少 Bean 而报错。
 * </p>
 *
 * @author zy
 * @date 2026-05-09
 */
public class NoOpMessageRepository implements MessageRepository {
    // 所有方法均继承 MessageRepository 的 default 空实现
}
