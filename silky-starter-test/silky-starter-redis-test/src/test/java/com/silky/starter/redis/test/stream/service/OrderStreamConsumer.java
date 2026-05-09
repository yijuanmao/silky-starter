package com.silky.starter.redis.test.stream.service;

import com.silky.starter.redis.stream.annotation.RedisStreamListener;
import com.silky.starter.redis.stream.core.StreamMessage;
import com.silky.starter.redis.test.stream.entity.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单 Stream 消费者（注解驱动示例）
 * <p>
 * 展示如何通过 {@link RedisStreamListener} 注解监听 Redis Stream 消息。
 * </p>
 *
 * @author zy
 * @date 2026-05-09
 */
@Component
public class OrderStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStreamConsumer.class);

    /** 已处理的消息列表（线程安全，测试验收用） */
    private final CopyOnWriteArrayList<OrderMessage> processedOrders = new CopyOnWriteArrayList<>();

    /** 处理失败计数 */
    private final AtomicInteger failCount = new AtomicInteger(0);

    /** 测试用 CountDownLatch（外部注入，用于等待消息消费完成） */
    private volatile CountDownLatch latch;

    /**
     * 期望的 orderNo 前缀集合（仅当消息的 orderNo 以某个前缀开头时才 countdown latch）
     * 为 null 或空时表示任意消息都会触发 countdown
     */
    private volatile Set<String> expectedOrderNoPrefixes;

    /**
     * 正常订单消费（主 Stream）
     */
    @RedisStreamListener(stream = "silky:test:stream:order", group = "order-group", concurrency = 1)
    public void onOrderMessage(StreamMessage message) {
        OrderMessage order = message.getPayload(OrderMessage.class);
        log.info("[OrderStreamConsumer] 收到订单消息: id={}, orderNo={}, status={}",
                message.getId(), order.getOrderNo(), order.getStatus());

        processedOrders.add(order);
        tryCountDown(order.getOrderNo());
    }

    /**
     * 高并发订单消费（并发数 = 2）
     */
    @RedisStreamListener(stream = "silky:test:stream:order:concurrent", group = "order-concurrent-group", concurrency = 2)
    public void onConcurrentOrderMessage(StreamMessage message) {
        OrderMessage order = message.getPayload(OrderMessage.class);
        String traceId = message.getHeader("traceId");
        log.info("[OrderStreamConsumer][并发] 收到订单消息: id={}, orderNo={}, traceId={}",
                message.getId(), order.getOrderNo(), traceId);

        processedOrders.add(order);
        tryCountDown(order.getOrderNo());
    }

    /**
     * 模拟消费失败（用于测试重试和死信队列）
     */
    @RedisStreamListener(stream = "silky:test:stream:order:fail", group = "order-fail-group", concurrency = 1)
    public void onFailOrderMessage(StreamMessage message) {
        failCount.incrementAndGet();
        log.warn("[OrderStreamConsumer][失败测试] 模拟处理失败: id={}, failCount={}",
                message.getId(), failCount.get());
        throw new RuntimeException("模拟业务处理失败（测试死信队列）");
    }

    /**
     * 尝试 countdown latch（根据 expectedOrderNoPrefixes 过滤）
     */
    private void tryCountDown(String orderNo) {
        if (latch == null) {
            return;
        }
        if (expectedOrderNoPrefixes == null || expectedOrderNoPrefixes.isEmpty()) {
            latch.countDown();
            return;
        }
        for (String prefix : expectedOrderNoPrefixes) {
            if (orderNo != null && orderNo.startsWith(prefix)) {
                latch.countDown();
                return;
            }
        }
    }

    public CopyOnWriteArrayList<OrderMessage> getProcessedOrders() {
        return processedOrders;
    }

    public int getFailCount() {
        return failCount.get();
    }

    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    public void setExpectedOrderNoPrefixes(Set<String> expectedOrderNoPrefixes) {
        this.expectedOrderNoPrefixes = expectedOrderNoPrefixes;
    }

    /**
     * 设置单个期望的 orderNo（精确匹配）
     */
    public void setExpectedOrderNo(String expectedOrderNo) {
        this.expectedOrderNoPrefixes = expectedOrderNo != null
                ? java.util.Collections.singleton(expectedOrderNo) : null;
    }

    public void clearProcessed() {
        processedOrders.clear();
    }

    /**
     * 重置所有测试状态（latch、expectedOrderNoPrefixes、processedOrders）
     */
    public void resetAll() {
        this.latch = null;
        this.expectedOrderNoPrefixes = null;
        processedOrders.clear();
    }
}
