package com.silky.starter.redis.test.redission;

import cn.hutool.core.thread.ThreadUtil;
import com.silky.starter.redis.lock.template.RedisLockTemplate;
import com.silky.starter.redis.test.RedisApplicationTest;
import com.silky.starter.redis.test.redission.server.OrderService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

/**
 * RedisLockTemplateTest
 *
 * @author zy
 * @date 2025-10-22 17:20
 **/
public class RedisLockTemplateTest extends RedisApplicationTest {
    private static final String LOCK_KEY = "lock";

    @Autowired
    private RedisLockTemplate redisLockTemplate;
    @Autowired
    private OrderService orderService;

    /**
     * 锁测试方法
     */
    @Test
    public void lockTest() {
        RLock lock = redisLockTemplate.getRLock(LOCK_KEY);
        try {
            lock.tryLock(1, 10, TimeUnit.SECONDS);
            //执行业务逻辑方法
            log.info("获取到锁，执行业务逻辑方法");
            ThreadUtil.sleep(1000);
            log.info("执行业务逻辑方法");
        } catch (InterruptedException e) {
            log.error("获取锁异常", e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                //释放锁
                lock.unlock();
            }
        }
    }

    /**
     * 测试事务提交后释放锁方式
     */
    @Test
    public void tryLockTest() {
        orderService.createOrder();
        log.info("执行完成");
    }

    /**
     * 测试事务提交后释放锁方式
     */
    @Test
    public void processOrderTest() {
        orderService.processOrder(System.currentTimeMillis() + "", "1");
        log.info("执行完成");
    }
}
