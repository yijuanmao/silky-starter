package com.silky.starter.mongodb.wrapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.silky.starter.mongodb.support.SerializableFunction;
import com.silky.starter.mongodb.utils.ReflectionUtil;
import com.silky.starter.mongodb.wrapper.query.CriteriaOrWrapper;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 查询语句条件生成器
 *
 * @author zy
 * @date 2025-09-04 10:40
 **/
public abstract class CriteriaWrapper {

    /**
     * 是否and操作
     */
    protected boolean andLink = true;

    /**
     * 多个Criteria集合
     */
    protected List<Criteria> criteriaChains = new ArrayList<>(4);

    /**
     * 将Wrapper转化为Criteria
     *
     * @return Criteria
     */
    public Criteria build() {
        Criteria criteria = new Criteria();
        if (CollUtil.isNotEmpty(criteriaChains)) {
            if (andLink) {
                criteria.andOperator(listToArray(criteriaChains));
            } else {
                criteria.orOperator(listToArray(criteriaChains));
            }
        }
        return criteria;
    }

    /**
     * 等于
     *
     * @param column 字段
     * @param values 参数
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper eq(SerializableFunction<E, R> column, Object values) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).is(values));
        return this;
    }

    /**
     * 不等于
     *
     * @param column 字段
     * @param values 值
     * @return AbstractCriteriaWrapper
     */
    protected <E, R> CriteriaWrapper ne(SerializableFunction<E, R> column, Object values) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).ne(values));
        return this;
    }

    /**
     * 小于
     *
     * @param column 字段
     * @param values 值
     * @return AbstractCriteriaWrapper
     */
    protected <E, R> CriteriaWrapper lt(SerializableFunction<E, R> column, Object values) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).lt(values));
        return this;
    }

    /**
     * 小于或等于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper lte(SerializableFunction<E, R> column, Object values) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).lte(values));
        return this;
    }

    /**
     * 大于
     *
     * @param column 字段
     * @param values 值
     * @return AbstractCriteriaWrapper
     */
    protected <E, R> CriteriaWrapper gt(SerializableFunction<E, R> column, Object values) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).gt(values));
        return this;
    }

    /**
     * 大于或等于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper gte(SerializableFunction<E, R> column, Object values) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).gte(values));
        return this;
    }

    /**
     * 范围查询
     *
     * @param column 字段
     * @param value1 值1
     * @param value2 值2
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper between(SerializableFunction<E, R> column, Object value1, Object value2) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).gte(value1).lte(value2));
//        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).lte(value2));
        return this;
    }

    /**
     * 包含
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper contain(SerializableFunction<E, R> column, Object values) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).all(values));
        return this;
    }

    /**
     * 包含,以或连接
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper containOr(SerializableFunction<E, R> column, Object[] values) {
        return containOr(column, Arrays.asList(values));
    }

    /**
     * 包含,以或连接
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper containOr(SerializableFunction<E, R> column, Collection<?> values) {
        CriteriaOrWrapper criteriaOrWrapper = new CriteriaOrWrapper();
        for (Object object : values) {
            criteriaOrWrapper.contain(column, object);
        }
        criteriaChains.add(criteriaOrWrapper.build());
        return this;
    }

    /**
     * 包含,以且连接
     *
     * @param column 字段
     * @param values 值
     * @return AbstractCriteriaWrapper
     */
    protected <E, R> CriteriaWrapper containAnd(SerializableFunction<E, R> column, Collection<?> values) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).all(values));
        return this;
    }

    /**
     * 包含,以且连接
     *
     * @param column 字段
     * @param values 值
     * @return AbstractCriteriaWrapper
     */
    protected <E, R> CriteriaWrapper containAnd(SerializableFunction<E, R> column, Object[] values) {
        return containAnd(column, Arrays.asList(values));
    }

    /**
     * 相似于
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper like(SerializableFunction<E, R> column, String values) {
        Pattern pattern = Pattern.compile("^.*" + replaceRegExp(values) + ".*$", Pattern.CASE_INSENSITIVE);
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).regex(pattern));
        return this;
    }

    /**
     * 在其中
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper in(SerializableFunction<E, R> column, Collection<?> values) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).in(values));
        return this;
    }

    /**
     * 在其中
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper in(SerializableFunction<E, R> column, Object[] values) {
        return in(column, Arrays.asList(values));
    }

    /**
     * 不在其中
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper nin(SerializableFunction<E, R> column, Collection<?> values) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).nin(values));
        return this;
    }

    /**
     * 不在其中
     *
     * @param column 字段
     * @param values 值
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper nin(SerializableFunction<E, R> column, Object[] values) {
        return nin(column, Arrays.asList(values));
    }

    /**
     * 为空
     *
     * @param column 字段
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper isNull(SerializableFunction<E, R> column) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).is(null));
        return this;
    }

    /**
     * 不为空
     *
     * @param column 字段
     * @return CriteriaWrapper
     */
    protected <E, R> CriteriaWrapper isNotNull(SerializableFunction<E, R> column) {
        criteriaChains.add(Criteria.where(ReflectionUtil.getFieldName(column)).ne(null));
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
    protected <E, R> CriteriaWrapper findArray(String arr, SerializableFunction<E, R> column, Object values) {
        criteriaChains.add(Criteria.where(arr).elemMatch(Criteria.where(ReflectionUtil.getFieldName(column)).is(values)));
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
    protected <E, R> CriteriaWrapper findArrayLike(String arr, SerializableFunction<E, R> column, String values) {
        Pattern pattern = Pattern.compile("^.*" + replaceRegExp(values) + ".*$", Pattern.CASE_INSENSITIVE);
        criteriaChains.add(Criteria.where(arr).elemMatch(Criteria.where(ReflectionUtil.getFieldName(column)).regex(pattern)));
        return this;
    }

    /**
     * 转换成数组
     *
     * @param list 条件
     * @return
     */
    private Criteria[] listToArray(List<Criteria> list) {
        return list.toArray(new Criteria[list.size()]);
    }

    /**
     * 转义正则特殊字符 （$()*+.[]?\^{} \\需要第一个替换，否则replace方法替换时会有逻辑bug
     *
     * @param str 转译字符串
     * @return String
     */
    private String replaceRegExp(String str) {
        if (StrUtil.isEmpty(str)) {
            return str;
        }
        return str.replace("\\", "\\\\").replace("*", "\\*")
                .replace("+", "\\+").replace("|", "\\|")
                .replace("{", "\\{").replace("}", "\\}")
                .replace("(", "\\(").replace(")", "\\)")
                .replace("^", "\\^").replace("$", "\\$")
                .replace("[", "\\[").replace("]", "\\]")
                .replace("?", "\\?").replace(",", "\\,")
                .replace(".", "\\.").replace("&", "\\&");
    }
}
