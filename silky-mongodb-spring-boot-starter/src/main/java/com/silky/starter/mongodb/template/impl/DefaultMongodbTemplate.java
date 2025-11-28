
package com.silky.starter.mongodb.template.impl;

import cn.hutool.core.util.ReflectUtil;
import com.silky.starter.mongodb.annotation.ReadOnly;
import com.silky.starter.mongodb.template.DynamicMongoTemplate;
import com.silky.starter.mongodb.core.constant.MongodbConstant;
import com.silky.starter.mongodb.entity.PageResult;
import com.silky.starter.mongodb.support.LambdaQueryWrapper;
import com.silky.starter.mongodb.support.LambdaUpdateWrapper;
import com.silky.starter.mongodb.template.SilkyMongoTemplate;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 默认MongoDB模板实现
 *
 * @author: zy
 * @date: 2025-11-19
 */
public class DefaultMongodbTemplate implements SilkyMongoTemplate {


    private final DynamicMongoTemplate dynamicMongoTemplate;

    public DefaultMongodbTemplate(DynamicMongoTemplate dynamicMongoTemplate) {
        this.dynamicMongoTemplate = dynamicMongoTemplate;
    }

    /**
     * 获取MongoTemplate
     *
     * @param dataSourceName 数据源名称
     * @param readOnly       是否只读
     * @return MongoTemplate
     */
    @Override
    public MongoTemplate getMongoTemplate(String dataSourceName, boolean readOnly) {
        return dynamicMongoTemplate.getTargetTemplate(readOnly, dataSourceName);
    }

    /**
     * 切换数据源
     *
     * @param dataSourceName 数据源名称
     */
    @Override
    public void switchDataSource(String dataSourceName) {
        dynamicMongoTemplate.setDataSource(dataSourceName);
    }

    /**
     * 清空数据源
     */
    @Override
    public void clearDataSource() {
        dynamicMongoTemplate.clearDataSource();
    }

    /**
     * 获取当前数据源名称
     *
     * @return 数据源名称
     */
    @Override
    public String getCurrentDataSource() {
        return dynamicMongoTemplate.getDataSource();
    }

    /**
     * 根据id查询
     *
     * @param id          id
     * @param entityClass 实体类
     * @return 实体类
     */
    @ReadOnly
    @Override
    public <T> T getById(String id, Class<T> entityClass) {
        return getMongoTemplate(true).findById(new ObjectId(id), entityClass);
    }

    /**
     * 查询所有
     *
     * @param entityClass 实体类
     * @return 实体类列表
     */
    @ReadOnly
    @Override
    public <T> List<T> list(Class<T> entityClass) {
        return getMongoTemplate(true).findAll(entityClass);
    }

    /**
     * 根据条件查询
     *
     * @param wrapper     条件
     * @param entityClass 实体类
     * @return 列表
     */
    @ReadOnly
    @Override
    public <T> List<T> list(LambdaQueryWrapper<T> wrapper, Class<T> entityClass) {
        return getMongoTemplate(true).find(wrapper.build(), entityClass);
    }

    /**
     * 根据条件查询
     *
     * @param wrapper     封装条件
     * @param entityClass 封装类
     * @return 封装类
     */
    @ReadOnly
    @Override
    public <T> T getOne(LambdaQueryWrapper<T> wrapper, Class<T> entityClass) {
        return getMongoTemplate(true).findOne(wrapper.build(), entityClass);
    }

    /**
     * 统计数量
     *
     * @param entityClass 封装类
     * @return 数量
     */
    @ReadOnly
    @Override
    public <T> long count(Class<T> entityClass) {
        return getMongoTemplate(true).count(new Query(), entityClass);
    }

    /**
     * 统计数量
     *
     * @param wrapper     封装条件
     * @param entityClass 封装类
     * @return 数量
     */
    @ReadOnly
    @Override
    public <T> long count(LambdaQueryWrapper<T> wrapper, Class<T> entityClass) {
        return getMongoTemplate(true).count(wrapper.build(), entityClass);
    }

    /**
     * 判断是否存在
     *
     * @param wrapper     封装条件
     * @param entityClass 封装类
     * @return 存在
     */
    @ReadOnly
    @Override
    public <T> boolean exists(LambdaQueryWrapper<T> wrapper, Class<T> entityClass) {
        return getMongoTemplate(true).exists(wrapper.build(), entityClass);
    }

    /**
     * 根据id查询是否存在
     *
     * @param id          id
     * @param entityClass 封装类
     * @return 结果
     */
    @Override
    public <T> boolean existsById(String id, Class<T> entityClass) {
        return getMongoTemplate(false).exists(Query.query(Criteria.where(MongodbConstant.ID_FIELD).is(id)), entityClass);
    }

    /**
     * 分页查询
     *
     * @param current     当前页
     * @param size        每页数量
     * @param entityClass 封装类
     * @return 分页结果
     */
    @ReadOnly
    @Override
    public <T> PageResult<T> page(long current, long size, Class<T> entityClass) {
        return page(current, size, new Query(), entityClass);
    }

    /**
     * 分页查询
     *
     * @param pageNum     当前页
     * @param size        每页数量
     * @param query       查询条件
     * @param entityClass 封装类
     * @return 分页结果
     */
    @ReadOnly
    @Override
    public <T> PageResult<T> page(long pageNum, long size, Query query, Class<T> entityClass) {
        long total = getMongoTemplate(true).count(query, entityClass);

        query.skip((pageNum - 1) * size).limit((int) size);
        List<T> records = getMongoTemplate(true).find(query, entityClass);

        return new PageResult<>(pageNum, size, total, records);
    }

    /**
     * 分页查询
     *
     * @param pageNum     当前页
     * @param size        每页数量
     * @param wrapper     封装条件
     * @param entityClass 封装类
     * @return 分页结果
     */
    @ReadOnly
    @Override
    public <T> PageResult<T> page(long pageNum, long size, LambdaQueryWrapper<T> wrapper, Class<T> entityClass) {
        Query query = wrapper.build();
        long total = getMongoTemplate(true).count(query, entityClass);

        query.skip((pageNum - 1) * size).limit((int) size);
        List<T> records = getMongoTemplate(true).find(query, entityClass);

        return new PageResult<>(pageNum, size, total, records);
    }

    /**
     * 保存
     *
     * @param entity 封装类
     * @return 是否保存成功
     */
    @Override
    public <T> T save(T entity) {
        return getMongoTemplate(false).save(entity);
    }

    /**
     * 批量保存
     *
     * @param entityList 封装类列表
     * @return 是否保存成功
     */
    @Override
    public <T> List<T> saveBatch(Collection<T> entityList) {
        return (List<T>) getMongoTemplate(false).insertAll(entityList);
    }

    /**
     * 批量保存
     *
     * @param entityList  封装类列表
     * @param entityClass 封装类
     * @return 是否保存成功
     */
    @Override
    public <T> List<T> saveBatch(Collection<T> entityList, Class<T> entityClass) {
        return (List<T>) getMongoTemplate(false).insert(entityList, entityClass);
    }

    /**
     * 根据id更新
     *
     * @param entity 封装类
     * @return 是否更新成功
     */
    @Override
    public <T> boolean updateById(T entity) {
        ObjectId objectId = (ObjectId) ReflectUtil.getFieldValue(entity, MongodbConstant.ID_FIELD);
        getMongoTemplate(false).save(entity);
        if (Objects.isNull(objectId)) {
            // 去除id值,如果是新增,则将id值设置为null，因为切面日志会根据MongoDB id判断是新增还是更新
            ReflectUtil.setFieldValue(entity, MongodbConstant.ID_FIELD, null);
        }
        return true;
    }

    /**
     * 根据条件更新
     *
     * @param queryWrapper  查询条件
     * @param updateWrapper 更新条件
     * @param entityClass   封装类
     *
     */
    @Override
    public <T> boolean update(LambdaQueryWrapper<T> queryWrapper, LambdaUpdateWrapper<T> updateWrapper, Class<T> entityClass) {
        return getMongoTemplate(false).updateFirst(queryWrapper.build(), updateWrapper.build(), entityClass).getModifiedCount() > 0;
    }

    /**
     * 批量更新
     *
     * @param query       查询条件
     * @param update      更新条件
     * @param entityClass 封装类
     *
     */
    @Override
    public <T> long updateMulti(Query query, Update update, Class<T> entityClass) {
        return getMongoTemplate(false).updateMulti(query, update, entityClass).getModifiedCount();
    }

    /**
     * 根据id删除
     *
     * @param id          id
     * @param entityClass 封装类
     * @return 是否删除成功
     */
    @Override
    public <T> boolean removeById(String id, Class<T> entityClass) {
        T entity = getMongoTemplate(false).findById(id, entityClass);
        if (entity != null) {
            getMongoTemplate(false).remove(entity);
            return true;
        }
        return false;
    }

    /**
     * 根据条件删除
     *
     * @param wrapper     封装条件
     * @param entityClass 封装类
     * @return 是否删除成功
     */
    @Override
    public <T> boolean remove(LambdaQueryWrapper<T> wrapper, Class<T> entityClass) {
        return getMongoTemplate(false).remove(wrapper.build(), entityClass).getDeletedCount() > 0;
    }

    /**
     * 批量删除
     *
     * @param ids         id列表
     * @param entityClass 封装类
     * @return 是否删除成功
     */
    @Override
    public <T> long removeBatch(Collection<String> ids, Class<T> entityClass) {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        Query query = new Query(Criteria.where(MongodbConstant.ID_FIELD).in(ids));
        return getMongoTemplate(false).remove(query, entityClass).getDeletedCount();
    }

    /**
     * 根据id查询
     *
     * @param ids         ids
     * @param entityClass 封装类
     * @return 封装类
     */
    @Override
    public <T> List<T> findByIds(Collection<String> ids, Class<T> entityClass) {
        if (CollectionUtils.isEmpty(ids)) {
            return java.util.Collections.emptyList();
        }

        Query query = new Query(Criteria.where(MongodbConstant.ID_FIELD).in(ids));
        return getMongoTemplate(false).find(query, entityClass);
    }

    /**
     * 根据条件查询并更新
     *
     * @param query       查询条件
     * @param update      更新条件
     * @param entityClass 封装类
     * @return 封装类
     */
    @Override
    public <T> T findAndModify(LambdaQueryWrapper<T> query, LambdaUpdateWrapper<T> update, Class<T> entityClass) {
        return getMongoTemplate(false).findAndModify(query.build(), update.build(), entityClass);
    }

    /**
     * 创建集合
     *
     * @param entityClass 封装类
     */
    @Override
    public <T> void createCollection(Class<T> entityClass) {
        if (!getMongoTemplate(false).collectionExists(entityClass)) {
            getMongoTemplate(false).createCollection(entityClass);
        }
    }

    /**
     * 删除集合
     *
     * @param entityClass 封装类
     */
    @Override
    public <T> void dropCollection(Class<T> entityClass) {
        if (getMongoTemplate(false).collectionExists(entityClass)) {
            getMongoTemplate(false).dropCollection(entityClass);
        }
    }

    /**
     * 获取当前数据源对应的 MongoTemplate
     */
    private MongoTemplate getMongoTemplate(boolean readOnly) {
        return dynamicMongoTemplate.getTargetTemplate(readOnly, null);
    }
}
