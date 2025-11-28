package com.silky.starter.mongodb.aspect;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ReflectUtil;
import com.silky.starter.mongodb.core.constant.MongodbConstant;
import com.silky.starter.mongodb.core.utils.LogPrintlnUtil;
import com.silky.starter.mongodb.properties.SilkyMongoProperties;
import com.silky.starter.mongodb.support.LambdaQueryWrapper;
import com.silky.starter.mongodb.support.LambdaUpdateWrapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.convert.UpdateMapper;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.lang.reflect.Field;
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
            logMongoOperation(methodName, joinPoint.getArgs(), startTime, result);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("{}.{} failed in {} ms with error: {}",
                    className, methodName, executionTime, e.getMessage(), e);
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
            case "exists":
                generateFindSyntax(args, startTime);
                break;
            default:
                generateDefaultSyntax(methodName, args, result);
                break;
        }
    }


    /**
     * 生成保存操作的语法
     */
    private void generateSaveSyntax(Object[] args, long startTime) {
        if (args.length > 0) {
            Object entity = args[0];
            LogPrintlnUtil.saveLog(ListUtil.toList(entity), startTime);
        }
    }

    /**
     * 生成批量保存操作的语法
     */
    private void generateSaveBatchSyntax(Object[] args, long startTime) {
        if (args.length > 0) {
            Collection<?> entities = (Collection<?>) args[0];
            LogPrintlnUtil.saveLog((List<?>) entities, startTime);
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
            LogPrintlnUtil.queryLog(entityClass, query, startTime, mappingMongoConverter, queryMapper);
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
                LogPrintlnUtil.saveLog(ListUtil.toList(entity), startTime);
            } else {
                Update update = this.buildUpdateFromEntity(entity);
                LogPrintlnUtil.updateLog(entityClass, new Query(), update, false, startTime, mappingMongoConverter, queryMapper, updateMapper);
            }
        }
    }

    /**
     * 生成查询语法
     */
    private void generateFindSyntax(Object[] args, long startTime) {
        if (args.length > 0) {
            if (args[0] instanceof LambdaQueryWrapper) {
                LambdaQueryWrapper wrapper = (LambdaQueryWrapper) args[0];
                Query query = wrapper.build();
                Class<?> entityClass = wrapper.getEntityClass();
                LogPrintlnUtil.queryLog(entityClass, query, startTime, mappingMongoConverter, queryMapper);
            } else if (args[0] instanceof Class) {
                Class<?> entityClass = (Class<?>) args[0];
                String collectionName = entityClass.getSimpleName().toLowerCase();
                String.format("db.%s.find({})", collectionName);
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
            LogPrintlnUtil.updateLog(entityClass, query.build(), update.build(), false, startTime, mappingMongoConverter, queryMapper, updateMapper);
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
            LogPrintlnUtil.deleteLog(entityClass, query, startTime, mappingMongoConverter, queryMapper);
        }
    }

    /**
     * 生成删除操作的语法
     */
    private void generateRemoveSyntax(Object[] args, long startTime) {
        if (args.length >= 2) {
            Query query = (Query) args[0];
            Class<?> entityClass = (Class<?>) args[1];
            LogPrintlnUtil.deleteLog(entityClass, query, startTime, mappingMongoConverter, queryMapper);
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
                LogPrintlnUtil.countLog(entityClass, query.build(), startTime, mappingMongoConverter, queryMapper);
            } else if (args[0] instanceof Class) {
                Class<?> entityClass = args[0].getClass();
                LogPrintlnUtil.countLog(entityClass, new Query(), startTime, mappingMongoConverter, queryMapper);
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
                LogPrintlnUtil.queryLog(entityClass, query.build(), startTime, mappingMongoConverter, queryMapper);
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
