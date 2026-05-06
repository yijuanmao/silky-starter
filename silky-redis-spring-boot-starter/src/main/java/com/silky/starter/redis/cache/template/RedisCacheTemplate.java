package com.silky.starter.redis.cache.template;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson2.JSON;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 缓存操作模板，封装 String、List、Set、Hash 及批量操作能力。
 *
 * @author zy
 * @date 2025-10-22 11:35
 */
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public class RedisCacheTemplate {

    private final RedisTemplate redisTemplate;

    /**
     * 构造 Redis 缓存模板。
     *
     * @param redisTemplate RedisTemplate 实例
     */
    public RedisCacheTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设置缓存对象（不指定过期时间）。
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param <T>   值类型
     */
    public <T> void setObject(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存对象（指定过期时间）。
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     * @param <T>      值类型
     */
    public <T> void setObject(String key, T value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 仅当键不存在时设置缓存对象。
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     * @param <T>      值类型
     * @return true 表示设置成功，false 表示键已存在
     */
    public <T> Boolean setIfAbsent(String key, T value, long timeout, TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
    }

    /**
     * 设置键的过期时间（默认秒）。
     *
     * @param key     缓存键
     * @param timeout 过期时长（秒）
     * @return true 表示设置成功
     */
    public boolean expire(String key, long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置键的过期时间。
     *
     * @param key     缓存键
     * @param timeout 过期时长
     * @param unit    时间单位
     * @return true 表示设置成功
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取键的剩余过期时间。
     *
     * @param key 缓存键
     * @return 剩余时间（单位由 RedisTemplate 默认行为决定）
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断键是否存在。
     *
     * @param key 缓存键
     * @return true 表示存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 删除单个键。
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除键。
     *
     * @param keys 键集合
     * @return 实际删除数量
     */
    public long delete(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        }
        return redisTemplate.delete(keys);
    }

    /**
     * 按模式批量删除键（基于 SCAN，避免 KEYS 全量阻塞）。
     *
     * @param pattern 匹配模式，例如 user:*
     * @return 实际删除数量
     */
    public long deleteByPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return 0L;
        }

        Set<String> keys = new HashSet<>();
        ScanOptions scanOptions = ScanOptions.scanOptions().match(pattern).count(1000).build();
        RedisSerializer keySerializer = redisTemplate.getKeySerializer();

        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(scanOptions)) {
                while (cursor.hasNext()) {
                    byte[] rawKey = cursor.next();
                    Object key = keySerializer.deserialize(rawKey);
                    if (key != null) {
                        keys.add(key.toString());
                    }
                }
            }
            return null;
        });

        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        }
        return delete(keys);
    }

    /**
     * 获取缓存对象（不做强类型转换）。
     *
     * @param key 缓存键
     * @param <T> 目标类型
     * @return 缓存对象
     */
    public <T> T getObject(String key) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 获取缓存对象并按指定类型转换。
     *
     * @param key   缓存键
     * @param clazz 目标类型
     * @param <T>   目标类型
     * @return 转换后的对象
     */
    public <T> T getObject(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return convertValue(value, clazz);
    }

    /**
     * 获取缓存对象；若不存在则回源并写入缓存。
     *
     * @param key      缓存键
     * @param supplier 回源函数
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     * @param <T>      目标类型
     * @return 缓存值或回源值
     */
    public <T> T getOrSet(String key, java.util.function.Supplier<T> supplier, long timeout, TimeUnit timeUnit) {
        T value = this.getObject(key);
        if (value == null && supplier != null) {
            value = supplier.get();
            if (value != null) {
                setObject(key, value, timeout, timeUnit);
            }
        }
        return value;
    }

    /**
     * 写入 List 数据。
     *
     * @param key      缓存键
     * @param dataList 列表数据
     * @param <T>      元素类型
     * @return 写入后的列表长度
     */
    public <T> long setList(String key, List<T> dataList) {
        return setList(key, dataList, -1, TimeUnit.SECONDS);
    }

    /**
     * 写入 List 数据并设置过期时间。
     *
     * @param key      缓存键
     * @param dataList 列表数据
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     * @param <T>      元素类型
     * @return 写入后的列表长度
     */
    public <T> long setList(String key, List<T> dataList, long timeout, TimeUnit timeUnit) {
        if (CollectionUtils.isEmpty(dataList)) {
            return 0L;
        }
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList, timeout, timeUnit);
        return count != null ? count : 0L;
    }

    /**
     * 读取完整 List。
     *
     * @param key 缓存键
     * @param <T> 元素类型
     * @return 列表数据
     */
    public <T> List<T> getList(String key) {
        return (List<T>) redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 读取完整 List 并按元素类型转换。
     *
     * @param key   缓存键
     * @param clazz 元素目标类型
     * @param <T>   元素类型
     * @return 转换后的列表数据
     */
    public <T> List<T> getList(String key, Class<T> clazz) {
        List<Object> rawList = this.getListRange(key, 0, -1);
        if (CollectionUtils.isEmpty(rawList)) {
            return Collections.emptyList();
        }
        return rawList.stream()
                .filter(Objects::nonNull)
                .map(item -> convertValue(item, clazz))
                .collect(Collectors.toList());
    }

    /**
     * 按范围读取 List 数据。
     *
     * @param key   缓存键
     * @param start 起始索引
     * @param end   结束索引
     * @param <T>   元素类型
     * @return 列表数据
     */
    public <T> List<T> getListRange(String key, long start, long end) {
        return (List<T>) redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 按范围读取 List 数据并按元素类型转换。
     *
     * @param key   缓存键
     * @param start 起始索引
     * @param end   结束索引
     * @param clazz 元素目标类型
     * @param <T>   元素类型
     * @return 转换后的列表数据
     */
    public <T> List<T> getListRange(String key, long start, long end, Class<T> clazz) {
        List<Object> rawList = this.getListRange(key, start, end);
        if (CollectionUtils.isEmpty(rawList)) {
            return Collections.emptyList();
        }
        return rawList.stream()
                .filter(Objects::nonNull)
                .map(item -> convertValue(item, clazz))
                .collect(Collectors.toList());
    }

    /**
     * 写入 Set 数据。
     *
     * @param key     缓存键
     * @param dataSet 集合数据
     * @param <T>     元素类型
     * @return 实际新增元素数量
     */
    public <T> long setSet(String key, Set<T> dataSet) {
        if (CollectionUtils.isEmpty(dataSet)) {
            return 0L;
        }
        Long count = redisTemplate.opsForSet().add(key, dataSet.toArray());
        return count != null ? count : 0L;
    }

    /**
     * 读取 Set 数据。
     *
     * @param key 缓存键
     * @param <T> 元素类型
     * @return 集合数据
     */
    public <T> Set<T> getSet(String key) {
        return (Set<T>) redisTemplate.opsForSet().members(key);
    }

    /**
     * 读取 Set 数据并按元素类型转换。
     *
     * @param key   缓存键
     * @param clazz 元素目标类型
     * @param <T>   元素类型
     * @return 转换后的集合数据
     */
    public <T> Set<T> getSet(String key, Class<T> clazz) {
        Set<Object> rawSet = redisTemplate.opsForSet().members(key);
        if (CollectionUtils.isEmpty(rawSet)) {
            return Collections.emptySet();
        }
        return rawSet.stream()
                .filter(Objects::nonNull)
                .map(item -> convertValue(item, clazz))
                .collect(Collectors.toSet());
    }

    /**
     * 写入 Hash 全量字段。
     *
     * @param key     缓存键
     * @param dataMap 字段映射
     * @param <T>     值类型
     */
    public <T> void setHash(String key, Map<String, T> dataMap) {
        if (MapUtil.isEmpty(dataMap)) {
            return;
        }
        redisTemplate.opsForHash().putAll(key, dataMap);
    }

    /**
     * 读取 Hash 全量字段并按值类型转换。
     *
     * @param key       缓存键
     * @param valueType 值目标类型
     * @param <T>       值类型
     * @return 转换后的字段映射
     */
    public <T> Map<String, T> getHash(String key, Class<T> valueType) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (MapUtil.isEmpty(entries)) {
            return MapUtil.empty();
        }
        return entries.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> convertValue(entry.getValue(), valueType)
                ));
    }

    /**
     * 写入 Hash 单个字段。
     *
     * @param key   缓存键
     * @param hKey  Hash 字段键
     * @param value 字段值
     * @param <T>   值类型
     */
    public <T> void setHashValue(String key, String hKey, T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 读取 Hash 单个字段值。
     *
     * @param key  缓存键
     * @param hKey Hash 字段键
     * @param <T>  值类型
     * @return 字段值
     */
    public <T> T getHashValue(String key, String hKey) {
        HashOperations<String, String, T> hashOps = redisTemplate.opsForHash();
        return hashOps.get(key, hKey);
    }

    /**
     * 读取 Hash 单个字段值并按类型转换。
     *
     * @param key   缓存键
     * @param hKey  Hash 字段键
     * @param clazz 值目标类型
     * @param <T>   值类型
     * @return 转换后的字段值
     */
    public <T> T getHashValue(String key, String hKey, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, hKey);
        if (value == null) {
            return null;
        }
        return convertValue(value, clazz);
    }

    /**
     * 批量读取 Hash 多个字段值。
     *
     * @param key   缓存键
     * @param hKeys Hash 字段键集合
     * @param <T>   值类型
     * @return 字段值列表
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getMultiHashValue(String key, Collection<String> hKeys) {
        if (CollectionUtils.isEmpty(hKeys)) {
            return Collections.emptyList();
        }
        return (List<T>) redisTemplate.opsForHash().multiGet(key, Arrays.asList(hKeys.toArray()));
    }

    /**
     * 批量读取 Hash 多个字段值并按类型转换。
     *
     * @param key   缓存键
     * @param hKeys Hash 字段键集合
     * @param clazz 值目标类型
     * @param <T>   值类型
     * @return 转换后的字段值列表
     */
    public <T> List<T> getMultiHashValue(String key, Collection<String> hKeys, Class<T> clazz) {
        if (CollectionUtils.isEmpty(hKeys)) {
            return Collections.emptyList();
        }
        List<Object> values = redisTemplate.opsForHash().multiGet(key, Arrays.asList(hKeys.toArray()));
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(item -> convertValue(item, clazz))
                .collect(Collectors.toList());
    }

    /**
     * 删除 Hash 单个字段。
     *
     * @param key  缓存键
     * @param hKey Hash 字段键
     * @return true 表示删除成功
     */
    public boolean deleteHashValue(String key, String hKey) {
        Long result = redisTemplate.opsForHash().delete(key, hKey);
        return result > 0;
    }

    /**
     * 批量读取多个键值并按类型转换。
     *
     * @param keys  键集合
     * @param clazz 值目标类型
     * @param <T>   值类型
     * @return 键值映射
     */
    public <T> Map<String, T> multiGet(Collection<String> keys, Class<T> clazz) {
        if (CollectionUtils.isEmpty(keys)) {
            return MapUtil.empty();
        }
        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        if (CollectionUtils.isEmpty(values)) {
            return MapUtil.empty();
        }
        Map<String, T> result = new HashMap<>(values.size() + 2);
        int index = 0;
        for (String key : keys) {
            if (index < values.size()) {
                Object value = values.get(index);
                if (value != null) {
                    result.put(key, convertValue(value, clazz));
                }
            }
            index++;
        }
        return result;
    }

    /**
     * 批量写入键值并通过 Pipeline 批量设置过期时间。
     *
     * @param keyValueMap 键值映射
     * @param timeout     过期时长
     * @param timeUnit    时间单位
     * @param <T>         值类型
     */
    public <T> void multiSet(Map<String, T> keyValueMap, long timeout, TimeUnit timeUnit) {
        if (keyValueMap == null || keyValueMap.isEmpty()) {
            return;
        }
        redisTemplate.opsForValue().multiSet(keyValueMap);
        if (timeout <= 0 || timeUnit == null) {
            return;
        }

        long expireMillis = Math.max(1L, timeUnit.toMillis(timeout));
        RedisSerializer keySerializer = redisTemplate.getKeySerializer();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keyValueMap.keySet()) {
                byte[] rawKey = keySerializer.serialize(key);
                if (rawKey != null) {
                    connection.pExpire(rawKey, expireMillis);
                }
            }
            return null;
        });
    }

    /**
     * 按模式查询键集合。
     *
     * @param pattern 匹配模式
     * @return 键集合
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 执行自增操作。
     *
     * @param key   缓存键
     * @param delta 增量
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 执行自减操作。
     *
     * @param key   缓存键
     * @param delta 减量
     * @return 自减后的值
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * 兼容旧版本的函数式接口定义（建议优先使用 JDK Supplier）。
     *
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    @Deprecated
    public interface Supplier<T> extends java.util.function.Supplier<T> {
    }

    /**
     * 将 Redis 原始值转换为目标类型，并处理字符串/集合等兼容场景。
     * <p>
     * 核心策略：先将 value 序列化为 JSON 字符串，再从字符串反序列化为目标类型，
     * 避免 {@code JSON.to()} 在 JSONObject→POJO 转换时因内部字段类型不匹配而报错。
     * </p>
     *
     * @param value      原始值
     * @param targetType 目标类型
     * @param <T>        目标类型
     * @return 转换后的值
     */
    private <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        // 如果已经是目标类型，直接返回
        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }
        // 如果value是Collection类型（JSONArray等），取第一个元素进行转换
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                return null;
            }
            if (collection.size() == 1 && !Collection.class.isAssignableFrom(targetType)) {
                value = collection.iterator().next();
                if (targetType.isInstance(value)) {
                    return targetType.cast(value);
                }
            }
        }
        // 先序列化为JSON字符串，再反序列化为目标类型（避免 JSON.to 的字段类型匹配问题）
        String jsonString = JSON.toJSONString(value);
        return JSON.parseObject(jsonString, targetType);
    }
}
