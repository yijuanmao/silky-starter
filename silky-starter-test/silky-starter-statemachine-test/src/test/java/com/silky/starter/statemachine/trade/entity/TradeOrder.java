package com.silky.starter.statemachine.trade.entity;

import java.io.Serializable;

/**
 * 交易订单实体类
 *
 * @author zy
 * @date 2025-10-09 14:59
 **/
public class TradeOrder implements Serializable {

    private static final long serialVersionUID = 2559764767295561674L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 订单状态
     */
    private String orderStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public TradeOrder() {
    }

    public TradeOrder(Long id, String orderStatus) {
        this.id = id;
        this.orderStatus = orderStatus;
    }

    @Override
    public String toString() {
        return "TradeOrder{" +
                "id=" + id +
                ", orderStatus='" + orderStatus + '\'' +
                '}';
    }
}
