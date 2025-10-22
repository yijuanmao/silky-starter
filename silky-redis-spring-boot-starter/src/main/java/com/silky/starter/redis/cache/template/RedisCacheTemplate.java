package com.silky.starter.redis.cache.template;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson2.JSON;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * redis缓存操作模板类
 *
 * @author zy
 * @date 2025-10-22 11:35
 **/
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public class RedisCacheTemplate {

    private final RedisTemplate redisTemplate;

    public RedisCacheTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设置缓存（支持泛型）
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setObject(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存（带过期时间）
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public <T> void setObject(String key, T value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置缓存（如果不存在）
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public <T> Boolean setIfAbsent(String key, T value, long timeout, TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key     键
     * @param timeout 超时时间，单位秒
     */
    public boolean expire(String key, long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key     键
     * @param timeout 超时时间
     * @param unit    时间单位
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /**
     * 获取有效时间
     *
     * @return 时间，单位秒；返回-2表示key不存在，返回-1表示key永久有效
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 删除单个对象
     *
     * @param key 键
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 批量删除对象
     *
     * @param keys 键集合
     */
    public long delete(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        }
        Long count = redisTemplate.delete(keys);
        return count != null ? count : 0L;
    }

    /**
     * 根据模式匹配删除
     *
     * @param pattern 模式
     */
    public long deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (!CollectionUtils.isEmpty(keys)) {
            return delete(keys);
        }
        return 0L;
    }

    // ============================== 对象操作 ==============================

    /**
     * 获取缓存对象
     *
     * @param key 键
     */
    public <T> T getObject(String key) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 获取缓存对象，如果不存在则设置
     *
     * @param key      键
     * @param supplier 供应者
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public <T> T getOrSet(String key, Supplier<T> supplier, long timeout, TimeUnit timeUnit) {
        T value = this.getObject(key);
        if (value == null && supplier != null) {
            value = supplier.get();
            if (value != null) {
                setObject(key, value, timeout, timeUnit);
            }
        }
        return value;
    }

    // ============================== List操作 ==============================

    /**
     * 缓存List数据
     *
     * @param key      键
     * @param dataList 数据列表
     */
    public <T> long setList(String key, List<T> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return 0L;
        }
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count != null ? count : 0L;
    }

    /**
     * 缓存List数据
     *
     * @param key      键
     * @param dataList 数据列表
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public <T> long setList(String key, List<T> dataList, long timeout, TimeUnit timeUnit) {
        if (CollectionUtils.isEmpty(dataList)) {
            return 0L;
        }
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList, timeout, timeUnit);
        return count != null ? count : 0L;
    }

    /**
     * 获取缓存的list对象
     *
     * @param key 键
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        return (List<T>) redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 获取List指定范围数据
     *
     * @param key   键
     * @param start 起始位置
     * @param end   结束位置
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getListRange(String key, long start, long end) {
        return (List<T>) redisTemplate.opsForList().range(key, start, end);
    }

    // ============================== Set操作 ==============================

    /**
     * 缓存Set
     *
     * @param key     键
     * @param dataSet 数据集合
     */
    public <T> long setSet(String key, Set<T> dataSet) {
        if (CollectionUtils.isEmpty(dataSet)) {
            return 0L;
        }
        Long count = redisTemplate.opsForSet().add(key, dataSet.toArray());
        return count != null ? count : 0L;
    }

    /**
     * 获取缓存的set
     *
     * @param key 键
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getSet(String key) {
        return (Set<T>) redisTemplate.opsForSet().members(key);
    }

    // ============================== Hash操作 ==============================

    /**
     * 缓存Map
     */
    public <T> void setHash(String key, Map<String, T> dataMap) {
        if (MapUtil.isEmpty(dataMap)) {
            return;
        }
        redisTemplate.opsForHash().putAll(key, dataMap);
    }

    /**
     * 获取缓存的Map
     *
     * @param key       Redis键
     * @param valueType Map值类型
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
     * 往Hash中存入数据
     */
    public <T> void setHashValue(String key, String hKey, T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     */
    public <T> T getHashValue(String key, String hKey) {
        HashOperations<String, String, T> hashOps = redisTemplate.opsForHash();
        return hashOps.get(key, hKey);
    }

    /**
     * 获取多个Hash中的数据
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getMultiHashValue(String key, Collection<String> hKeys) {
        if (CollectionUtils.isEmpty(hKeys)) {
            return Collections.emptyList();
        }
        return (List<T>) redisTemplate.opsForHash().multiGet(key, Arrays.asList(hKeys.toArray()));
    }

    /**
     * 删除Hash中的某条数据
     */
    public boolean deleteHashValue(String key, String hKey) {
        Long result = redisTemplate.opsForHash().delete(key, hKey);
        return result > 0;
    }

    // ============================== 批量操作 ==============================

    /**
     * 批量获取缓存
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
                    if (clazz.isInstance(value)) {
                        result.put(key, clazz.cast(value));
                    } else {
                        result.put(key, JSON.parseObject(JSON.toJSONString(value), clazz));
                    }
                }
            }
            index++;
        }
        return result;
    }

    /**
     * 批量设置缓存
     */
    public <T> void multiSet(Map<String, T> keyValueMap, long timeout, TimeUnit timeUnit) {
        if (keyValueMap == null || keyValueMap.isEmpty()) {
            return;
        }
        redisTemplate.opsForValue().multiSet(keyValueMap);
        // 为每个key设置过期时间
        keyValueMap.keySet().forEach(key -> expire(key, timeout, timeUnit));
    }

    // ============================== 其他操作 ==============================

    /**
     * 获取匹配的keys
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 自增操作
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 自减操作
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    // ============================== 函数式接口 ==============================

    @FunctionalInterface
    public interface Supplier<T> {
        T get();
    }


    /**
     * 值转换辅助方法
     */
    private <T> T convertValue(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        // 如果已经是目标类型，直接返回
        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }
        // 使用JSON进行类型转换
        String jsonString = JSON.toJSONString(value);
        return JSON.parseObject(jsonString, targetType);
    }
}
