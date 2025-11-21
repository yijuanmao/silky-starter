package com.silky.starter.mongodb.aspect;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.silky.starter.mongodb.core.constant.MongodbConstant;
import com.silky.starter.mongodb.core.utils.FormatUtils;
import com.silky.starter.mongodb.core.utils.MongoQueryParser;
import com.silky.starter.mongodb.properties.SilkyMongoProperties;
import com.silky.starter.mongodb.support.LambdaQueryWrapper;
import com.silky.starter.mongodb.support.LambdaUpdateWrapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.convert.UpdateMapper;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Mongo日志切面
 *
 * @author: zy
 * @date: 2025-11-19
 */
@Aspect
public class MongoLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(MongoLogAspect.class);

    private final SilkyMongoProperties properties;

    private final QueryMapper queryMapper;

    private final UpdateMapper updateMapper;

    private final MappingMongoConverter mappingMongoConverter;

    public MongoLogAspect(SilkyMongoProperties properties,
                          MappingMongoConverter mappingMongoConverter) {
        this.properties = properties;
        this.mappingMongoConverter = mappingMongoConverter;
        queryMapper = new QueryMapper(mappingMongoConverter);
        updateMapper = new UpdateMapper(mappingMongoConverter);
    }

    @Pointcut("execution(* com.silky.starter.mongodb.template.impl.DefaultMongodbTemplate.*(..))")
    public void mongoOperation() {
    }

    @Around("mongoOperation()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isPrintLog()) {
            return joinPoint.proceed();
        }
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        try {
            Object result = joinPoint.proceed();
            if (properties.isShowSql()) {
                logMongoOperation(methodName, joinPoint.getArgs(), startTime, result);
            } else {
                logger.info("{}.{} executed in {} ms", className, methodName, startTime);
            }
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("{}.{} failed in {} ms with error: {}",
                    className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * 打印MongoDB操作日志
     *
     * @param methodName 方法名
     * @param args       参数
     * @param startTime  开始时间
     * @param result     结果
     */
    private void logMongoOperation(String methodName, Object[] args, long startTime, Object result) {
        switch (methodName) {
            case "save":
                generateSaveSyntax(args, startTime);
                break;
            case "saveBatch":
                generateSaveBatchSyntax(args, startTime);
                break;
            case "getById":
                generateFindByIdSyntax(args, result, startTime);
                break;
            case "list":
                generateFindSyntax(args, startTime);
                break;
            case "updateById":
                generateUpdateByIdSyntax(args, startTime);
                break;
            case "update":
                generateUpdateSyntax(args, startTime);
                break;
            case "removeById":
                generateRemoveByIdSyntax(args, startTime);
                break;
            case "remove":
                generateRemoveSyntax(args, startTime);
                break;
            case "count":
                generateCountSyntax(args, startTime);
                break;
            case "page":
                generatePageSyntax(args, startTime);
                break;
            default:
                generateDefaultSyntax(methodName, args, result);
                break;
        }
    }

    /**
     * 打印查询语句
     *
     * @param clazz         参数
     * @param query         查询对象
     * @param executionTime 执行时间
     */
    private void printlnLogQuery(Class<?> clazz, Query query, long executionTime) {
        MongoPersistentEntity<?> entity = mappingMongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);
        Document mappedField = queryMapper.getMappedObject(query.getFieldsObject(), entity);
        Document mappedSort = queryMapper.getMappedObject(query.getSortObject(), entity);

        String log = "\ndb." + getCollectionName(clazz) + ".find(";
        log += FormatUtils.toJson(mappedQuery.toJson()) + ")";

        if (!query.getFieldsObject().isEmpty()) {
            log += ".projection(";
            log += FormatUtils.toJson(mappedField.toJson()) + ")";
        }
        if (query.isSorted()) {
            log += ".sort(";
            log += FormatUtils.toJson(mappedSort.toJson()) + ")";
        }
        if (query.getLimit() != 0L) {
            log += ".limit(" + query.getLimit() + ")";
        }
        if (query.getSkip() != 0L) {
            log += ".skip(" + query.getSkip() + ")";
        }
        log += ";";
        // 记录慢查询
        long queryTime = System.currentTimeMillis() - executionTime;
        // 打印语句
        logger.info(log + "\n执行时间:" + queryTime + "ms");
    }

    /**
     * 打印查询语句
     *
     * @param clazz     类
     * @param query     查询对象
     * @param update    更新对象
     * @param multi     是否为批量更新
     * @param startTime 查询开始时间
     */
    private void logUpdate(Class<?> clazz, Query query, Update update, boolean multi, Long startTime) {

        MongoPersistentEntity<?> entity = mappingMongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);
        Document mappedUpdate = updateMapper.getMappedObject(update.getUpdateObject(), entity);

        String log = "\ndb." + getCollectionName(clazz) + ".update(";
        log += FormatUtils.toJson(mappedQuery.toJson()) + ",";
        log += FormatUtils.toJson(mappedUpdate.toJson()) + ",";
        log += FormatUtils.toJson("{multi:" + multi + "})");
        log += ";";
        // 记录慢查询
        long queryTime = System.currentTimeMillis() - startTime;
        // 打印语句
        logger.info(log + "\n执行时间:" + queryTime + "ms");
    }

    /**
     * 打印保存语句
     *
     * @param list      保存集合
     * @param startTime 查询开始时间
     */
    private void printlnLogSave(List<?> list, Long startTime) {
        List<JSONObject> cloneList = new ArrayList<>();
        for (Object item : list) {
            JSONObject jsonObject = JSONUtil.parseObj(item);

            jsonObject.remove(MongodbConstant.ID_FIELD);
            cloneList.add(jsonObject);
        }

        Object object = list.get(0);
        String log = "\ndb." + getCollectionName(object.getClass()) + ".save(";
        log += JSONUtil.toJsonPrettyStr(cloneList);
        log += ");";

        // 记录慢查询
        long queryTime = System.currentTimeMillis() - startTime;
        // 打印语句
        logger.info(log + "\n执行时间:" + queryTime + "ms");
    }

    /**
     * 打印查询语句
     *
     * @param clazz     类
     * @param query     查询对象
     * @param startTime 查询开始时间
     */
    private void logDelete(Class<?> clazz, Query query, Long startTime) {

        MongoPersistentEntity<?> entity = mappingMongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);

        String log = "\ndb." + this.getCollectionName(clazz) + ".remove(";
        log += FormatUtils.toJson(mappedQuery.toJson()) + ")";
        log += ";";
        // 记录慢查询
        long queryTime = System.currentTimeMillis() - startTime;
        // 打印语句
        logger.info(log + "\n执行时间:" + queryTime + "ms");
    }

    /**
     * 打印查询数量语句
     *
     * @param clazz     类
     * @param query     查询对象
     * @param startTime 查询开始时间
     */
    private void logCount(Class<?> clazz, Query query, Long startTime) {
        MongoPersistentEntity<?> entity = mappingMongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);

        String log = "\ndb." + this.getCollectionName(clazz) + ".find(";
        log += FormatUtils.toJson(mappedQuery.toJson()) + ")";
        log += ".count();";
        long queryTime = System.currentTimeMillis() - startTime;
        // 打印语句
        logger.info(log + "\n执行时间:" + queryTime + "ms");
    }

    /**
     * 生成保存操作的语法
     */
    private void generateSaveSyntax(Object[] args, long executionTime) {
        if (args.length > 0) {
            Object entity = args[0];
            printlnLogSave(ListUtil.toList(entity), executionTime);
        }
    }

    /**
     * 生成批量保存操作的语法
     */
    private void generateSaveBatchSyntax(Object[] args, long startTime) {
        if (args.length > 0) {
            Collection<?> entities = (Collection<?>) args[0];
            this.printlnLogSave((List<?>) entities, startTime);
        }
    }

    /**
     * 生成根据ID查找的语法
     */
    private void generateFindByIdSyntax(Object[] args, Object result, long startTime) {
        if (args.length >= 2) {
            String id = (String) args[0];
            Class<?> entityClass = (Class<?>) args[1];
            Query query = new Query();
            query.addCriteria(Criteria.where(MongodbConstant.ID_FIELD).is(id));
            printlnLogQuery(entityClass, query, startTime);
        }
    }

    /**
     * 生成查询语法
     */
    private void generateFindSyntax(Object[] args, long executionTime) {
        if (args.length > 0) {
            if (args[0] instanceof LambdaQueryWrapper) {
                LambdaQueryWrapper wrapper = (LambdaQueryWrapper) args[0];
                Query query = wrapper.build();
                Class<?> entityClass = wrapper.getEntityClass();
                printlnLogQuery(entityClass, query, executionTime);
            } else if (args[0] instanceof Class) {
                Class<?> entityClass = (Class<?>) args[0];
                String collectionName = entityClass.getSimpleName().toLowerCase();
                String.format("db.%s.find({})", collectionName);
            }
        }
    }

    /**
     * 生成根据ID更新的语法
     */
    private void generateUpdateByIdSyntax(Object[] args, long startTime) {
        if (args.length >= 2) {
            Object entity = args[1];
            Class<?> entityClass = entity.getClass();
            ObjectId id = (ObjectId) ReflectUtil.getFieldValue(entity, MongodbConstant.ID_FIELD);
            if (Objects.isNull(id)) {
                printlnLogSave(ListUtil.toList(entity), startTime);
            } else {
                Update update = this.buildUpdateFromEntity(entity);
                logUpdate(entityClass, new Query(), update, false, startTime);
            }
        }
    }

    /**
     * 生成更新操作的语法
     */
    private void generateUpdateSyntax(Object[] args, long startTime) {
        if (args.length >= 3) {
            LambdaQueryWrapper<?> query = (LambdaQueryWrapper<?>) args[0];
            LambdaUpdateWrapper<?> update = (LambdaUpdateWrapper<?>) args[1];
            Class<?> entityClass = (Class<?>) args[2];
            this.logUpdate(entityClass, query.build(), update.build(), false, startTime);
        }
    }

    /**
     * 生成根据ID删除的语法
     */
    private void generateRemoveByIdSyntax(Object[] args, long startTime) {
        if (args.length >= 2) {
            String id = (String) args[0];
            Class<?> entityClass = (Class<?>) args[1];
            Query query = new Query(Criteria.where("_id").is(id));
            logDelete(entityClass, query, startTime);
        }
    }

    /**
     * 生成删除操作的语法
     */
    private void generateRemoveSyntax(Object[] args, long startTime) {
        if (args.length >= 2) {
            Query query = (Query) args[0];
            Class<?> entityClass = (Class<?>) args[1];
            logDelete(entityClass, query, startTime);
        }
    }

    /**
     * 生成计数操作的语法
     */
    private void generateCountSyntax(Object[] args, long startTime) {
        if (args.length > 0) {
            if (args[0] instanceof LambdaQueryWrapper) {
                LambdaQueryWrapper<?> query = (LambdaQueryWrapper<?>) args[0];
                Class<?> entityClass = args[1].getClass();

                this.logCount(entityClass, query.build(), startTime);

            } else if (args[0] instanceof Class) {
                Class<?> entityClass = args[0].getClass();
                this.logCount(entityClass, new Query(), startTime);
            }
        }
    }

    /**
     * 生成分页查询的语法
     */
    private void generatePageSyntax(Object[] args, long startTime) {
        if (args.length >= 3) {
            if (args[2] instanceof LambdaQueryWrapper) {
                LambdaQueryWrapper<?> query = (LambdaQueryWrapper<?>) args[2];
                Class<?> entityClass = args[3].getClass();
                printlnLogQuery(entityClass, query.build(), startTime);
            }
        }
    }

    /**
     * 生成默认语法
     */
    private String generateDefaultSyntax(String methodName, Object[] args, Object result) {
        return String.format("db.collection.%s(...)", methodName);
    }


    /**
     * 根据类获取集合名
     *
     * @param clazz 类
     * @return String 集合名
     */
    private String getCollectionName(Class<?> clazz) {
        org.springframework.data.mongodb.core.mapping.Document document = clazz.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class);
        if (document != null) {
            if (StrUtil.isNotEmpty(document.value())) {
                return document.value();
            }
            if (StrUtil.isNotEmpty(document.collection())) {
                return document.collection();
            }
        }
        return StrUtil.lowerFirst(clazz.getSimpleName());
    }

    /**
     * 根据实体对象构建Update对象，排除null值和_id字段
     *
     * @param entity 实体对象
     * @return Update对象
     */
    private <T> Update buildUpdateFromEntity(T entity) {
        Update update = new Update();

        // 使用反射获取实体类的所有字段
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                // 跳过serialVersionUID字段
                if ("serialVersionUID".equals(field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(entity);
                // 排除null值和_id字段
                if (value != null && !MongodbConstant.ID_FIELD.equals(field.getName())) {
                    update.set(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
            }
        }
        return update;
    }

}
