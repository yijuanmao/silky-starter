package com.silky.starter.mongodb.wrapper.query;

import com.silky.starter.mongodb.support.SerializableFunction;
import com.silky.starter.mongodb.core.utils.ReflectionUtil;
import com.silky.starter.mongodb.wrapper.CriteriaWrapper;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Collection;

/**
 * 查询语句生成器 OR连接
 *
 * @author zy
 * @date 2022-11-22 11:07
 **/
public class CriteriaOrWrapper extends CriteriaWrapper {

    public CriteriaOrWrapper() {
        andLink = false;
    }

    /**
     * 或连接
     *
     * @param criteria 条件构造器
     * @return CriteriaOrWrapper
     */
    public CriteriaOrWrapper or(Criteria criteria) {
        criteriaChains.add(criteria);
        return this;
    }

    /**
     * 或连接
     *
     * @param criteriaWrapper 条件构造器
     * @return CriteriaOrWrapper
     */
    public CriteriaOrWrapper or(CriteriaWrapper criteriaWrapper) {
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
    public <E, R> CriteriaOrWrapper eq(SerializableFunction<E, R> column, Object values) {
        super.eq(column, values);
        return this;
    }

    /**
     * 不等于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper ne(SerializableFunction<E, R> column, Object values) {
        super.ne(column, values);
        return this;
    }

    /**
     * 小于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper lt(SerializableFunction<E, R> column, Object values) {
        super.lt(column, values);
        return this;
    }

    /**
     * 小于或等于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper lte(SerializableFunction<E, R> column, Object values) {
        super.lte(column, values);
        return this;
    }

    /**
     * 大于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper gt(SerializableFunction<E, R> column, Object values) {
        super.gt(column, values);
        return this;
    }

    /**
     * 大于或等于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper gte(SerializableFunction<E, R> column, Object values) {
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
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).gte(value1));
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).lte(value2));
        return this;
    }

    /**
     * 包含
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper contain(SerializableFunction<E, R> column, Object values) {
        super.contain(column, values);
        return this;
    }

    /**
     * 包含,以或连接
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper containOr(SerializableFunction<E, R> column, Collection<?> values) {
        super.containOr(column, values);
        return this;
    }

    /**
     * 包含,以或连接
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper containOr(SerializableFunction<E, R> column, Object[] values) {
        super.containOr(column, values);
        return this;
    }

    /**
     * 包含,以且连接
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper containAnd(SerializableFunction<E, R> column, Collection<?> values) {
        super.containAnd(column, values);
        return this;
    }

    /**
     * 包含,以且连接
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper containAnd(SerializableFunction<E, R> column, Object[] values) {
        super.containAnd(column, values);
        return this;
    }

    /**
     * 相似于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper like(SerializableFunction<E, R> column, String values) {
        super.like(column, values);
        return this;
    }

    /**
     * 在其中
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper in(SerializableFunction<E, R> column, Collection<?> values) {
        super.in(column, values);
        return this;
    }

    /**
     * 在其中
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper in(SerializableFunction<E, R> column, Object[] values) {
        super.in(column, values);
        return this;
    }

    /**
     * 不在其中
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper nin(SerializableFunction<E, R> column, Collection<?> values) {
        super.nin(column, values);
        return this;
    }

    /**
     * 不在其中
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper nin(SerializableFunction<E, R> column, Object[] values) {
        super.nin(column, values);
        return this;
    }

    /**
     * 为空
     *
     * @param column 字段
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper isNull(SerializableFunction<E, R> column) {
        super.isNull(column);
        return this;
    }

    /**
     * 不为空
     *
     * @param column 字段
     * @return CriteriaOrWrapper
     */
    @Override
    public <E, R> CriteriaOrWrapper isNotNull(SerializableFunction<E, R> column) {
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
    public <E, R> CriteriaOrWrapper findArray(SerializableFunction<E, R> arr, SerializableFunction<E, R> column, Object values) {
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
    public <E, R> CriteriaOrWrapper findArrayLike(SerializableFunction<E, R> arr, SerializableFunction<E, R> column, String values) {
        super.findArrayLike(ReflectionUtil.getFieldName(arr), column, values);
        return this;
    }
}
