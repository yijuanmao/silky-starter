package com.silky.starter.rabbitmq.test.entity;

import com.silky.starter.rabbitmq.core.BaseMassageSend;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交易订单实体类
 *
 * @author zy
 * @date 2025-10-15 15:09
 **/
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class TradeOrder extends BaseMassageSend implements Serializable {

    private static final long serialVersionUID = 5009955102872378736L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 订单名称
     */
    private String orderName;

    public TradeOrder() {
    }

    public TradeOrder(Long orderId, LocalDateTime createTime, String orderName) {
        this.orderId = orderId;
        this.createTime = createTime;
        this.orderName = orderName;
    }
}
