package com.silky.starter.redis.lock.enums;

/**
 * 锁类型枚举
 *
 * @author zy
 * @date 2025-10-21 15:33
 **/
public enum LockType {

    /**
     * 可重入锁
     */
    REENTRANT,

    /**
     * 公平锁
     */
    FAIR,

    /**
     * 读锁
     */
    READ,

    /**
     * 写锁
     */
    WRITE
}
