package com.silky.starter.redis.test.ratelimit.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付请求
 *
 * @author zy
 * @date 2025-10-23 16:26
 **/
@Data
public class PaymentRequest {

    private String orderId;

    private BigDecimal amount;
}
