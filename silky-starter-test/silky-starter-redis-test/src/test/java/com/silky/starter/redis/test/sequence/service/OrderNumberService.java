package com.silky.starter.redis.test.sequence.service;

import com.silky.starter.redis.sequence.annotation.RedisSequence;
import org.springframework.stereotype.Service;

/**
 * redis序列号生成器默认实现
 *
 * @author zy
 * @date 2025-10-23 16:03
 **/
@Service
public class OrderNumberService {

    /**
     * 生成订单编号
     */
    @RedisSequence(
            redisKey = "test:order:number",      // redis缓存key
            prefix = "ORDER",                    // 业务前缀
            datePattern = "yyyyMMddHHmmss",     // 时间格式
            sequenceLength = 6,                 // 序列号长度
            randomLength = 3                  // 随机数长度
    )
    public String generateOrderNumber() {
        return null; // 实际由切面自动生成并返回序列号，并不会返回null
        // 生成示例: ORDER20251023160705000001631
        // 格式: 前缀 + 时间戳 + 序列号 + 随机数
    }

    /**
     * 生成支付编号
     */
    @RedisSequence(
            redisKey = "test:pay:number",      // redis缓存key
            prefix = "PAY",
            sequenceLength = 8,
            randomLength = 2
    )
    public String generatePaymentNo() {
        return null;
        // 生成示例: PAY202510230000000143
    }
}
