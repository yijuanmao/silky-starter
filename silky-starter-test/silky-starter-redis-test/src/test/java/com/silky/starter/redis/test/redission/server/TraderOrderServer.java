package com.silky.starter.redis.test.redission.server;

import cn.hutool.core.thread.ThreadUtil;
import com.silky.starter.redis.lock.annotation.RedisLock;
import com.silky.starter.redis.lock.enums.LockType;
import com.silky.starter.redis.lock.template.RedisLockTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

/**
 * TraderOrderServer
 *
 * @author zy
 * @date 2025-10-22 17:34
 **/
@Slf4j
@Service
public class TraderOrderServer {
    private static final String LOCK_KEY = "lock";
    @Autowired
    private RedisLockTemplate lockTemplate;

    /**
     * 模式提交事务后释放锁测试方法
     */
    @Transactional
    public void createOrder() {
        boolean lock = lockTemplate.lock(LOCK_KEY, 1, 10, TimeUnit.SECONDS, true, () -> {

            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("当前事务是否激活：{}", isActive);
            //执行业务逻辑方法
            log.info("获取到锁，执行业务逻辑方法");
            ThreadUtil.sleep(1000);
            log.info("执行业务逻辑方法");
            return true;
        });
    }

    /**
     * 锁注解测试方法,支持使用SpEL表达式
     * 比如这里需要带上orderId和id两个参数，可以这样写：@RedisLock(key = "'order:' + #orderId + ':id:' + #id")
     *
     * @param orderId 订单ID
     * @param id      标识ID
     */
//    @RedisLock(key = "'order:' + #orderId",
//            lockType = LockType.REENTRANT,
//            waitTime = 10, leaseTime = 30)
    @RedisLock(key = "'order:' + #orderId + ':id:' + #id",
            lockType = LockType.REENTRANT,
            waitTime = 10, leaseTime = 30)
    @Transactional
    public void processOrder(String orderId, String id) {
        log.info("Processing order: {}, id:{}", orderId, id);
        // 业务处理，锁会在事务提交后释放
        // ...
        log.info("Processing order: {}, id:{}", orderId, id);
    }
}
