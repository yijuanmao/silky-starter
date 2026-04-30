package com.silky.starter.excel.template.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举 - 用于 @ExcelEnum 测试
 * @author zy
 */
@Getter
@AllArgsConstructor
public enum UserStatus {

    ACTIVE(1, "启用"),
    DISABLED(0, "禁用"),
    LOCKED(2, "锁定");

    private final int code;
    private final String label;
}
