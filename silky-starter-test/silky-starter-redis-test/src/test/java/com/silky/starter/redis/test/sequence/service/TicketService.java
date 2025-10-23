package com.silky.starter.redis.test.sequence.service;

import com.silky.starter.redis.sequence.template.RedisSequenceTemplate;
import com.silky.starter.redis.test.RedisApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * redis序列号生成器默认实现
 *
 * @author zy
 * @date 2025-10-23 15:25
 **/
public class TicketService extends RedisApplicationTest {

    private static final String REDIS_KEY = "test:";

    @Autowired
    private RedisSequenceTemplate sequenceTemplate;
    @Autowired
    private OrderNumberService orderNumberService;

    /**
     * 生成票务编号
     */
    @Test
    public void testGenerateTicketNumber() {
        // 方式1：快速生成
        String ticketNo = sequenceTemplate.generate(REDIS_KEY, "TICKET", "yyyyMMdd", 10, 6);
        // 示例: TICKET202510230000000003953818
        log.info("生成票务编号: {}", ticketNo);
    }

    /**
     * 批量生成序列号
     */
    @Test
    public void testBatchGenerateNumbers() {
        List<String> numbers = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String number = sequenceTemplate.generate(REDIS_KEY, "TICKET", "yyyyMMdd", 10, 6);
            numbers.add(number);
        }
        log.info("批量生成 {} 个序列号完成", numbers);
    }

    /**
     * 生成当前序列号，比如：餐饮、票务等不需要日期前缀的场景，只需要一个递增的序列号
     */
    @Test
    public void testGenNowSerialNumber() {
        String number = sequenceTemplate.genNowSerialNumber(REDIS_KEY, "B", 5);
        // 示例: B00006
        log.info("生成当前序列号: {}", number);
    }

    /**
     * 测试生成订单编号
     */
    @Test
    public void testGenerateOrderNumber() {
        String orderNumber = orderNumberService.generateOrderNumber();
        log.info("生成订单编号: {}", orderNumber);
    }

    /**
     * 测试生成支付编号
     */
    @Test
    public void testGeneratePaymentNo() {
        String orderNumber = orderNumberService.generatePaymentNo();
        log.info("测试生成支付编号: {}", orderNumber);
    }
}
