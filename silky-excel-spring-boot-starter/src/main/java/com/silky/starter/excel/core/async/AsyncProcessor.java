package com.silky.starter.excel.core.async;

import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.export.ExportTask;

/**
 * 异步处理器接口
 * 定义异步处理导出任务的标准方法，支持不同的异步处理策略
 * 用户可以实现此接口来自定义异步处理方式
 *
 * @author zy
 * @date 2025-10-24 14:27
 **/
public interface AsyncProcessor {

    /**
     * 获取处理器类型，类型应该与AsyncType枚举值对应，用于在工厂中标识处理器
     * 参照枚举类:{@link com.silky.starter.excel.enums.AsyncType}
     */
    String getType();


    /**
     * 初始化处理器,在处理器注册到工厂后调用，用于初始化资源
     */
    default void init() throws ExcelExportException {
        // 默认空实现
    }

    /**
     * 销毁处理器
     * 在处理器从工厂注销时调用，用于释放资源
     */
    default void destroy() throws ExcelExportException {
        // 默认空实现
    }

    /**
     * 检查处理器是否可用，用于判断处理器当前是否能够处理任务
     *
     * @return 如果处理器可用返回true，否则返回false
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 获取处理器状态
     * 返回处理器的当前状态信息，用于监控和管理
     *
     * @return 处理器状态对象，包含类型、可用性、处理数量等信息
     */
    default ProcessorStatus getStatus() {
        return ProcessorStatus.builder()
                .type(getType())
                .available(isAvailable())
                .build();
    }

    /**
     * 获取处理器描述信息
     *
     * @return 处理器描述信息
     */
    default String getDescription() {
        return "Async Processor: " + getType();
    }
}
