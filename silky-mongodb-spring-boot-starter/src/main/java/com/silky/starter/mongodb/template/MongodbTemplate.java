package com.silky.starter.mongodb.template;

import com.mongodb.client.result.DeleteResult;
import com.silky.starter.mongodb.build.SortBuilder;
import com.silky.starter.mongodb.model.page.Page;
import com.silky.starter.mongodb.support.SerializableFunction;
import com.silky.starter.mongodb.wrapper.CriteriaWrapper;

import java.util.Collection;
import java.util.List;

/**
 * mongodb操作模板
 *
 * @author zy
 * @date 2025-09-04 16:00
 **/
public interface MongodbTemplate {

    /**
     * 分页查询
     *
     * @param page  分页
     * @param clazz 实体类
     * @param <T>   泛型
     * @return 分页结果
     */
    <T> Page<T> findPage(Page<?> page, Class<T> clazz);

    /**
     * 条件分页查询
     *
     * @param criteriaWrapper 条件构造器
     * @param page            分页
     * @param clazz           实体类
     * @param <T>             泛型
     * @return 分页结果
     */
    <T> Page<T> findPage(CriteriaWrapper criteriaWrapper, Page<?> page, Class<T> clazz);

    /**
     * 条件分页查询
     *
     * @param criteriaWrapper 条件构造
     * @param sort            排序
     * @param page            分页
     * @param clazz           实体类
     * @param <T>             泛型
     * @return 分页结果
     */
    <T> Page<T> findPage(CriteriaWrapper criteriaWrapper, SortBuilder sort, Page<?> page, Class<T> clazz);

    /**
     * 根据ID查询
     *
     * @param id    ID
     * @param clazz 实体类
     * @param <T>   泛型
     * @return 实体类
     */
    <T> T findById(String id, Class<T> clazz);

    /**
     * 根据条件查询单个
     *
     * @param criteriaWrapper 条件构造
     * @param clazz           实体类
     * @param <T>             泛型
     * @return 实体类
     */
    <T> T findOneByQuery(CriteriaWrapper criteriaWrapper, Class<T> clazz);

    /**
     * 根据条件和排序查询单个
     *
     * @param criteriaWrapper 条件构造
     * @param sortBuilder     排序
     * @param clazz           实体类
     * @param <T>             泛型
     * @return 实体类
     */
    <T> T findOneByQuery(CriteriaWrapper criteriaWrapper, SortBuilder sortBuilder, Class<T> clazz);

    /**
     * 根据条件查询列表
     *
     * @param criteria 条件构造
     * @param clazz    实体类
     * @param <T>      泛型
     * @return 实体类列表
     */
    <T> List<T> findListByQuery(CriteriaWrapper criteria, Class<T> clazz);

    /**
     * 根据条件查询列表
     *
     * @param criteria 条件构造
     * @param sort     排序
     * @param clazz    实体类
     * @param <T>      泛型
     * @return 实体类列表
     */
    <T> List<T> findListByQuery(CriteriaWrapper criteria, SortBuilder sort, Class<T> clazz);

    /**
     * 根据条件查找某个属性
     *
     * @param <T>           类型
     * @param criteria      查询
     * @param documentClass 类
     * @param property      属性
     * @param propertyClass 属性类
     * @return List 列表
     */
    <T, R, E> List<T> findPropertiesByQuery(CriteriaWrapper criteria, Class<?> documentClass, SerializableFunction<E, R> property, Class<T> propertyClass);

    /**
     * 根据id集合查找某个属性
     *
     * @param ids      id列表
     * @param clazz    类
     * @param property 属性
     * @return List 列表
     */
    <R, E> List<String> findPropertiesByIds(List<String> ids, Class<?> clazz, SerializableFunction<E, R> property);

    /**
     * 根据条件查找某个属性
     *
     * @param criteria      查询
     * @param documentClass 类
     * @param property      属性
     * @return List 列表
     */
    <R, E> List<String> findPropertiesByQuery(CriteriaWrapper criteria, Class<?> documentClass, SerializableFunction<E, R> property);

    /**
     * 根据条件查询ID列表
     *
     * @param criteria 条件构造
     * @param clazz    实体类
     * @return ID列表
     */
    List<String> findIdsByQuery(CriteriaWrapper criteria, Class<?> clazz);

    /**
     * 根据ID列表查询
     *
     * @param ids   ID列表
     * @param clazz 实体类
     * @param <T>   泛型
     * @return 实体类列表
     */
    <T> List<T> findListByIds(Collection<String> ids, Class<T> clazz);

    /**
     * 根据ID列表查询
     *
     * @param ids         ID列表
     * @param sortBuilder 排序
     * @param clazz       实体类
     * @param <T>         泛型
     */
    <T> List<T> findListByIds(Collection<String> ids, SortBuilder sortBuilder, Class<T> clazz);

    /**
     * 根据条件查询数量
     *
     * @param criteriaWrapper 条件构造
     * @param clazz           实体类
     * @return 数量
     */
    Long countByQuery(CriteriaWrapper criteriaWrapper, Class<?> clazz);

    /**
     * 插入
     *
     * @param object 实体类
     * @return ID
     */
    String insert(Object object);

    /**
     * 批量插入
     *
     * @param list 实体类列表
     * @param <T>  泛型
     * @return ID
     */
    <T> List<String> batchInsert(List<T> list);

    /**
     * 根据ID删除
     *
     * @param object 实体类
     * @return ID
     */
    String updateById(Object object);

    /**
     * 插入或更新
     *
     * @param object 保存对象
     * @return String 对象id
     */
    String insertOrUpdate(Object object);

    /**
     * 根据id删除
     *
     * @param id    对象
     * @param clazz 类
     * @return DeleteResult 删除结果
     */
    DeleteResult deleteById(String id, Class<?> clazz);

    /**
     * 根据id删除
     *
     * @param ids   对象
     * @param clazz 类
     * @return DeleteResult 删除结果
     */
    DeleteResult deleteByIds(List<String> ids, Class<?> clazz);

    /**
     * 根据条件删除
     *
     * @param criteriaWrapper 查询
     * @param clazz           类
     * @return DeleteResult 删除结果
     */
    DeleteResult deleteByQuery(CriteriaWrapper criteriaWrapper, Class<?> clazz);
}
