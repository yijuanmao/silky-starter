package com.silky.starter.mongodb.build;

import com.silky.starter.mongodb.support.SerializableFunction;
import com.silky.starter.mongodb.core.utils.ReflectionUtil;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * 排序条件构造器
 *
 * @author zy
 * @date 2022-11-22 17:49
 **/
public class SortBuilder {

    private final List<Sort.Order> orderList = new ArrayList<>(2);

    public SortBuilder() {
    }

    public SortBuilder(List<Sort.Order> orderList) {
        this.orderList.addAll(orderList);
    }

    /**
     * 构造排序条件
     *
     * @param column    排序字段
     * @param direction 排序方向
     * @param <E>       实体类
     * @param <R>       字段类型
     */
    public <E, R> SortBuilder(SerializableFunction<E, R> column, Sort.Direction direction) {
        Sort.Order order = new Sort.Order(direction, ReflectionUtil.getFieldName(column));
        orderList.add(order);
    }

    /**
     * 添加排序条件
     *
     * @param column    排序字段
     * @param direction 排序方向
     * @param <E>       实体类
     * @param <R>       字段类型
     * @return this
     */
    public <E, R> SortBuilder add(SerializableFunction<E, R> column, Sort.Direction direction) {
        Sort.Order order = new Sort.Order(direction, ReflectionUtil.getFieldName(column));
        orderList.add(order);
        return this;
    }

    public Sort toSort() {
        return Sort.by(orderList);
    }
}
