package com.silky.starter.mongodb.template;

import com.silky.starter.mongodb.entity.PageResult;
import com.silky.starter.mongodb.support.LambdaQueryWrapper;
import com.silky.starter.mongodb.support.LambdaUpdateWrapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Collection;
import java.util.List;

/**
 * Mongo模板接口
 *
 * @author: zy
 * @date: 2025-11-19
 */
public interface SilkyMongoTemplate {

    /**
     * 获取MongoTemplate
     *
     * @param dataSourceName 数据源名称
     * @param readOnly       是否只读
     * @return MongoTemplate
     */
    MongoTemplate getMongoTemplate(String dataSourceName, boolean readOnly);

    /**
     * 切换数据源
     *
     * @param dataSourceName 数据源名称
     */
    void switchDataSource(String dataSourceName);

    /**
     * 清空数据源
     */
    void clearDataSource();

    /**
     * 获取当前数据源名称
     *
     * @return 数据源名称
     */
    String getCurrentDataSource();

    /**
     * 根据id查询
     *
     * @param id          id
     * @param entityClass 实体类
     * @param <T>         实体类
     * @return 实体类
     */
    <T> T getById(String id, Class<T> entityClass);

    /**
     * 查询所有
     *
     * @param entityClass 实体类
     * @param <T>         实体类
     * @return 实体类列表
     */
    <T> List<T> list(Class<T> entityClass);


    /**
     * 根据条件查询
     *
     * @param wrapper     条件
     * @param entityClass 实体类
     * @param <T>         实体类
     * @return 列表
     */
    <T> List<T> list(LambdaQueryWrapper<T> wrapper, Class<T> entityClass);

    /**
     * 根据条件查询
     *
     * @param wrapper     封装条件
     * @param entityClass 封装类
     * @param <T>         封装类
     * @return 封装类
     */
    <T> T getOne(LambdaQueryWrapper<T> wrapper, Class<T> entityClass);

    /**
     * 统计数量
     *
     * @param entityClass 封装类
     * @param <T>         封装类
     * @return 数量
     */
    <T> long count(Class<T> entityClass);

    /**
     * 统计数量
     *
     * @param wrapper     封装条件
     * @param entityClass 封装类
     * @param <T>         封装类
     * @return 数量
     */
    <T> long count(LambdaQueryWrapper<T> wrapper, Class<T> entityClass);

    /**
     * 判断是否存在
     *
     * @param wrapper     封装条件
     * @param entityClass 封装类
     * @param <T>         封装类
     * @return 存在
     */
    <T> boolean exists(LambdaQueryWrapper<T> wrapper, Class<T> entityClass);

    /**
     * 分页查询
     *
     * @param current     当前页
     * @param size        每页数量
     * @param entityClass 封装类
     * @param <T>
     * @return 分页结果
     */
    <T> PageResult<T> page(long current, long size, Class<T> entityClass);

    /**
     * 分页查询
     *
     * @param pageNum     当前页
     * @param size        每页数量
     * @param query       查询条件
     * @param entityClass 封装类
     * @param <T>
     * @return 分页结果
     */
    <T> PageResult<T> page(long pageNum, long size, Query query, Class<T> entityClass);

    /**
     * 分页查询
     *
     * @param pageNum     当前页
     * @param size        每页数量
     * @param wrapper     封装条件
     * @param entityClass 封装类
     * @param <T>
     * @return 分页结果
     */
    <T> PageResult<T> page(long pageNum, long size, LambdaQueryWrapper<T> wrapper, Class<T> entityClass);

    /**
     * 保存
     *
     * @param entity 封装类
     * @param <T>
     * @return 是否保存成功
     */
    <T> T save(T entity);

    /**
     * 批量保存
     *
     * @param entityList 封装类列表
     * @param <T>
     * @return 是否保存成功
     */
    <T> List<T> saveBatch(Collection<T> entityList);

    /**
     * 批量保存
     *
     * @param entityList  封装类列表
     * @param entityClass 封装类
     * @param <T>
     * @return 是否保存成功
     */
    <T> List<T> saveBatch(Collection<T> entityList, Class<T> entityClass);

    /**
     * 根据id更新
     *
     * @param id     id
     * @param entity 封装类
     * @param <T>
     * @return 是否更新成功
     */
    <T> boolean updateById(String id, T entity);

    /**
     * 根据条件更新
     *
     * @param queryWrapper  查询条件
     * @param updateWrapper 更新条件
     * @param entityClass   封装类
     * @param <T>           * @return 是否更新成功
     *
     */
    <T> boolean update(LambdaQueryWrapper<T> queryWrapper,
                       LambdaUpdateWrapper<T> updateWrapper, Class<T> entityClass);

    /**
     * 批量更新
     *
     * @param query       查询条件
     * @param update      更新条件
     * @param entityClass 封装类
     * @param <T>         * @return 是否更新成功
     *
     */
    <T> long updateMulti(Query query, Update update, Class<T> entityClass);

    /**
     * 根据id删除
     *
     * @param id          id
     * @param entityClass 封装类
     * @param <T>
     * @return 是否删除成功
     */
    <T> boolean removeById(String id, Class<T> entityClass);

    /**
     * 根据条件删除
     *
     * @param wrapper     封装条件
     * @param entityClass 封装类
     * @param <T>
     * @return 是否删除成功
     */
    <T> boolean remove(LambdaQueryWrapper<T> wrapper, Class<T> entityClass);

    /**
     * 批量删除
     *
     * @param ids         id列表
     * @param entityClass 封装类
     * @param <T>
     * @return 是否删除成功
     */
    <T> long removeBatch(Collection<String> ids, Class<T> entityClass);

    /**
     * 根据id查询
     *
     * @param ids         ids
     * @param entityClass 封装类
     * @param <T>
     * @return 封装类
     */
    <T> List<T> findByIds(Collection<String> ids, Class<T> entityClass);

    /**
     * 根据id查询是否存在
     *
     * @param id          id
     * @param entityClass 封装类
     * @param <T>
     * @return 结果
     */
    <T> boolean existsById(String id, Class<T> entityClass);

    /**
     * 根据条件查询并更新
     *
     * @param query       查询条件
     * @param update      更新条件
     * @param entityClass 封装类
     * @param <T>
     * @return 封装类
     */
    <T> T findAndModify(LambdaQueryWrapper<T> query, LambdaUpdateWrapper<T> update, Class<T> entityClass);

    /**
     * 创建集合
     *
     * @param entityClass 封装类
     * @param <T>
     */
    <T> void createCollection(Class<T> entityClass);

    /**
     * 删除集合
     *
     * @param entityClass 封装类
     * @param <T>
     */
    <T> void dropCollection(Class<T> entityClass);
}
