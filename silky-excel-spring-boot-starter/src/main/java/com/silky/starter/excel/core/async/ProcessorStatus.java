package com.silky.starter.excel.core.async;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 处理器状态模型,用于监控和管理异步处理器的状态信息
 *
 * @author zy
 * @date 2025-10-24 14:29
 **/
@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessorStatus implements Serializable {

    private static final long serialVersionUID = -1004688778791966929L;

    /**
     * 处理器类型
     * 与AsyncType枚举值对应
     */
    private String type;

    /**
     * 处理器是否可用
     * true表示可以接收新任务，false表示不可用
     */
    private boolean available;

    /**
     * 状态描述信息
     * 包含详细的状态说明，如错误信息、警告信息等
     */
    private String message;

    /**
     * 已处理的任务数量
     * 从处理器启动开始累计处理的任务数量
     */
    private Long processedCount;

    /**
     * 队列中的任务数量
     * 当前等待处理的任务数量（如果适用）
     */
    private Long queueSize;

    /**
     * 处理器启动时间 处理器初始化的时间戳
     */
    private LocalDateTime startTime;

    /**
     * 最后活跃时间 处理器最后一次处理任务的时间戳
     */
    private LocalDateTime lastActiveTime;

    /**
     * 创建基础状态对象
     *
     * @param type      处理器类型
     * @param available 是否可用
     * @return 处理器状态实例
     */
    public static ProcessorStatus of(String type, boolean available) {
        return ProcessorStatus.builder()
                .type(type)
                .available(available)
                .startTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建带消息的状态对象
     *
     * @param type      处理器类型
     * @param available 是否可用
     * @param message   状态消息
     * @return 处理器状态实例
     */
    public static ProcessorStatus of(String type, boolean available, String message) {
        return ProcessorStatus.builder()
                .type(type)
                .available(available)
                .message(message)
                .startTime(LocalDateTime.now())
                .build();
    }

    /**
     * 更新最后活跃时间
     * 在处理器处理任务时调用
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 获取运行时长（毫秒）
     *
     * @return 从启动到现在的运行时长
     */
    public long getUptime() {
        if (startTime == null) {
            return 0;
        }
        return System.currentTimeMillis() - startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }

    /**
     * 获取空闲时长（毫秒）
     *
     * @return 从最后活跃到现在的空闲时长
     */
    public long getIdleTime() {
        if (lastActiveTime == null) {
            return getUptime();
        }
        return System.currentTimeMillis() - lastActiveTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }
}
