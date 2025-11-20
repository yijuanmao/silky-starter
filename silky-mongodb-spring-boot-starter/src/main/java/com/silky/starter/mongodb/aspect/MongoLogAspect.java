package com.silky.starter.mongodb.aspect;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.silky.starter.mongodb.core.constant.MongodbConstant;
import com.silky.starter.mongodb.core.utils.FormatUtils;
import com.silky.starter.mongodb.core.utils.MongoQueryParser;
import com.silky.starter.mongodb.properties.SilkyMongoProperties;
import com.silky.starter.mongodb.support.LambdaQueryWrapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.convert.UpdateMapper;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
                logMongoOperation(className, methodName, joinPoint.getArgs(), startTime, result);
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
     * @param className  类名
     * @param methodName 方法名
     * @param args       参数
     * @param startTime  开始时间
     * @param result     结果
     */
    private void logMongoOperation(String className, String methodName, Object[] args, long startTime, Object result) {
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
                generateUpdateByIdSyntax(args, result);
                break;
            case "update":
                generateUpdateSyntax(args, result);
                break;
            case "removeById":
                generateRemoveByIdSyntax(args, result);
                break;
            case "remove":
                generateRemoveSyntax(args, result);
                break;
            case "count":
                generateCountSyntax(args, result);
                break;
            case "page":
                generatePageSyntax(args, result);
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
    private void logQuery(Class<?> clazz, Query query, long executionTime) {
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
     * 打印保存语句
     *
     * @param list      保存集合
     * @param startTime 查询开始时间
     */
    private void logSave(List<?> list, Long startTime) {
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
     * 根据方法名获取操作类型
     */
    private String getOperationType(String methodName) {
        if (methodName.startsWith("save")) {
            return "INSERT/SAVE";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return "UPDATE";
        } else if (methodName.startsWith("remove") || methodName.startsWith("delete")) {
            return "DELETE";
        } else if (methodName.startsWith("find") || methodName.startsWith("get") ||
                methodName.startsWith("list") || methodName.startsWith("count") ||
                methodName.startsWith("exists")) {
            return "QUERY";
        } else if (methodName.startsWith("create")) {
            return "CREATE COLLECTION";
        } else if (methodName.startsWith("drop")) {
            return "DROP COLLECTION";
        } else {
            return "OPERATION";
        }
    }

    /**
     * 生成保存操作的语法
     */
    private void generateSaveSyntax(Object[] args, long executionTime) {
        if (args.length > 0) {
            Object entity = args[0];
            logSave(ListUtil.toList(entity), executionTime);
        }
    }

    /**
     * 生成批量保存操作的语法
     */
    private void generateSaveBatchSyntax(Object[] args, long startTime) {
        if (args.length > 0) {
            Collection<?> entities = (Collection<?>) args[0];
            this.logSave((List<?>) entities, startTime);
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
            logQuery(entityClass, query, startTime);
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
                logQuery(entityClass, query, executionTime);
            } else if (args[0] instanceof Class) {
                Class<?> entityClass = (Class<?>) args[0];
                String collectionName = entityClass.getSimpleName().toLowerCase();
                String.format("db.%s.find({})", collectionName);
            }
        }
//        return "db.collection.find({})";
    }

    /**
     * 生成根据ID更新的语法
     */
    private String generateUpdateByIdSyntax(Object[] args, Object result) {
        if (args.length >= 2) {
            String id = (String) args[0];
            Object entity = args[1];
            String collectionName = entity.getClass().getSimpleName().toLowerCase();
            return String.format("db.%s.updateOne({_id: ObjectId('%s')}, {$set: {...}})",
                    collectionName, id);
        }
        return "db.collection.updateOne({_id: ObjectId('id')}, {$set: {...}})";
    }

    /**
     * 生成更新操作的语法
     */
    private String generateUpdateSyntax(Object[] args, Object result) {
        if (args.length >= 3) {
            Query query = (Query) args[0];
            Update update = (Update) args[1];
            Class<?> entityClass = (Class<?>) args[2];
            String collectionName = entityClass.getSimpleName().toLowerCase();

            return String.format("db.%s.updateMany(%s, %s)",
                    collectionName,
                    buildQueryCriteria(query),
                    buildUpdateCriteria(update));
        }
        return "db.collection.updateMany(query, update)";
    }

    /**
     * 生成根据ID删除的语法
     */
    private String generateRemoveByIdSyntax(Object[] args, Object result) {
        if (args.length >= 2) {
            String id = (String) args[0];
            Class<?> entityClass = (Class<?>) args[1];
            String collectionName = entityClass.getSimpleName().toLowerCase();
            return String.format("db.%s.deleteOne({_id: ObjectId('%s')})", collectionName, id);
        }
        return "db.collection.deleteOne({_id: ObjectId('id')})";
    }

    /**
     * 生成删除操作的语法
     */
    private String generateRemoveSyntax(Object[] args, Object result) {
        if (args.length >= 2) {
            Query query = (Query) args[0];
            Class<?> entityClass = (Class<?>) args[1];
            String collectionName = entityClass.getSimpleName().toLowerCase();
            return String.format("db.%s.deleteMany(%s)", collectionName, buildQueryCriteria(query));
        }
        return "db.collection.deleteMany(query)";
    }

    /**
     * 生成计数操作的语法
     */
    private String generateCountSyntax(Object[] args, Object result) {
        if (args.length > 0) {
            if (args[0] instanceof Query) {
                Query query = (Query) args[0];
                Class<?> entityClass = (Class<?>) args[1];
                String collectionName = entityClass.getSimpleName().toLowerCase();
                return String.format("db.%s.countDocuments(%s)", collectionName, buildQueryCriteria(query));
            } else if (args[0] instanceof Class) {
                Class<?> entityClass = (Class<?>) args[0];
                String collectionName = entityClass.getSimpleName().toLowerCase();
                return String.format("db.%s.countDocuments({})", collectionName);
            }
        }
        return "db.collection.countDocuments({})";
    }

    /**
     * 生成分页查询的语法
     */
    private String generatePageSyntax(Object[] args, Object result) {
        if (args.length >= 3) {
            long pageNum = (Long) args[0];
            long size = (Long) args[1];

            if (args[2] instanceof Query) {
                Query query = (Query) args[2];
                Class<?> entityClass = (Class<?>) args[3];
                String collectionName = entityClass.getSimpleName().toLowerCase();

                return String.format("db.%s.find(%s).skip(%d).limit(%d)",
                        collectionName, buildQueryCriteria(query),
                        (pageNum - 1) * size, size);
            }
        }
        return "db.collection.find(query).skip(n).limit(m)";
    }

    /**
     * 生成默认语法
     */
    private String generateDefaultSyntax(String methodName, Object[] args, Object result) {
        return String.format("db.collection.%s(...)", methodName);
    }

    /**
     * 构建查询条件
     */
    private String buildQueryCriteria(Query query) {
        return MongoQueryParser.parseQuery(query);
    }

    /**
     * 构建排序条件
     */
    private String buildSortCriteria(Query query) {
        if (query == null || query.getSortObject() == null) {
            return "{}";
        }
        return query.getSortObject().toString();
    }

    /**
     * 构建更新条件
     */
    private String buildUpdateCriteria(Update update) {
        if (update == null) {
            return "{}";
        }
        Map<String, Object> updateObject = update.getUpdateObject();
        if (updateObject.isEmpty()) {
            return "{}";
        }
        return updateObject.toString();
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

}
