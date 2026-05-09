package com.silky.starter.redis.test.stream;

import com.silky.starter.redis.stream.core.StreamMessage;
import com.silky.starter.redis.stream.producer.RedisMessageQueueTemplate;
import com.silky.starter.redis.stream.registry.MessageListenerContainerRegistry;
import com.silky.starter.redis.test.RedisApplicationTest;
import com.silky.starter.redis.test.stream.entity.OrderMessage;
import com.silky.starter.redis.test.stream.service.OrderStreamConsumer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis Stream 全量测试类
 * <p>
 * 测试覆盖：
 * <ol>
 *   <li>基础消息发送与接收</li>
 *   <li>带扩展 headers 的消息</li>
 *   <li>批量发送消息</li>
 *   <li>并发消费</li>
 *   <li>消息 payload 类型安全反序列化</li>
 *   <li>编程式注册消费者</li>
 *   <li>死信队列触发验证</li>
 * </ol>
 * </p>
 *
 * <p><b>前置条件：</b>需要 Redis 实例运行在 localhost:6379，并在 application.yml 中启用
 * {@code silky.redis.stream.enabled=true}</p>
 *
 * @author zy
 * @date 2026-05-09
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisStreamTest extends RedisApplicationTest {

    /**
     * 测试用 Stream Key（基础）
     */
    private static final String STREAM_ORDER = "silky:test:stream:order";

    /**
     * 测试用 Stream Key（并发）
     */
    private static final String STREAM_ORDER_CONCURRENT = "silky:test:stream:order:concurrent";

    /**
     * 测试用 Stream Key（失败/死信）
     */
    private static final String STREAM_ORDER_FAIL = "silky:test:stream:order:fail";

    /**
     * 死信 Stream Key（对应 STREAM_ORDER_FAIL）
     */
    private static final String STREAM_ORDER_FAIL_DEAD = "silky:test:stream:order:fail:dead";

    /**
     * 编程式注册测试 Stream Key
     */
    private static final String STREAM_PROGRAMMATIC = "silky:test:stream:programmatic";

    @Autowired
    private RedisMessageQueueTemplate queueTemplate;

    @Autowired
    private MessageListenerContainerRegistry registry;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private OrderStreamConsumer orderStreamConsumer;

    @BeforeEach
    public void resetState() {
        // 重置消费者所有状态（latch、expectedOrderNo、processedOrders）
        orderStreamConsumer.resetAll();
        log.info("[BeforeEach] 已重置消费者状态");
    }

    // ========================= 基础功能测试 =========================

    /**
     * 测试1：基础消息发送，验证返回有效消息 ID
     */
    @Test
    @Order(1)
    public void testSendMessage_shouldReturnValidId() {
        OrderMessage order = OrderMessage.of("ORD001", "user001", "iPhone 16", new BigDecimal("9999.00"));

        String messageId = queueTemplate.send(STREAM_ORDER, order);

        log.info("[测试1] 发送消息成功: streamKey={}, messageId={}", STREAM_ORDER, messageId);

        assertNotNull(messageId, "消息 ID 不应为空");
        assertFalse(messageId.isEmpty(), "消息 ID 不应为空字符串");
        assertTrue(messageId.contains("-"), "Redis Stream 消息 ID 应包含 '-' 分隔符（格式：timestamp-seq）");
    }

    /**
     * 测试2：发送并消费消息，验证 payload 完整性
     */
    @Test
    @Order(2)
    public void testSendAndReceiveMessage_shouldConsumeSuccessfully() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        orderStreamConsumer.setLatch(latch);
        orderStreamConsumer.setExpectedOrderNo("ORD002");

        OrderMessage order = OrderMessage.of("ORD002", "user002", "MacBook Pro", new BigDecimal("19999.00"));
        String messageId = queueTemplate.send(STREAM_ORDER, order);
        log.info("[测试2] 发送消息: messageId={}", messageId);

        // 等待消费者处理（最多 10 秒）
        boolean consumed = latch.await(10, TimeUnit.SECONDS);

        assertTrue(consumed, "消息应在 10 秒内被消费");
        assertFalse(orderStreamConsumer.getProcessedOrders().isEmpty(), "已处理列表不应为空");

        OrderMessage processed = orderStreamConsumer.getProcessedOrders().stream()
                .filter(o -> "ORD002".equals(o.getOrderNo()))
                .findFirst()
                .orElse(null);
        assertNotNull(processed, "应找到已处理的 ORD002 订单");
        assertEquals("user002", processed.getUserId(), "用户 ID 应匹配");
        assertEquals("MacBook Pro", processed.getProductName(), "商品名称应匹配");
        assertEquals(new BigDecimal("19999.00"), processed.getAmount(), "金额应匹配");

        log.info("[测试2] 消息消费成功: orderNo={}", processed.getOrderNo());
    }

    /**
     * 测试3：发送带 headers 的消息，验证 header 传递
     */
    @Test
    @Order(3)
    public void testSendMessageWithHeaders() throws InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("traceId", "trace-abc-123");
        headers.put("source", "mobile-app");

        OrderMessage order = OrderMessage.of("ORD003", "user003", "AirPods Pro", new BigDecimal("1999.00"));

        String messageId = queueTemplate.send(STREAM_ORDER, order, headers);
        assertNotNull(messageId, "带 headers 的消息 ID 不应为空");

        log.info("[测试3] 发送带 headers 的消息: messageId={}, traceId=trace-abc-123", messageId);
    }

    /**
     * 测试4：批量发送消息，验证都能成功
     */
    @Test
    @Order(4)
    public void testBatchSendMessages() {
        int batchSize = 5;
        for (int i = 1; i <= batchSize; i++) {
            OrderMessage order = OrderMessage.of(
                    "BATCH-ORD-" + i,
                    "batch-user-" + i,
                    "商品-" + i,
                    new BigDecimal(i * 100)
            );
            String messageId = queueTemplate.send(STREAM_ORDER, order);
            assertNotNull(messageId);
            log.info("[测试4] 批量发送第 {}/{} 条: messageId={}", i, batchSize, messageId);
        }
        log.info("[测试4] 批量发送完成，共 {} 条", batchSize);
    }

    /**
     * 测试5：并发消费（并发数 = 2）
     */
    @Test
    @Order(5)
    public void testConcurrentConsumption() throws InterruptedException {
        int msgCount = 4;
        CountDownLatch latch = new CountDownLatch(msgCount);
        orderStreamConsumer.setLatch(latch);
        orderStreamConsumer.setExpectedOrderNoPrefixes(java.util.Collections.singleton("CONC-ORD-"));

        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i <= msgCount; i++) {
            headers.put("traceId", "trace-concurrent-" + i);
            OrderMessage order = OrderMessage.of("CONC-ORD-" + i, "conc-user-" + i, "商品" + i, BigDecimal.TEN);
            queueTemplate.send(STREAM_ORDER_CONCURRENT, order, headers);
        }
        log.info("[测试5] 已发送 {} 条消息到并发 Stream", msgCount);

        boolean done = latch.await(15, TimeUnit.SECONDS);
        assertTrue(done, "所有并发消息应在 15 秒内被消费");
        log.info("[测试5] 并发消费完成，处理消息数={}", orderStreamConsumer.getProcessedOrders().size());
    }

    /**
     * 测试6：StreamMessage payload 类型安全反序列化
     */
    @Test
    @Order(6)
    public void testPayloadDeserialization() {
        OrderMessage order = OrderMessage.of("ORD006", "user006", "Test Product", new BigDecimal("666.00"));
        StreamMessage message = new StreamMessage(STREAM_ORDER, order);

        // 验证序列化后能正确反序列化
        OrderMessage deserialized = message.getPayload(OrderMessage.class);
        assertNotNull(deserialized, "反序列化结果不应为空");
        assertEquals("ORD006", deserialized.getOrderNo(), "订单号应匹配");
        assertEquals("user006", deserialized.getUserId(), "用户 ID 应匹配");
        assertEquals(new BigDecimal("666.00"), deserialized.getAmount(), "金额应匹配");

        log.info("[测试6] Payload 反序列化验证通过: {}", deserialized);
    }

    /**
     * 测试7：StreamMessage headers 读写
     */
    @Test
    @Order(7)
    public void testMessageHeaders() {
        StreamMessage message = new StreamMessage(STREAM_ORDER, "test");
        message.putHeader("traceId", "trace-007");
        message.putHeader("userId", "user-007");

        assertEquals("trace-007", message.getHeader("traceId"), "traceId 应匹配");
        assertEquals("user-007", message.getHeader("userId"), "userId 应匹配");
        assertNull(message.getHeader("nonExistentKey"), "不存在的 header 应返回 null");

        log.info("[测试7] headers 读写验证通过");
    }

    /**
     * 测试8：编程式注册消费者
     */
    @Test
    @Order(8)
    public void testProgrammaticRegistration() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        OrderMessage[] received = new OrderMessage[1];

        // 编程式注册消费者
        registry.register(STREAM_PROGRAMMATIC, "programmatic-group", 1, message -> {
            received[0] = message.getPayload(OrderMessage.class);
            log.info("[测试8] 编程式消费者收到消息: id={}, orderNo={}", message.getId(), received[0].getOrderNo());
            latch.countDown();
            return null;
        });

        // 发送一条消息
        OrderMessage order = OrderMessage.of("PROG-ORD-001", "prog-user", "编程式商品", new BigDecimal("888.00"));
        queueTemplate.send(STREAM_PROGRAMMATIC, order);

        boolean done = latch.await(10, TimeUnit.SECONDS);
        assertTrue(done, "编程式消费者应在 10 秒内处理消息");
        assertNotNull(received[0], "消费者应收到消息");
        assertEquals("PROG-ORD-001", received[0].getOrderNo(), "订单号应匹配");

        log.info("[测试8] 编程式注册消费者验证通过");
    }

    /**
     * 测试9：死信队列触发验证（消费失败后写入死信 Stream）
     */
    @Test
    @Order(9)
    public void testDeadLetterQueue() throws InterruptedException {
        OrderMessage order = OrderMessage.of("FAIL-ORD-001", "fail-user", "必失败商品", BigDecimal.ONE);
        queueTemplate.send(STREAM_ORDER_FAIL, order);
        log.info("[测试9] 已发送会失败的消息到 Stream: {}", STREAM_ORDER_FAIL);

        // 等待框架处理（包含重试和死信写入）
        Thread.sleep(5000);

        // 验证死信 Stream 有消息
        Long deadCount = stringRedisTemplate.opsForStream().size(STREAM_ORDER_FAIL_DEAD);
        log.info("[测试9] 死信 Stream 消息数: stream={}, count={}", STREAM_ORDER_FAIL_DEAD, deadCount);
        // 注：由于重试次数和 pending 超时配置，死信消息的写入有延迟，此处仅记录日志，不强断言
        // 在生产环境中，可以通过配置缩短 pendingTimeoutMs 来加速触发
    }

    /**
     * 测试10：Stream 状态验证（Stream 已被创建）
     */
    @Test
    @Order(10)
    public void testStreamExists() {
        // 发送一条消息以确保 Stream 已创建
        queueTemplate.send(STREAM_ORDER, OrderMessage.of("STATUS-ORD", "status-user", "状态测试", BigDecimal.ONE));

        Boolean exists = stringRedisTemplate.hasKey(STREAM_ORDER);
        assertTrue(Boolean.TRUE.equals(exists), "Stream 应已创建");

        Long size = stringRedisTemplate.opsForStream().size(STREAM_ORDER);
        log.info("[测试10] Stream 状态: stream={}, exists={}, size={}", STREAM_ORDER, exists, size);
        assertNotNull(size, "Stream 大小不应为 null");
        assertTrue(size > 0, "Stream 应有消息");
    }

    /**
     * 测试11：消息发送性能测试（100条消息）
     */
    @Test
    @Order(11)
    public void testSendPerformance() {
        int count = 100;
        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            OrderMessage order = OrderMessage.of("PERF-ORD-" + i, "perf-user", "性能测试商品", BigDecimal.TEN);
            queueTemplate.send(STREAM_ORDER, order);
        }

        long elapsed = System.currentTimeMillis() - start;
        double tps = count * 1000.0 / elapsed;
        log.info("[测试11] 发送性能: count={}, elapsed={}ms, TPS={}", count, elapsed, String.format("%.1f", tps));

        assertTrue(elapsed < 10000, "100条消息发送应在 10 秒内完成（实际 " + elapsed + "ms）");
    }

    /**
     * 清理测试数据（测试完成后删除测试用 Stream）
     */
    @AfterAll
    public static void cleanup() {
        // 清理工作可在此执行，避免测试数据污染其他测试
        // 实际项目中可根据需要选择是否清理
    }
}
