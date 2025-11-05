package com.silky.starter.excel.enums;

import lombok.Getter;

/**
 * 异步类型
 *
 * @author zy
 * @date 2025-10-24 11:31
 **/
@Getter
public enum AsyncType {

    /**
     * 同步执行 - 在当前线程中立即执行
     */
    SYNC("同步执行"),

    /**
     * 线程异步
     */
    THREAD_POOL("线程异步"),

    /**
     * 自定义异步执行 - 使用用户自定义的异步处理器
     */
    CUSTOM("自定义异步");

    /**
     * 异步类型描述
     */
    private final String description;

    AsyncType(String description) {
        this.description = description;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 枚举编码
     * @return 对应的枚举值，如果找不到返回null
     */
    public static AsyncType getByCode(String code) {
        for (AsyncType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 检查是否为异步类型
     *
     * @return 如果是异步类型返回true，同步类型返回false
     */
    public boolean isAsync() {
        return this != SYNC;
    }
}
