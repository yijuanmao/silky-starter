package com.silky.starter.redis.test.redission.entity;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Order
 *
 * @author zy
 * @date 2025-10-23 14:58
 **/
@Data
@ToString
public class TradeOrder {

    private String orderId;

    private String orderName;

    private BigDecimal amount;

    private String status;
}
