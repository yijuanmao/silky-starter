package com.silky.starter.mongodb.template.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.mongodb.client.result.DeleteResult;
import com.silky.starter.mongodb.annotation.CreateTime;
import com.silky.starter.mongodb.annotation.IgnoreColumn;
import com.silky.starter.mongodb.annotation.InitValue;
import com.silky.starter.mongodb.annotation.UpdateTime;
import com.silky.starter.mongodb.build.SortBuilder;
import com.silky.starter.mongodb.core.utils.FormatUtils;
import com.silky.starter.mongodb.core.utils.ReflectionUtil;
import com.silky.starter.mongodb.model.base.BaseMongodbModel;
import com.silky.starter.mongodb.model.page.Page;
import com.silky.starter.mongodb.properties.MongodbProperties;
import com.silky.starter.mongodb.support.SerializableFunction;
import com.silky.starter.mongodb.template.MongodbTemplate;
import com.silky.starter.mongodb.wrapper.CriteriaWrapper;
import com.silky.starter.mongodb.wrapper.query.CriteriaAndWrapper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.convert.UpdateMapper;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.Query;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

/**
 * mongodb操作模板
 *
 * @author zy
 * @date 2025-09-04 16:00
 **/
public class DefaultMongodbTemplateImpl implements MongodbTemplate {

    private static final Logger log = LoggerFactory.getLogger(MongodbTemplate.class);

    private static final String MONGO_ID_FIELD = "mongoId";
    private static final String ID_FIELD = "_id";

    /**
     * 是否打印日志
     */
    protected boolean print;

    /**
     * 是否记录慢查询到数据库中
     */
    protected Boolean slowQuery;

    /**
     * 打印执行时间
     */
    protected boolean slowTime;

    private final MongodbProperties mongodbProperties;

    protected final MongoConverter mongoConverter;

    protected final MongoTemplate mongoTemplate;

    private final QueryMapper queryMapper;

    private final UpdateMapper updateMapper;

    public DefaultMongodbTemplateImpl(MongodbProperties properties, MongoConverter mongoConverter, MongoTemplate mongoTemplate) {
        this.mongodbProperties = properties;
        this.mongoConverter = mongoConverter;
        this.mongoTemplate = mongoTemplate;
        this.queryMapper = new QueryMapper(mongoConverter);
        this.updateMapper = new UpdateMapper(mongoConverter);

        this.slowQuery = properties.isSlowQuery();
        this.print = properties.isPrintLog();
        this.slowTime = properties.isSlowQuery();
    }

    /**
     * 分页查询
     *
     * @param page  分页
     * @param clazz 实体类
     * @return 分页结果
     */
    @Override
    public <T> Page<T> findPage(Page<?> page, Class<T> clazz) {
        return findPage(new CriteriaAndWrapper(), page, clazz);
    }

    /**
     * 条件分页查询
     *
     * @param criteriaWrapper 条件构造器
     * @param page            分页
     * @param clazz           实体类
     * @return 分页结果
     */
    @Override
    public <T> Page<T> findPage(CriteriaWrapper criteriaWrapper, Page<?> page, Class<T> clazz) {
        SortBuilder sortBuilder = new SortBuilder(BaseMongodbModel::getMongoId, Sort.Direction.DESC);
        return findPage(criteriaWrapper, sortBuilder, page, clazz);
    }

    /**
     * 条件分页查询
     *
     * @param criteriaWrapper 条件构造
     * @param sort            排序
     * @param page            分页
     * @param clazz           实体类
     * @return 分页结果
     */
    @Override
    public <T> Page<T> findPage(CriteriaWrapper criteriaWrapper, SortBuilder sort, Page<?> page, Class<T> clazz) {
        Page<T> pageResp = new Page<T>(page.getPageNum(), page.getPageSize());

        Long count = countByQuery(criteriaWrapper, clazz);
        pageResp.setTotal(count);

        // 查询List
        Query query = new Query(criteriaWrapper.build());
        query.with(sort.toSort());
        // 从那条记录开始
        query.skip((long) (page.getPageNum() - 1) * page.getPageSize());
        // 取多少条记录
        query.limit(page.getPageSize());

        Long systemTime = System.currentTimeMillis();
        List<T> list = mongoTemplate.find(query, clazz);
        logQuery(clazz, query, systemTime);
        pageResp.setList(list);
        return pageResp;
    }

    /**
     * 根据ID查询
     *
     * @param id    ID
     * @param clazz 实体类
     * @return 实体类
     */
    @Override
    public <T> T findById(String id, Class<T> clazz) {
        if (StrUtil.isEmpty(id)) {
            return null;
        }
        Long systemTime = System.currentTimeMillis();
        T t = mongoTemplate.findById(id, clazz);
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper().eq(BaseMongodbModel::getMongoId, id);
        logQuery(clazz, new Query(criteriaAndWrapper.build()), systemTime);
        return t;
    }

    /**
     * 根据条件查询单个
     *
     * @param criteriaWrapper 条件构造
     * @param clazz           实体类
     * @return 实体类
     */
    @Override
    public <T> T findOneByQuery(CriteriaWrapper criteriaWrapper, Class<T> clazz) {
        SortBuilder sortBuilder = new SortBuilder(BaseMongodbModel::getMongoId, Sort.Direction.DESC);
        return findOneByQuery(criteriaWrapper, sortBuilder, clazz);
    }

    /**
     * 根据条件和排序查询单个
     *
     * @param criteriaWrapper 条件构造
     * @param sortBuilder     排序
     * @param clazz           实体类
     * @return 实体类
     */
    @Override
    public <T> T findOneByQuery(CriteriaWrapper criteriaWrapper, SortBuilder sortBuilder, Class<T> clazz) {
        Query query = new Query(criteriaWrapper.build());
        query.limit(1);
        query.with(sortBuilder.toSort());

        Long systemTime = System.currentTimeMillis();
        T t = mongoTemplate.findOne(query, clazz);
        logQuery(clazz, query, systemTime);
        return t;
    }

    /**
     * 根据条件查询列表
     *
     * @param criteria 条件构造
     * @param clazz    实体类
     * @return 实体类列表
     */
    @Override
    public <T> List<T> findListByQuery(CriteriaWrapper criteria, Class<T> clazz) {
        SortBuilder sortBuilder = new SortBuilder().add(BaseMongodbModel::getMongoId, Sort.Direction.DESC);
        return findListByQuery(criteria, sortBuilder, clazz);
    }

    /**
     * 根据条件查询列表
     *
     * @param criteria 条件构造
     * @param sort     排序
     * @param clazz    实体类
     * @return 实体类列表
     */
    @Override
    public <T> List<T> findListByQuery(CriteriaWrapper criteria, SortBuilder sort, Class<T> clazz) {
        Query query = new Query(criteria.build());
        query.with(sort.toSort());
        Long systemTime = System.currentTimeMillis();
        List<T> list = mongoTemplate.find(query, clazz);
        logQuery(clazz, query, systemTime);
        return list;
    }

    /**
     * 根据条件查找某个属性
     *
     * @param criteria      查询
     * @param documentClass 类
     * @param property      属性
     * @return List 列表
     */
    @Override
    public <R, E> List<String> findPropertiesByQuery(CriteriaWrapper criteria, Class<?> documentClass, SerializableFunction<E, R> property) {
        return findPropertiesByQuery(criteria, documentClass, property, String.class);
    }

    /**
     * 根据条件查找某个属性
     *
     * @param criteria      查询
     * @param documentClass 类
     * @param property      属性
     * @param propertyClass 属性类
     * @return List 列表
     */
    @Override
    public <T, R, E> List<T> findPropertiesByQuery(CriteriaWrapper criteria, Class<?> documentClass, SerializableFunction<E, R> property, Class<T> propertyClass) {
        Query query = new Query(criteria.build());
        query.fields().include(ReflectionUtil.getFieldName(property));
        Long systemTime = System.currentTimeMillis();
        List<?> documents = mongoTemplate.find(query, documentClass);
        logQuery(documentClass, query, systemTime);
        return extractProperty(documents, ReflectionUtil.getFieldName(property), propertyClass);
    }

    /**
     * 根据id集合查找某个属性
     *
     * @param ids      id列表
     * @param clazz    类
     * @param property 属性
     * @return List 列表
     */
    @Override
    public <R, E> List<String> findPropertiesByIds(List<String> ids, Class<?> clazz, SerializableFunction<E, R> property) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper().in(BaseMongodbModel::getMongoId, ids);
        return findPropertiesByQuery(criteriaAndWrapper, clazz, property);
    }

    /**
     * 根据条件查询ID列表
     *
     * @param criteria 条件构造
     * @param clazz    实体类
     * @return ID列表
     */
    @Override
    public List<String> findIdsByQuery(CriteriaWrapper criteria, Class<?> clazz) {
        return findPropertiesByQuery(criteria, clazz, BaseMongodbModel::getMongoId);
    }

    /**
     * 根据ID列表查询
     *
     * @param ids   ID列表
     * @param clazz 实体类
     * @return 实体类列表
     */
    @Override
    public <T> List<T> findListByIds(Collection<String> ids, Class<T> clazz) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper().in(BaseMongodbModel::getMongoId, ids);
        return findListByQuery(criteriaAndWrapper, clazz);
    }

    /**
     * 根据ID列表查询
     *
     * @param ids         ID列表
     * @param sortBuilder 排序
     * @param clazz       实体类
     */
    @Override
    public <T> List<T> findListByIds(Collection<String> ids, SortBuilder sortBuilder, Class<T> clazz) {
        CriteriaAndWrapper criteriaAndWrapper = new CriteriaAndWrapper().in(BaseMongodbModel::getMongoId, ids);
        return findListByQuery(criteriaAndWrapper, sortBuilder, clazz);
    }

    /**
     * 根据条件查询数量
     *
     * @param criteriaWrapper 条件构造
     * @param clazz           实体类
     * @return 数量
     */
    @Override
    public Long countByQuery(CriteriaWrapper criteriaWrapper, Class<?> clazz) {
        Long systemTime = System.currentTimeMillis();
        long count;
        Query query = new Query(criteriaWrapper.build());
        if (query.getQueryObject().isEmpty()) {
            count = mongoTemplate.estimatedCount(clazz);
        } else {
            count = mongoTemplate.count(query, clazz);
        }
        this.logOperation("count", clazz, query, systemTime);
        return count;
    }

    /**
     * 插入
     *
     * @param object 实体类
     * @return ID
     */
    @Override
    public String insert(Object object) {
        insertOrUpdate(object);
        return (String) ReflectUtil.getFieldValue(object, ID_FIELD);
    }

    /**
     * 批量插入
     *
     * @param list 实体类列表
     * @return ID
     */
    @Override
    public <T> List<String> batchInsert(List<T> list) {
        Long time = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        List<Object> listClone = new ArrayList<>(list.size());
        for (Object object : list) {
            Field[] fields = ReflectUtil.getFields(object.getClass());
            // 去除id以便插入
            String id = Convert.toStr(ReflectUtil.getFieldValue(object, ID_FIELD));
            if (StrUtil.isNotBlank(id)) {
                // 去除id值
                ReflectUtil.setFieldValue(object, ID_FIELD, null);
            }
            // 设置插入时间
            setCreateTime(object, now);
            // 设置更新时间
            setUpdateTime(object, now);
            // 设置默认值
            setDefaultValue(object, fields);
            // 克隆一个@IgnoreColumn的字段设为null的对象;
            Object objectClone = BeanUtil.copyProperties(object, object.getClass());
            ignoreColumn(objectClone);
            listClone.add(objectClone);
        }
        mongoTemplate.insertAll(listClone);
        logSave(listClone, time, true);
        List<String> ids = new ArrayList<>(list.size());
        for (Object object : listClone) {
            String id = (String) ReflectUtil.getFieldValue(object, ID_FIELD);
            ids.add(id);
        }
        return ids;
    }

    /**
     * 根据ID删除
     *
     * @param object 实体类
     * @return ID
     */
    @Override
    public String updateById(Object object) {
        return insertOrUpdate(object);
    }

    /**
     * 插入或更新
     *
     * @param object 保存对象
     * @return String 对象id
     */
    @Override
    public String insertOrUpdate(Object object) {
        Long time = System.currentTimeMillis();
        LocalDateTime now = LocalDateTimeUtil.now();
        String id = (String) ReflectUtil.getFieldValue(object, ID_FIELD);
        Object objectOrg = StrUtil.isNotEmpty(id) ? findById(id, object.getClass()) : null;

        if (objectOrg == null) {
            Field[] fields = ReflectUtil.getFields(object.getClass());
            // 插入
            // 设置插入时间
            setCreateTime(object, now);
            // 设置更新时间
            setUpdateTime(object, now);

            // 设置默认值
            setDefaultValue(object, fields);
            //校验是否有mongoid主键
//            boolean annotation = isIdAnnotation(fields);
            id = Convert.toStr(ReflectUtil.getFieldValue(object, ID_FIELD));
            if (StrUtil.isNotBlank(id)) {
                // 去除id值
                ReflectUtil.setFieldValue(object, ID_FIELD, null);
            }
            // 克隆一个@IgnoreColumn的字段设为null的对象;
            Object objectClone = BeanUtil.copyProperties(object, object.getClass());
            ignoreColumn(objectClone);

            mongoTemplate.save(objectClone);
            id = (String) ReflectUtil.getFieldValue(objectClone, ID_FIELD);

            if (StrUtil.isNotBlank(id)) {
                // 设置id值
                ReflectUtil.setFieldValue(object, ID_FIELD, id);
            }
            logSave(objectClone, time, true);
        } else {
            // 更新
            Field[] fields = ReflectUtil.getFields(object.getClass());
            // 拷贝属性
            for (Field field : fields) {
                if (!field.getName().equals(ID_FIELD) && ReflectUtil.getFieldValue(object, field) != null) {
                    ReflectUtil.setFieldValue(objectOrg, field, ReflectUtil.getFieldValue(object, field));
                }
            }
            // 设置更新时间
            setUpdateTime(objectOrg, now);
            // 克隆一个@IgnoreColumn的字段设为null的对象;
            Object objectClone = BeanUtil.copyProperties(objectOrg, object.getClass());
            ignoreColumn(objectClone);

            mongoTemplate.save(objectClone);
            logSave(objectClone, time, false);
        }
        return id;
    }

    /**
     * 根据id删除
     *
     * @param id    对象
     * @param clazz 类
     * @return DeleteResult 删除结果
     */
    @Override
    public DeleteResult deleteById(String id, Class<?> clazz) {
        if (StrUtil.isEmpty(id)) {
            return null;
        }
        return deleteByQuery(new CriteriaAndWrapper().eq(BaseMongodbModel::getMongoId, id), clazz);
    }

    /**
     * 根据id删除
     *
     * @param ids   对象
     * @param clazz 类
     * @return DeleteResult 删除结果
     */
    @Override
    public DeleteResult deleteByIds(List<String> ids, Class<?> clazz) {
        if (CollUtil.isEmpty(ids)) {
            return null;
        }
        return deleteByQuery(new CriteriaAndWrapper().in(BaseMongodbModel::getMongoId, ids), clazz);
    }

    /**
     * 根据条件删除
     *
     * @param criteriaWrapper 查询
     * @param clazz           类
     * @return DeleteResult 删除结果
     */
    @Override
    public DeleteResult deleteByQuery(CriteriaWrapper criteriaWrapper, Class<?> clazz) {
        Long time = System.currentTimeMillis();
        Query query = new Query(criteriaWrapper.build());
        DeleteResult deleteResult = mongoTemplate.remove(query, clazz);
        logOperation("remove", clazz, query, time);
        return deleteResult;
    }

    /**
     * 打印查询语句
     *
     * @param clazz     类
     * @param query     查询对象
     * @param startTime 查询开始时间
     */
    private void logQuery(Class<?> clazz, Query query, Long startTime) {
        if (!print) {
            return;
        }
        MongoPersistentEntity<?> entity = mongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);
        Document mappedField = queryMapper.getMappedObject(query.getFieldsObject(), entity);
        Document mappedSort = queryMapper.getMappedObject(query.getSortObject(), entity);

        String log = "\ndb." + ReflectionUtil.getCollectionName(clazz) + ".find(";
        log += FormatUtils.bson(mappedQuery.toJson()) + ")";

        if (!query.getFieldsObject().isEmpty()) {
            log += ".projection(";
            log += FormatUtils.bson(mappedField.toJson()) + ")";
        }
        if (query.isSorted()) {
            log += ".sort(";
            log += FormatUtils.bson(mappedSort.toJson()) + ")";
        }
        if (query.getLimit() != 0L) {
            log += ".limit(" + query.getLimit() + ")";
        }
        if (query.getSkip() != 0L) {
            log += ".skip(" + query.getSkip() + ")";
        }
        log += ";";
        // 记录慢查询
        long queryTime = System.currentTimeMillis() - startTime;
        // 打印语句
        DefaultMongodbTemplateImpl.log.info(log + "\n执行时间:" + queryTime + "ms");
    }

    /**
     * 获取list中对象某个属性,组成新的list
     *
     * @param list     列表
     * @param clazz    类
     * @param property 属性名
     * @return List<T> 属性列表
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> extractProperty(List<?> list, String property, Class<T> clazz) {
        Set<T> rs = new HashSet<T>();
        for (Object object : list) {
            Object value = ReflectUtil.getFieldValue(object, property);
            if (value != null && value.getClass().equals(clazz)) {
                rs.add((T) value);
            }
        }
        return new ArrayList<T>(rs);
    }

    /**
     * 打印查询数量语句
     *
     * @param clazz     类
     * @param query     查询对象
     * @param startTime 查询开始时间
     */
    private void logCount(Class<?> clazz, Query query, Long startTime) {
        if (!print) {
            return;
        }
        MongoPersistentEntity<?> entity = mongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);

        String log = "\ndb." + ReflectionUtil.getCollectionName(clazz) + ".find(";
        log += FormatUtils.bson(mappedQuery.toJson()) + ")";
        log += ".count();";

        // 记录慢查询
        long queryTime = System.currentTimeMillis() - startTime;
        // 打印语句
        DefaultMongodbTemplateImpl.log.info(log + "\n执行时间:" + queryTime + "ms");

        this.logOperation("count", clazz, query, startTime);
    }

    /**
     * 设置更新时间
     *
     * @param object 对象
     * @param now    更新时间
     */
    private void setUpdateTime(Object object, LocalDateTime now) {
        Field[] fields = ReflectUtil.getFields(object.getClass());
        for (Field field : fields) {
            // 获取注解
            if (field.isAnnotationPresent(UpdateTime.class) && field.getType().equals(LocalDateTime.class)) {
//                String updateTimeStr = LocalDateTimeUtil.format(now, DatePattern.NORM_DATETIME_PATTERN);
                ReflectUtil.setFieldValue(object, field, now);
            }
        }
    }

    /**
     * 设置创建时间
     *
     * @param object 对象
     * @param now    创建时间
     */
    private void setCreateTime(Object object, LocalDateTime now) {
        Field[] fields = ReflectUtil.getFields(object.getClass());
        for (Field field : fields) {
            // 获取注解
            if (field.isAnnotationPresent(CreateTime.class) && field.getType().equals(LocalDateTime.class)) {
//                String createTimeStr = LocalDateTimeUtil.format(now, DatePattern.NORM_DATETIME_PATTERN);
                ReflectUtil.setFieldValue(object, field, now);
            }
        }
    }

    /**
     * 设置默认值
     *
     * @param object 对象
     * @param fields 字段
     */
    private void setDefaultValue(Object object, Field[] fields) {
        for (Field field : fields) {
            // 获取注解
            if (field.isAnnotationPresent(InitValue.class)) {
                InitValue defaultValue = field.getAnnotation(InitValue.class);

                String value = defaultValue.value();

                if (ReflectUtil.getFieldValue(object, field) == null) {
                    // 获取字段类型
                    Class<?> type = field.getType();
                    if (type.equals(String.class)) {
                        ReflectUtil.setFieldValue(object, field, value);
                    }
                    if (type.equals(Short.class)) {
                        ReflectUtil.setFieldValue(object, field, Short.parseShort(value));
                    }
                    if (type.equals(Integer.class)) {
                        ReflectUtil.setFieldValue(object, field, Integer.parseInt(value));
                    }
                    if (type.equals(Long.class)) {
                        ReflectUtil.setFieldValue(object, field, Long.parseLong(value));
                    }
                    if (type.equals(Float.class)) {
                        ReflectUtil.setFieldValue(object, field, Float.parseFloat(value));
                    }
                    if (type.equals(Double.class)) {
                        ReflectUtil.setFieldValue(object, field, Double.parseDouble(value));
                    }
                    if (type.equals(Boolean.class)) {
                        ReflectUtil.setFieldValue(object, field, Boolean.parseBoolean(value));
                    }
                }
            }
        }
    }

    /**
     * 将带有@IgnoreColumn的字段设为null;
     *
     * @param object 对象
     */
    @SuppressWarnings("unchecked")
    private void ignoreColumn(Object object) {
        Field[] fields = ReflectUtil.getFields(object.getClass());
        for (Field field : fields) {
            // 获取注解
            if (field.isAnnotationPresent(IgnoreColumn.class)) {
                ReflectUtil.setFieldValue(object, field, null);
            }
            if (field.getType().equals(Map.class)) {
                Map<String, Object> fieldValue = (Map<String, Object>) ReflectUtil.getFieldValue(object, field);
                if (MapUtil.isEmpty(fieldValue)) {
                    ReflectUtil.setFieldValue(object, field, null);
                }
            }
        }
    }

    /**
     * 打印保存语句
     *
     * @param object    保存对象
     * @param startTime 查询开始时间
     * @param isInsert  是否为插入
     */
    private void logSave(Object object, Long startTime, boolean isInsert) {
        this.logSave(CollUtil.toList(object), startTime, isInsert);
    }

    /**
     * 打印保存语句
     *
     * @param list      保存集合
     * @param startTime 查询开始时间
     */
    private void logSave(List<?> list, Long startTime, boolean isInsert) {
        //是否打印日志
        if (!print) {
            return;
        }
        if (CollUtil.isEmpty(list)) {
            return;
        }
        List<JSONObject> cloneList = new ArrayList<>(list.size());
        for (Object item : list) {
            JSONObject jsonObject = JSONObject.from(item);
            if (isInsert) {
                jsonObject.remove(ID_FIELD);
            }
            cloneList.add(jsonObject);
        }
        Object object = list.get(0);
        String collectionName = ReflectionUtil.getCollectionName(object.getClass());

        String logBuilder = "\ndb." + collectionName + ".save(" +
                JSONObject.toJSONString(cloneList) +
                ");";
        // 记录慢查询
        long queryTime = System.currentTimeMillis() - startTime;

        // 可选：记录慢查询
        if (queryTime > 5000L) {
            log.warn("Slow query detected: " + queryTime + "ms");
        }
        log.info(logBuilder + "\n执行时间:" + queryTime + "ms");
    }

    /**
     * 打印日志语句
     *
     * @param operation 操作
     * @param clazz     类
     * @param query     查询对象
     * @param startTime 查询开始时间
     */
    private void logOperation(String operation, Class<?> clazz, Query query, Long startTime) {
        if (!print) {
            return;
        }

        MongoPersistentEntity<?> entity = mongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);

        StringBuilder logBuilder = new StringBuilder("\ndb.")
                .append(ReflectionUtil.getCollectionName(clazz))
                .append(".")
                .append(operation)
                .append("(")
                .append(FormatUtils.bson(mappedQuery.toJson()))
                .append(")");

        // 添加执行时间
        long queryTime = System.currentTimeMillis() - startTime;
        logBuilder.append(";\n执行时间:").append(queryTime).append("ms");

        // 可选：记录慢查询
        if (queryTime > 5000L) {
            log.warn("Slow query detected: " + queryTime + "ms");
        }
        log.info(logBuilder + "\n执行时间:" + queryTime + "ms");
    }
}
