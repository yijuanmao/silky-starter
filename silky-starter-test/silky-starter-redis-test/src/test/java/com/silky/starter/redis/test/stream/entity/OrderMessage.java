package com.silky.starter.redis.test.stream.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 测试用订单实体
 *
 * @author zy
 * @date 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 订单状态
     */
    private String status;

    public static OrderMessage of(String orderNo, String userId, String productName, BigDecimal amount) {
        return new OrderMessage(orderNo, userId, productName, amount, "PENDING");
    }
}
