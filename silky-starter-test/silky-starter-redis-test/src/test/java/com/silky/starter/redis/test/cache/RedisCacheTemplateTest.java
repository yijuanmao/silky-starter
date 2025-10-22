package com.silky.starter.redis.test.cache;

import cn.hutool.core.collection.ListUtil;
import com.silky.starter.redis.cache.template.RedisCacheTemplate;
import com.silky.starter.redis.test.RedisApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * RedisCacheTemplateTest
 *
 * @author zy
 * @date 2025-10-22 15:37
 **/
public class RedisCacheTemplateTest extends RedisApplicationTest {

    private static final String KEY = "silky:test";

    private static final String KEY_LIST = "silky:list";

    @Autowired
    private RedisCacheTemplate redisCacheTemplate;

    /**
     * set测试方法
     */
    @Test
    public void setTest() {
        redisCacheTemplate.setObject(KEY, this.buildTradeOrder(), 1L, TimeUnit.HOURS);
    }

    /**
     * set批量测试方法
     */
    @Test
    public void batchSetTest() {
        redisCacheTemplate.setObject(KEY_LIST, this.buildTradeOrders(), 1L, TimeUnit.HOURS);
    }


    /**
     * 删除测试方法
     */
    @Test
    public void deleteTest() {
        redisCacheTemplate.delete(KEY);
    }


    /**
     * 获取测试方法
     */
    @Test
    public void getTest() {
        TradeOrder object = redisCacheTemplate.getObject(KEY);
        log.info("获取到的对象：{}", object);
    }

    /**
     * 获取测试方法
     */
    @Test
    public void getListTest() {
        List<TradeOrder> list = redisCacheTemplate.getObject(KEY_LIST);
        for (TradeOrder order : list) {
            log.info("订单对象：{}", order);
        }
        log.info("获取到的对象列表：{}", list);
    }

    /**
     * 构建TradeOrder对象
     *
     * @return TradeOrder对象
     */
    private TradeOrder buildTradeOrder() {
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(System.currentTimeMillis());
        tradeOrder.setCreateTime(java.time.LocalDateTime.now());
        tradeOrder.setOrderName("测试订单-缓存");
        tradeOrder.setAmount(BigDecimal.ZERO);
        return tradeOrder;
    }

    /**
     * 构建TradeOrder对象
     *
     * @return List<TradeOrder>
     */
    private List<TradeOrder> buildTradeOrders() {
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(System.currentTimeMillis());
        tradeOrder.setCreateTime(java.time.LocalDateTime.now());
        tradeOrder.setOrderName("测试订单-缓存");
        tradeOrder.setAmount(BigDecimal.ONE);

        TradeOrder tradeOrder1 = new TradeOrder();
        tradeOrder1.setOrderId(System.currentTimeMillis());
        tradeOrder1.setCreateTime(java.time.LocalDateTime.now());
        tradeOrder1.setOrderName("测试订单1-缓存");
        tradeOrder1.setAmount(BigDecimal.valueOf(20));

        return ListUtil.of(tradeOrder, tradeOrder1);
    }
}
