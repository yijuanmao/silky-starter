package com.silky.starter.mongodb.core.utils;

import org.springframework.data.mongodb.core.query.Query;

import java.util.Map;
import java.util.StringJoiner;

/**
 * MongoDB 查询解析工具类
 *
 * @author: zy
 * @date: 2025-11-20
 */
public class MongoQueryParser {

    /**
     * 解析查询条件为可读字符串
     */
    public static String parseQuery(Query query) {
        if (query == null) {
            return "{}";
        }
        Map<String, Object> criteriaMap = query.getQueryObject();
        if (criteriaMap.isEmpty()) {
            return "{}";
        }
        return formatCriteria(criteriaMap);
    }

    /**
     * 格式化查询条件
     */
    private static String formatCriteria(Map<String, Object> criteria) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        StringJoiner joiner = new StringJoiner(", ");
        criteria.forEach((key, value) -> {
            if (value instanceof Map) {
                // 处理操作符如 $gt, $lt 等
                joiner.add(formatOperatorCriteria(key, (Map<String, Object>) value));
            } else {
                joiner.add(formatSimpleCriteria(key, value));
            }
        });

        sb.append(joiner.toString());
        sb.append("}");
        return sb.toString();
    }

    /**
     * 格式化操作符条件
     */
    private static String formatOperatorCriteria(String field, Map<String, Object> operators) {
        StringBuilder sb = new StringBuilder();
        sb.append(field).append(": {");

        StringJoiner opJoiner = new StringJoiner(", ");
        operators.forEach((op, value) -> {
            opJoiner.add(String.format("%s: %s", op, formatValue(value)));
        });
        sb.append(opJoiner);
        sb.append("}");
        return sb.toString();
    }

    /**
     * 格式化简单条件
     */
    private static String formatSimpleCriteria(String field, Object value) {
        return String.format("%s: %s", field, formatValue(value));
    }

    /**
     * 格式化值
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String) {
            return "'" + value + "'";
        }
        return value.toString();
    }
}
