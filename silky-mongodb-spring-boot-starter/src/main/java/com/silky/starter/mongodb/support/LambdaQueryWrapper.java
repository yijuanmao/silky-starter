package com.silky.starter.mongodb.support;

import cn.hutool.core.lang.func.LambdaUtil;
import lombok.Getter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Collection;

/**
 * lambda查询条件封装
 *
 * @author: zy
 * @date: 2025-11-19
 */
public class LambdaQueryWrapper<T> {

    private final Query query = new Query();

    @Getter
    private final Class<T> entityClass;

    public LambdaQueryWrapper() {
        this(null);
    }

    public LambdaQueryWrapper(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * 等于
     *
     * @param column 列
     * @param value  值
     */
    public LambdaQueryWrapper<T> eq(SFunction<T, ?> column, Object value) {
        if (value != null) {
            query.addCriteria(Criteria.where(LambdaUtil.getFieldName(column)).is(value));
        }
        return this;
    }

    /**
     * 不等于
     *
     * @param column 列
     * @param value  值
     */
    public LambdaQueryWrapper<T> ne(SFunction<T, ?> column, Object value) {
        if (value != null) {
            query.addCriteria(Criteria.where(LambdaUtil.getFieldName(column)).ne(value));
        }
        return this;
    }

    /**
     * 模糊查询
     *
     * @param column 列
     * @param value  值
     */
    public LambdaQueryWrapper<T> like(SFunction<T, ?> column, String value) {
        if (value != null) {
            query.addCriteria(Criteria.where(LambdaUtil.getFieldName(column)).regex(value));
        }
        return this;
    }

    /**
     * 模糊查询
     *
     * @param column 列
     * @param values 值
     */
    public LambdaQueryWrapper<T> in(SFunction<T, ?> column, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            query.addCriteria(Criteria.where(LambdaUtil.getFieldName(column)).in(values));
        }
        return this;
    }

    /**
     * 大于
     *
     * @param column 列
     * @param value  值
     */
    public LambdaQueryWrapper<T> gt(SFunction<T, ?> column, Object value) {
        if (value != null) {
            query.addCriteria(Criteria.where(LambdaUtil.getFieldName(column)).gt(value));
        }
        return this;
    }

    /**
     * 小于
     *
     * @param column 列
     * @param value  值
     */
    public LambdaQueryWrapper<T> lt(SFunction<T, ?> column, Object value) {
        if (value != null) {
            query.addCriteria(Criteria.where(LambdaUtil.getFieldName(column)).lt(value));
        }
        return this;
    }


    /**
     * 排序 Asc
     *
     * @param column 列
     */
    public LambdaQueryWrapper<T> orderByAsc(SFunction<T, ?> column) {
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC,
                LambdaUtil.getFieldName(column)
        ));
        return this;
    }

    /**
     * 排序 Desc
     *
     * @param column 列
     */
    public LambdaQueryWrapper<T> orderByDesc(SFunction<T, ?> column) {
        query.with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC,
                LambdaUtil.getFieldName(column)
        ));
        return this;
    }

    /**
     * 构建查询条件
     *
     * @return 查询条件
     */
    public Query build() {
        return query;
    }
}
