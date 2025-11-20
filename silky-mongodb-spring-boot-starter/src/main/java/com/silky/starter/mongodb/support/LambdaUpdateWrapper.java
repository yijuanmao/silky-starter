package com.silky.starter.mongodb.support;

import cn.hutool.core.lang.func.LambdaUtil;
import org.springframework.data.mongodb.core.query.Update;

/**
 * lambda更新条件封装
 *
 * @author: zy
 * @date: 2025-11-19
 */
public class LambdaUpdateWrapper<T> {


    private final Update update = new Update();

    /**
     * 设置字段
     *
     * @param column 列
     * @param value  值
     */
    public LambdaUpdateWrapper<T> set(SFunction<T, ?> column, Object value) {
        if (value != null) {
            update.set(LambdaUtil.getFieldName(column), value);
        }
        return this;
    }

    /**
     * 自增
     *
     * @param column 列
     * @param value  值
     */
    public LambdaUpdateWrapper<T> inc(SFunction<T, ?> column, Number value) {
        if (value != null) {
            update.inc(LambdaUtil.getFieldName(column), value);
        }
        return this;
    }

    /**
     * 构建更新条件
     *
     * @return 更新条件
     */
    public Update build() {
        return update;
    }
}
