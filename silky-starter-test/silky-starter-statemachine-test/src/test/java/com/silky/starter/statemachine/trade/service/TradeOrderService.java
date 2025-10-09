package com.silky.starter.statemachine.trade.service;

import com.silky.starter.statemachine.trade.entity.TradeOrder;
import com.silky.starter.statemachine.trade.enums.OrderStatusEnum;
import org.springframework.stereotype.Service;

/**
 * 交易订单服务
 *
 * @author zy
 * @date 2025-10-09 15:11
 **/
@Service
public class TradeOrderService {


    /**
     * 根据id交易订单
     *
     * @param id 主键id
     * @return 交易订单
     */
    public TradeOrder selectTradeOrderById(Long id) {
        //这里模拟从数据库查询
        return new TradeOrder(id, OrderStatusEnum.PAYING.name());
    }

    /**
     * 更新交易订单
     *
     * @param order 交易订单
     */
    public void updateTradeOrder(TradeOrder order) {
        //这里模拟更新数据库
        System.out.println("TradeOrder updated: " + order);
    }
}
