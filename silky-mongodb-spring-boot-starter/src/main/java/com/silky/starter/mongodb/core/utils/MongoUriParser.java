package com.silky.starter.mongodb.core.utils;

import com.mongodb.ConnectionString;
import org.springframework.util.StringUtils;

/**
 * MongoDB URI解析工具类
 *
 * @author: zy
 * @date: 2025-11-19
 */
public class MongoUriParser {

    /**
     * 从 MongoDB URI 中解析数据库名
     */
    public static String getDatabaseFromUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            throw new IllegalArgumentException("MongoDB URI cannot be null or empty");
        }

        try {
            ConnectionString connectionString = new ConnectionString(uri);
            String database = connectionString.getDatabase();
            if (database == null) {
                throw new IllegalArgumentException("No database specified in MongoDB URI: " + uri);
            }
            return database;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid MongoDB URI: " + uri, e);
        }
    }

    /**
     * 从 MongoDB URI 中解析用户名
     */
    public static String getUsernameFromUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            return null;
        }
        try {
            ConnectionString connectionString = new ConnectionString(uri);
            return connectionString.getUsername();
        } catch (Exception e) {
            return null;
        }
    }
}
