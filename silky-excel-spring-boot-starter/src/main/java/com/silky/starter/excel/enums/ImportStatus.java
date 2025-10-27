package com.silky.starter.excel.enums;

import lombok.Getter;

/**
 * 导入状态
 *
 * @author zy
 * @date 2025-10-24 11:32
 **/
@Getter
public enum ImportStatus {

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


    ;


    /**
     * 枚举描述
     */
    private final String description;

    ImportStatus(String description) {
        this.description = description;
    }

}
