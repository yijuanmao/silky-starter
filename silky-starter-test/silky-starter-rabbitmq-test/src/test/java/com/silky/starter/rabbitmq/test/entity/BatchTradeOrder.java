package com.silky.starter.rabbitmq.test.entity;

import com.silky.starter.rabbitmq.core.model.BaseMassageSend;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 批量交易订单
 * @author zy
 * @date 2025-10-16 11:35
 **/
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class BatchTradeOrder extends BaseMassageSend implements Serializable {

    private static final long serialVersionUID = 7197785208416129372L;

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * 交易订单列表
     */
    private List<TradeOrder> tradeOrders;

    public BatchTradeOrder() {
    }

    public BatchTradeOrder(List<TradeOrder> tradeOrders) {
        this.tradeOrders = tradeOrders;
    }
}
