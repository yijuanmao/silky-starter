package com.silky.starter.redis.stream.annotation;

import java.lang.annotation.*;

/**
 * Redis Stream 消息监听器注解
 * <p>
 * 标注在方法上，使该方法自动注册为指定 Stream 的消费者。
 * 方法签名支持以下形式：
 * </p>
 *
 * <pre>
 * // 接收 StreamMessage（推荐，可获取完整消息上下文）
 * {@literal @}RedisStreamListener(stream = "order-stream", group = "order-group")
 * public void processOrder(StreamMessage message) {
 *     Order order = message.getPayload(Order.class);
 *     // 处理业务...
 *     // 若方法正常返回 → 框架自动 ACK
 *     // 若方法抛出异常 → 框架触发重试或死信流程
 * }
 * </pre>
 *
 * @author zy
 * @date 2026-05-09
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisStreamListener {

    /**
     * Stream Key（队列名称），必填
     */
    String stream();

    /**
     * 消费者组名，默认使用 {@code silky.redis.stream.default-group} 全局配置
     */
    String group() default "";

    /**
     * 消费者名称前缀，默认使用 hostname，并发时会自动追加序号后缀
     * 如：order-consumer-0、order-consumer-1
     */
    String consumer() default "";

    /**
     * 并发消费者数量，默认 1（多消费者共享同一个 Consumer Group，Redis 自动分配消息）
     */
    int concurrency() default 1;

    /**
     * 每次拉取的最大消息数，0 表示使用全局配置 {@code silky.redis.stream.consumer.batch-size}
     */
    int batchSize() default 0;

    /**
     * 是否自动创建消费者组（不存在时），默认 true
     */
    boolean autoCreateGroup() default true;
}
