package com.silky.starter.redis.lock.template;

import com.silky.starter.redis.lock.enums.LockType;
import com.silky.starter.redis.lock.exception.RedissonException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * redis分布式锁模板类
 *
 * @author zy
 * @date 2025-10-21 15:39
 **/
public class RedisLockTemplate {

    private static final Logger log = LoggerFactory.getLogger(RedisLockTemplate.class);

    private final RedissonClient redissonClient;

    public RedisLockTemplate(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 执行带锁的操作（推荐使用）
     *
     * @param key       锁的key
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 锁的租期时间
     * @param supplier  锁操作的函数
     * @param timeUnit  时间单位
     * @return 锁操作的结果
     */
    public <T> T lock(String key, long waitTime, long leaseTime,
                      TimeUnit timeUnit, Supplier<T> supplier) {
        return lock(key, waitTime, leaseTime, timeUnit, false, supplier);
    }

    /**
     * 执行带锁的操作（推荐使用）
     *
     * @param key                     锁的key
     * @param waitTime                获取锁的最大等待时间
     * @param leaseTime               锁的租期时间
     * @param supplier                锁操作的函数
     * @param timeUnit                时间单位
     * @param releaseAfterTransaction 是否在事务完成后释放锁
     * @return 锁操作的结果
     */
    public <T> T lock(String key, long waitTime, long leaseTime,
                      TimeUnit timeUnit, boolean releaseAfterTransaction,
                      Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(key);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!isLocked) {
                throw new RedissonException("获取分布式锁失败, key: " + key);
            }
            if (releaseAfterTransaction && TransactionSynchronizationManager.isActualTransactionActive()) {
                // 注册事务同步
                registerTransactionCompletionCallback(lock);
                return supplier.get();
            } else {
                try {
                    return supplier.get();
                } finally {
                    safeUnlock(lock);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断,锁key: " + key, e);
            throw new RedissonException("获取分布式锁被中断,锁key: " + key, e);
        } catch (Exception e) {
            if (isLocked && lock.isHeldByCurrentThread() && !(releaseAfterTransaction && TransactionSynchronizationManager.isActualTransactionActive())) {
                safeUnlock(lock);
            }
            throw e;
        }
    }

    /**
     * 执行带锁的操作（无返回值）
     *
     * @param key                     锁的key
     * @param waitTime                获取锁的最大等待时间
     * @param leaseTime               锁的租期时间
     * @param timeUnit                时间单位
     * @param releaseAfterTransaction 是否在事务完成后释放锁
     * @param runnable                锁操作的函数
     */
    public void lock(String key, long waitTime, long leaseTime,
                     TimeUnit timeUnit, boolean releaseAfterTransaction,
                     Runnable runnable) {
        this.lock(key, waitTime, leaseTime, timeUnit, releaseAfterTransaction, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 直接获取RLock对象，提供最大的灵活性
     */
    public RLock getRLock(String key) {
        return getRLock(key, LockType.REENTRANT);
    }

    /**
     * 直接获取RLock对象，提供最大的灵活性
     */
    public RLock getRLock(String key, LockType lockType) {
        return redissonClient.getLock(key);
    }

    /**
     * 强制解锁（谨慎使用）
     *
     * @param key 锁的key
     */
    public void forceUnlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            safeUnlock(lock);
        }
    }

    /**
     * 检查锁是否被当前线程持有
     *
     * @param key 锁的key
     */
    public boolean isHeldByCurrentThread(String key) {
        RLock lock = redissonClient.getLock(key);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 获取锁剩余租期时间
     *
     * @param key 锁的key
     */
    public long remainTimeToLive(String key) {
        RLock lock = redissonClient.getLock(key);
        return lock.remainTimeToLive();
    }

    /**
     * 注册事务完成回调以释放锁
     *
     * @param lock 锁对象
     */
    private void registerTransactionCompletionCallback(RLock lock) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        safeUnlock(lock);
                    }
                }
        );
    }

    /**
     * 释放锁（线程安全）
     *
     * @param lock 锁对象
     */
    private void safeUnlock(RLock lock) {
        if (lock != null && lock.isLocked() && lock.isHeldByCurrentThread()) {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                // 锁可能已经自动释放，忽略此异常
            }
        }
    }
}
