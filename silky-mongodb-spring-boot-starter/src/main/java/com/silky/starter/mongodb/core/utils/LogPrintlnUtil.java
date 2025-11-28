package com.silky.starter.mongodb.core.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.silky.starter.mongodb.core.constant.MongodbConstant;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.convert.UpdateMapper;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述: 日志打印工具类
 *
 * @author: zy
 * @date: 2025-11-28
 */
public class LogPrintlnUtil {

    private static final Logger logger = LoggerFactory.getLogger(LogPrintlnUtil.class);

    private LogPrintlnUtil() {
    }

    /**
     * 打印查询语句
     *
     * @param clazz                 参数
     * @param query                 查询对象
     * @param startTime             开始时间
     * @param mappingMongoConverter 映射转换器
     * @param queryMapper           查询映射
     */
    public static void queryLog(Class<?> clazz, Query query, long startTime, MappingMongoConverter mappingMongoConverter, QueryMapper queryMapper) {
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
    public static void saveLog(List<?> list, Long startTime) {
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
    public static void deleteLog(Class<?> clazz, Query query, Long startTime,
                                 MappingMongoConverter mappingMongoConverter,
                                 QueryMapper queryMapper) {

        MongoPersistentEntity<?> entity = mappingMongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);

        String log = "\ndb." + getCollectionName(clazz) + ".remove(";
        log += FormatUtils.toJson(mappedQuery.toJson()) + ")";
        log += ";";
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
     * @param update    更新对象
     * @param multi     是否为批量更新
     * @param startTime 查询开始时间
     */
    public static void updateLog(Class<?> clazz, Query query, Update update, boolean multi, Long startTime,
                                 MappingMongoConverter mappingMongoConverter,
                                 QueryMapper queryMapper,
                                 UpdateMapper updateMapper) {

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
     * 打印查询数量语句
     *
     * @param clazz     类
     * @param query     查询对象
     * @param startTime 查询开始时间
     */
    public static void countLog(Class<?> clazz, Query query, Long startTime,
                                MappingMongoConverter mappingMongoConverter,
                                QueryMapper queryMapper) {

        MongoPersistentEntity<?> entity = mappingMongoConverter.getMappingContext().getPersistentEntity(clazz);
        Document mappedQuery = queryMapper.getMappedObject(query.getQueryObject(), entity);

        String log = "\ndb." + getCollectionName(clazz) + ".find(";
        log += FormatUtils.toJson(mappedQuery.toJson()) + ")";
        log += ".count();";
        long queryTime = System.currentTimeMillis() - startTime;
        // 打印语句
        logger.info(log + "\n执行时间:" + queryTime + "ms");
    }

    /**
     * 根据类获取集合名
     *
     * @param clazz 类
     * @return String 集合名
     */
    private static String getCollectionName(Class<?> clazz) {
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
