package com.silky.starter.redis.lock.template;

import com.silky.starter.redis.lock.enums.LockType;
import lombok.Getter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
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
@Getter
public class RedisLockTemplate {

    private final RedissonClient redissonClient;

    public RedisLockTemplate(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 执行带锁的操作（推荐使用）
     *
     * @param key       锁的key
     * @param lockType  锁类型
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 锁的租期时间
     * @param supplier  锁操作的函数
     * @param timeUnit  时间单位
     * @return 锁操作的结果
     */
    public <T> T lock(String key, LockType lockType, long waitTime, long leaseTime,
                      TimeUnit timeUnit, Supplier<T> supplier) {
        return lock(key, lockType, waitTime, leaseTime, timeUnit, false, supplier);
    }

    /**
     * 执行带锁的操作（推荐使用）
     *
     * @param key                     锁的key
     * @param lockType                锁类型
     * @param waitTime                获取锁的最大等待时间
     * @param leaseTime               锁的租期时间
     * @param supplier                锁操作的函数
     * @param timeUnit                时间单位
     * @param releaseAfterTransaction 是否在事务完成后释放锁
     * @return 锁操作的结果
     */
    public <T> T lock(String key, LockType lockType, long waitTime, long leaseTime,
                      TimeUnit timeUnit, boolean releaseAfterTransaction,
                      Supplier<T> supplier) {
        RLock lock = getLock(lockType, key);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!isLocked) {
                throw new RuntimeException("获取分布式锁失败, key: " + key);
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
            throw new RuntimeException("获取分布式锁被中断", e);
        } catch (Exception e) {
            if (isLocked && lock.isHeldByCurrentThread() &&
                    !(releaseAfterTransaction && TransactionSynchronizationManager.isActualTransactionActive())) {
                safeUnlock(lock);
            }
            throw e;
        }
    }

    /**
     * 执行带锁的操作（无返回值）
     *
     * @param key                     锁的key
     * @param lockType                锁类型
     * @param waitTime                获取锁的最大等待时间
     * @param leaseTime               锁的租期时间
     * @param timeUnit                时间单位
     * @param releaseAfterTransaction 是否在事务完成后释放锁
     * @param runnable                锁操作的函数
     */
    public void lock(String key, LockType lockType, long waitTime, long leaseTime,
                     TimeUnit timeUnit, boolean releaseAfterTransaction,
                     Runnable runnable) {
        this.lock(key, lockType, waitTime, leaseTime, timeUnit, releaseAfterTransaction, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 尝试获取锁（非阻塞）
     *
     * @param key       锁的key
     * @param lockType  锁类型
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 锁的租期时间
     * @param timeUnit  时间单位
     */
    public boolean tryLock(String key, LockType lockType, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getLock(lockType, key);
        try {
            return lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 直接获取RLock对象，提供最大的灵活性
     */
    public RLock getRLock(String key, LockType lockType) {
        return getLock(lockType, key);
    }

    /**
     * 强制解锁（谨慎使用）
     *
     * @param key      锁的key
     * @param lockType 锁类型
     */
    public void forceUnlock(String key, LockType lockType) {
        RLock lock = getLock(lockType, key);
        if (lock.isHeldByCurrentThread()) {
            safeUnlock(lock);
        }
    }

    /**
     * 检查锁是否被当前线程持有
     *
     * @param key      锁的key
     * @param lockType 锁类型
     */
    public boolean isHeldByCurrentThread(String key, LockType lockType) {
        RLock lock = getLock(lockType, key);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 获取锁剩余租期时间
     *
     * @param key      锁的key
     * @param lockType 锁类型
     */
    public long remainTimeToLive(String key, LockType lockType) {
        RLock lock = getLock(lockType, key);
        return lock.remainTimeToLive();
    }

    /**
     * 获取锁对象
     *
     * @param lockType 锁类型
     * @param key      key
     * @return RLock
     */
    private RLock getLock(LockType lockType, String key) {
        switch (lockType) {
            case FAIR:
                return redissonClient.getFairLock(key);
            case READ:
                RReadWriteLock readLock = redissonClient.getReadWriteLock(key);
                return readLock.readLock();
            case WRITE:
                RReadWriteLock writeLock = redissonClient.getReadWriteLock(key);
                return writeLock.writeLock();
            case REENTRANT:
            default:
                return redissonClient.getLock(key);
        }
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
