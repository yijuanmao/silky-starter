package com.silky.starter.mongodb.wrapper.query;

import com.silky.starter.mongodb.support.SerializableFunction;
import com.silky.starter.mongodb.core.utils.ReflectionUtil;
import com.silky.starter.mongodb.wrapper.CriteriaWrapper;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Collection;

/**
 * 查询语句生成器 AND连接
 *
 * @author zy
 * @date 2025-09-04 11:13
 **/
public class CriteriaAndWrapper extends CriteriaWrapper {

    public CriteriaAndWrapper() {
        andLink = true;
    }

    public CriteriaAndWrapper and(Criteria criteria) {
        criteriaChains.add(criteria);
        return this;
    }

    public CriteriaAndWrapper and(CriteriaWrapper criteriaWrapper) {
        criteriaChains.add(criteriaWrapper.build());
        return this;
    }

    /**
     * 等于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper eq(SerializableFunction<E, R> column, Object values) {
        super.eq(column, values);
        return this;
    }

    /**
     * 不等于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper ne(SerializableFunction<E, R> column, Object values) {
        super.ne(column, values);
        return this;
    }

    /**
     * 小于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper lt(SerializableFunction<E, R> column, Object values) {
        super.lt(column, values);
        return this;
    }

    /**
     * 小于或等于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper lte(SerializableFunction<E, R> column, Object values) {
        super.lte(column, values);
        return this;
    }

    /**
     * 大于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper gt(SerializableFunction<E, R> column, Object values) {
        super.gt(column, values);
        return this;
    }

    /**
     * 大于或等于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper gte(SerializableFunction<E, R> column, Object values) {
        super.gte(column, values);
        return this;
    }

    /**
     * 区间
     *
     * @param column 字段
     * @param value1 值1
     * @param value2 值2
     * @return CriteriaWrapper
     */
    @Override
    public <E, R> CriteriaWrapper between(SerializableFunction<E, R> column, Object value1, Object value2) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).gte(value1).lte(value2));
//        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).lte(value2));
        return this;
    }

    /**
     * 包含
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper contain(SerializableFunction<E, R> column, Object values) {
        super.contain(column, values);
        return this;
    }

    /**
     * 包含,以或连接
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper containOr(SerializableFunction<E, R> column, Collection<?> values) {
        super.containOr(column, values);
        return this;
    }

    /**
     * 包含,以或连接
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper containOr(SerializableFunction<E, R> column, Object[] values) {
        super.containOr(column, values);
        return this;
    }

    /**
     * 包含,以且连接
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper containAnd(SerializableFunction<E, R> column, Collection<?> values) {
        super.containAnd(column, values);
        return this;
    }

    /**
     * 包含,以且连接
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper containAnd(SerializableFunction<E, R> column, Object[] values) {
        super.containAnd(column, values);
        return this;
    }

    /**
     * 相似于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper like(SerializableFunction<E, R> column, String values) {
        super.like(column, values);
        return this;
    }


    /**
     * 在其中
     *
     * @param column 字段
     * @param values 参数
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper in(SerializableFunction<E, R> column, Collection<?> values) {
        super.in(column, values);
        return this;
    }

    /**
     * 在其中
     *
     * @param column 字段
     * @param values 参数
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper in(SerializableFunction<E, R> column, Object[] values) {
        super.in(column, values);
        return this;
    }

    /**
     * 不在其中
     *
     * @param column 字段
     * @param values 参数
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper nin(SerializableFunction<E, R> column, Collection<?> values) {
        super.nin(column, values);
        return this;
    }

    /**
     * 不在其中
     *
     * @param column 字段
     * @param values 参数
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper nin(SerializableFunction<E, R> column, Object[] values) {
        super.nin(column, values);
        return this;
    }

    /**
     * 为空
     *
     * @param column 字段
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper isNull(SerializableFunction<E, R> column) {
        super.isNull(column);
        return this;
    }

    /**
     * 不为空
     *
     * @param column 字段
     * @return CriteriaAndWrapper
     */
    @Override
    public <E, R> CriteriaAndWrapper isNotNull(SerializableFunction<E, R> column) {
        super.isNotNull(column);
        return this;
    }

    /**
     * 数组查询
     *
     * @param arr    数组名
     * @param column 字段名
     * @param values 字段值
     * @return
     */
    public <E, R> CriteriaAndWrapper findArray(SerializableFunction<E, R> arr, SerializableFunction<E, R> column, Object values) {
        super.findArray(ReflectionUtil.getFieldName(arr), column, values);
        return this;
    }

    /**
     * 数组模糊查询
     *
     * @param arr    数组名
     * @param column 字段名
     * @param values 字段值
     * @return
     */
    public <E, R> CriteriaAndWrapper findArrayLike(SerializableFunction<E, R> arr, SerializableFunction<E, R> column, String values) {
        super.findArrayLike(ReflectionUtil.getFieldName(arr), column, values);
        return this;
    }
}
