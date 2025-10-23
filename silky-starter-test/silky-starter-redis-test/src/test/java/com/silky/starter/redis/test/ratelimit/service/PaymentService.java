package com.silky.starter.redis.test.ratelimit.service;

import com.silky.starter.redis.test.ratelimit.request.PaymentRequest;
import com.silky.starter.redis.test.redission.result.PaymentResult;
import org.springframework.stereotype.Service;

/**
 * Payment服务
 *
 * @author zy
 * @date 2025-10-23 16:27
 **/
@Service
public class PaymentService {

    PaymentResult createPayment(Long userId, PaymentRequest request) {
        // 模拟创建支付订单的业务逻辑
        return new PaymentResult();
    }
}
