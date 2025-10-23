package com.silky.starter.redis.test.redission.mapper;

import com.silky.starter.redis.test.redission.entity.TradeOrder;
import org.springframework.stereotype.Component;

/**
 * 订单仓库
 *
 * @author zy
 * @date 2025-10-23 15:03
 **/
@Component
public interface OrderRepository {

    /**
     * 这里模拟查询数据库
     *
     * @param orderNo 订单号
     * @return TradeOrder
     */
    TradeOrder findByOrderNo(String orderNo);

    /**
     * 这里模拟保存数据库
     *
     * @param tradeOrder 订单
     */
    void save(TradeOrder tradeOrder);
}
