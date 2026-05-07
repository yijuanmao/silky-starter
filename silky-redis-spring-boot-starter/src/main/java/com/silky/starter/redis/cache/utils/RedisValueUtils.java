package com.silky.starter.redis.cache.utils;

import com.alibaba.fastjson2.JSON;
import com.silky.starter.redis.cache.exception.RedisCacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Redis 值类型转换工具类。
 * <p>处理从 Redis 读取数据后的类型转换，支持自动解包嵌套集合/数组、简单类型安全降级等场景。</p>
 *
 * @author zy
 * @date 2026-05-07
 */
public class RedisValueUtils {

    private static final Logger log = LoggerFactory.getLogger(RedisValueUtils.class);

    private RedisValueUtils() {
        // 工具类禁止实例化
    }

    /**
     * 将任意对象转换为目标类型（自动解包嵌套集合/数组）。
     * <p>处理策略：</p>
     * <ul>
     *     <li>null -> null</li>
     *     <li>已是目标类型 -> 强转返回</li>
     *     <li>简单类型（Number、Boolean、Character）目标为非对应简单类型 -> 返回 null 并 warn</li>
     *     <li>Collection 类型 -> 递归处理第一个元素</li>
     *     <li>JSON 数组字符串 -> 解析后递归处理第一个元素</li>
     *     <li>否则 -> JSON.toJSONString + JSON.parseObject 转换</li>
     * </ul>
     *
     * @param value      原始值（可能为 null、集合、JSON 字符串等）
     * @param targetType 目标类型 Class
     * @param <T>        目标类型
     * @return 转换后的对象，若无法转换则抛出 {@link RedisCacheException}
     */
    public static <T> T convert(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        // 1. 已经是目标类型，直接返回
        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }

        // 2. 简单类型目标为非对应简单类型时安全跳过
        if (isSimpleType(value.getClass()) && !isSimpleType(targetType)) {
            log.warn("convert: 无法将简单类型 {} 的值 [{}] 转换为目标类型 {}，返回 null",
                    value.getClass().getSimpleName(), value, targetType.getName());
            return null;
        }

        // 3. 处理集合类型（递归取首元素）
        if (value instanceof Collection) {
            Collection<?> col = (Collection<?>) value;
            if (col.isEmpty()) {
                return null;
            }
            log.debug("convert: value is Collection, extracting first element for target type {}", targetType.getName());
            return convert(col.iterator().next(), targetType);
        }

        // 4. 处理字符串形式的 JSON 数组
        if (value instanceof String) {
            String json = (String) value;
            String trimmed = json.trim();
            if (trimmed.startsWith("[") && !trimmed.equals("[]")) {
                log.warn("convert: value is a JSON array string, but target type {} is not a collection. " +
                        "Will extract first element and convert. JSON: {}", targetType.getName(), json);
                try {
                    List<?> list = JSON.parseArray(trimmed, Object.class);
                    if (list != null && !list.isEmpty()) {
                        return convert(list.get(0), targetType);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse JSON array: {}", json, e);
                }
            }
        }

        // 5. 通用转换：转为 JSON 字符串并解析
        String json;
        try {
            json = (value instanceof String) ? (String) value : JSON.toJSONString(value);
        } catch (Exception e) {
            log.error("convert: failed to convert to JSON string, value={}, type={}", value, value.getClass().getName(), e);
            throw new RedisCacheException("无法将值转换为 JSON 字符串", e);
        }

        try {
            return JSON.parseObject(json, targetType);
        } catch (Exception e) {
            // 最后兜底：尝试解析为数组并取首元素
            String trimmed = json.trim();
            if (trimmed.startsWith("[") && !Collection.class.isAssignableFrom(targetType) && !targetType.isArray()) {
                log.warn("convert fallback: extracting first element from JSON array for target type {}", targetType.getName());
                try {
                    List<?> list = JSON.parseArray(trimmed, Object.class);
                    if (list != null && !list.isEmpty()) {
                        return convert(list.get(0), targetType);
                    }
                } catch (Exception ex) {
                    log.error("Fallback extraction also failed", ex);
                }
            }
            throw new RedisCacheException(String.format("无法将值转换为 %s，原始 JSON: %s", targetType.getName(), json), e);
        }
    }

    /**
     * 判断给定类型是否为简单类型（Number、Boolean、Character 及其包装类和基本类型）。
     *
     * @param clazz 要检查的类型
     * @return 如果是简单类型则返回 true
     */
    private static boolean isSimpleType(Class<?> clazz) {
        return Number.class.isAssignableFrom(clazz)
                || clazz == Boolean.class
                || clazz == Character.class
                || clazz.isPrimitive();
    }
}
