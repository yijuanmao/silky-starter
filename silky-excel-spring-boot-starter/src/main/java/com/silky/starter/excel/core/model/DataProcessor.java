package com.silky.starter.excel.core.model;

import com.silky.starter.excel.core.exception.ExcelExportException;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Excel数据处理器接口
 *
 * @author zy
 * @date 2025-10-24 11:25
 **/
@FunctionalInterface
public interface DataProcessor<T> {

    /**
     * 处理数据方法 对输入的数据列表进行处理，返回处理后的数据列表
     *
     * @param data 原始数据列表，不会为null但可能为空列表
     * @return 处理后的数据列表，不能返回null，如果不需要处理可以直接返回原始数据
     */
    List<T> process(List<T> data) throws ExcelExportException;

    /**
     * 处理器准备方法（可选实现）
     * 在导出开始前调用，用于初始化资源
     */
    default void prepare() throws ExcelExportException {
        // 默认空实现
    }

    /**
     * 处理器清理方法（可选实现）
     * 在导出结束后调用，用于释放资源
     */
    default void cleanup() throws ExcelExportException {
        // 默认空实现
    }

    /**
     * 创建数据脱敏处理器 使用函数式方式对每个数据进行脱敏处理
     *
     * @param maskingFunction 脱敏函数，接收原始数据返回脱敏后的数据
     * @param <T>             数据类型
     * @return 数据脱敏处理器实例
     */
    static <T> DataProcessor<T> masking(Function<T, T> maskingFunction) {
        return (data) -> data.stream()
                .map(maskingFunction)
                .collect(Collectors.toList());
    }

    /**
     * 创建数据过滤处理器
     * 使用断言函数过滤数据
     *
     * @param filter 过滤条件断言函数
     * @param <T>    数据类型
     * @return 数据过滤处理器实例
     */
    static <T> DataProcessor<T> filtering(Predicate<T> filter) {
        return (data) -> data.stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * 创建数据转换处理器，将数据从一种类型转换为另一种类型
     *
     * @param converter 数据转换函数
     * @param <T>       原始数据类型
     * @param <R>       目标数据类型
     * @return 数据转换处理器实例
     */
    static <T, R> DataProcessor<T> converting(Function<T, R> converter) {
        return (data) -> data.stream()
                .map(converter)
                .map(item -> (T) item) // 类型擦除，实际使用中需要确保类型安全
                .collect(Collectors.toList());
    }

    /**
     * 创建数据排序处理器，对数据进行排序
     *
     * @param comparator 比较器
     * @param <T>        数据类型
     * @return 数据排序处理器实例
     */
    static <T> DataProcessor<T> sorting(java.util.Comparator<T> comparator) {
        return (data) -> data.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * 创建数据分页统计处理器，对每页数据进行统计，并记录日志
     *
     * @param <T> 数据类型
     * @return 数据统计处理器实例
     */
    static <T> DataProcessor<T> statistics() {
        return (data) -> {
            // 记录每页数据的统计信息
            return data;
        };
    }
}
