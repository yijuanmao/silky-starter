package com.silky.starter.excel.enums;

import lombok.Getter;

/**
 * 导出状态
 *
 * @author zy
 * @date 2025-10-24 11:32
 **/
@Getter
public enum ExportStatus {

    /**
     * 等待中 - 任务已创建，等待执行
     */
    PENDING("等待中"),

    /**
     * 处理中 - 任务正在执行导出操作
     */
    PROCESSING("处理中"),

    /**
     * 已完成 - 任务成功完成，文件已生成
     */
    COMPLETED("已完成"),

    /**
     * 已失败 - 任务执行过程中发生错误
     */
    FAILED("已失败"),

    /**
     * 已取消 - 任务被用户或系统取消
     */
    CANCELLED("已取消"),

    /**
     * 部分完成 - 部分数据导出成功（适用于复杂导出场景）
     */
    PARTIAL("部分完成"),


    ;


    /**
     * 枚举描述
     */
    private final String description;

    ExportStatus(String description) {
        this.description = description;
    }

    /**
     * 检查状态是否为最终状态（不会再改变的状态）
     *
     * @return 如果是最终状态返回true，否则返回false
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    /**
     * 检查状态是否为进行中状态
     *
     * @return 如果是进行中状态返回true，否则返回false
     */
    public boolean isInProgress() {
        return this == PENDING || this == PROCESSING;
    }

    /**
     * 检查状态是否为成功状态
     *
     * @return 如果是成功状态返回true，否则返回false
     */
    public boolean isSuccess() {
        return this == COMPLETED || this == PARTIAL;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 枚举编码
     * @return 对应的枚举值，如果找不到返回null
     */
    public static ExportStatus getByCode(String code) {
        for (ExportStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
