package com.silky.starter.redis.cache.template;

import cn.hutool.core.map.MapUtil;
import com.silky.starter.redis.cache.utils.RedisValueUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Redis 缓存操作模板，封装 String、List、Set、Hash 及批量操作能力。
 *
 * @author zy
 * @date 2025-10-22 11:35
 */
@SuppressWarnings(value = {"unchecked"})
public class RedisCacheTemplate {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 构造 Redis 缓存模板。
     *
     * @param redisTemplate RedisTemplate 实例，通常配置为使用 FastJson2RedisSerializer
     */
    public RedisCacheTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    /**
     * 设置缓存对象（不指定过期时间，永久有效）。
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param <T>   值类型
     */
    public <T> void setObject(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置缓存对象并指定过期时间。
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
     * 仅当键不存在时设置缓存对象（原子操作，常用于分布式锁）。
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     * @param <T>      值类型
     * @return true 表示设置成功（键之前不存在），false 表示键已存在
     */
    public <T> Boolean setIfAbsent(String key, T value, long timeout, TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, timeUnit);
    }

    /**
     * 获取缓存对象（不做类型转换，直接返回原始类型）。
     *
     * @param key 缓存键
     * @param <T> 目标类型（通常由调用方隐式推断）
     * @return 缓存对象，可能为 null
     */
    public <T> T getObject(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存对象并按指定类型转换。
     *
     * @param key   缓存键
     * @param clazz 目标类型 Class
     * @param <T>   目标类型
     * @return 转换后的对象，若键不存在则返回 null
     */
    public <T> T getObject(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        return RedisValueUtils.convert(value, clazz);
    }

    /**
     * 获取缓存对象；若不存在则通过 Supplier 回源加载并写入缓存。
     *
     * @param key      缓存键
     * @param supplier 回源函数（仅当缓存不存在时调用）
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     * @param <T>      目标类型
     * @return 缓存值或回源加载的值
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


    /**
     * 写入 List 数据（不设置过期时间）。
     *
     * @param key      缓存键
     * @param dataList 列表数据
     * @param <T>      元素类型
     * @return 写入后的列表长度（即元素个数）
     */
    public <T> long setList(String key, List<T> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return 0L;
        }
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count != null ? count : 0L;
    }

    /**
     * 写入 List 数据并设置过期时间。
     * <p>先通过 rightPushAll 批量写入所有元素，再对 key 设置过期时间。</p>
     *
     * @param key      缓存键
     * @param dataList 列表数据
     * @param timeout  过期时长（必须大于 0）
     * @param timeUnit 时间单位
     * @param <T>      元素类型
     * @return 写入后的列表长度
     */
    public <T> long setList(String key, List<T> dataList, long timeout, TimeUnit timeUnit) {
        if (CollectionUtils.isEmpty(dataList)) {
            return 0L;
        }
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        if (timeout > 0 && timeUnit != null && count != null && count > 0) {
            redisTemplate.expire(key, timeout, timeUnit);
        }
        return count != null ? count : 0L;
    }

    /**
     * 向 List 右侧追加单个元素。
     *
     * @param key   缓存键
     * @param value 元素值
     * @param <T>   元素类型
     * @return 追加后的列表长度
     */
    public <T> long rightPush(String key, T value) {
        Long count = redisTemplate.opsForList().rightPush(key, value);
        return count != null ? count : 0L;
    }

    /**
     * 获取 List 长度。
     *
     * @param key 缓存键
     * @return 列表长度
     */
    public long listSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0L;
    }

    /**
     * 读取完整 List（不进行元素类型转换，直接返回原始对象列表）。
     *
     * @param key 缓存键
     * @param <T> 元素类型
     * @return 列表数据
     */
    public <T> List<T> getList(String key) {
        return (List<T>) redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 读取完整 List 并将每个元素转换为指定类型。
     *
     * @param key   缓存键
     * @param clazz 元素目标类型 Class
     * @param <T>   元素类型
     * @return 转换后的列表数据
     */
    public <T> List<T> getList(String key, Class<T> clazz) {
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (CollectionUtils.isEmpty(rawList)) {
            return Collections.emptyList();
        }
        return rawList.stream()
                .filter(Objects::nonNull)
                .map(item -> RedisValueUtils.convert(item, clazz))
                .collect(Collectors.toList());
    }

    /**
     * 按范围读取 List 数据（不转换元素类型）。
     *
     * @param key   缓存键
     * @param start 起始索引（从 0 开始）
     * @param end   结束索引（-1 表示末尾）
     * @param <T>   元素类型
     * @return 列表数据
     */
    public <T> List<T> getListRange(String key, long start, long end) {
        return (List<T>) redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 按范围读取 List 数据，并将每个元素转换为指定类型。
     *
     * @param key   缓存键
     * @param start 起始索引
     * @param end   结束索引
     * @param clazz 元素目标类型 Class
     * @param <T>   元素类型
     * @return 转换后的列表数据
     */
    public <T> List<T> getListRange(String key, long start, long end, Class<T> clazz) {
        List<Object> rawList = getListRange(key, start, end);
        if (CollectionUtils.isEmpty(rawList)) {
            return Collections.emptyList();
        }
        return rawList.stream()
                .filter(Objects::nonNull)
                .map(item -> RedisValueUtils.convert(item, clazz))
                .collect(Collectors.toList());
    }

    /**
     * 移除 List 中所有与给定值相等的元素。
     *
     * @param key   缓存键
     * @param value 要移除的值
     * @param <T>   元素类型
     * @return 被移除的元素数量
     */
    public <T> long removeListValue(String key, T value) {
        Long count = redisTemplate.opsForList().remove(key, 0, value);
        return count != null ? count : 0L;
    }

    // ============================== Set 相关操作 ==============================

    /**
     * 写入 Set 集合数据（不设置过期时间）。
     *
     * @param key     缓存键
     * @param dataSet 集合数据
     * @param <T>     元素类型
     * @return 实际新增的元素数量（重复元素不会增加）
     */
    public <T> long setSet(String key, Set<T> dataSet) {
        if (CollectionUtils.isEmpty(dataSet)) {
            return 0L;
        }
        Long count = redisTemplate.opsForSet().add(key, dataSet.toArray());
        return count != null ? count : 0L;
    }

    /**
     * 写入 Set 集合数据并设置过期时间。
     *
     * @param key      缓存键
     * @param dataSet  集合数据
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     * @param <T>      元素类型
     * @return 实际新增的元素数量
     */
    public <T> long setSet(String key, Set<T> dataSet, long timeout, TimeUnit timeUnit) {
        long count = setSet(key, dataSet);
        if (timeout > 0 && timeUnit != null && count > 0) {
            redisTemplate.expire(key, timeout, timeUnit);
        }
        return count;
    }

    /**
     * 读取 Set 集合（不转换元素类型）。
     *
     * @param key 缓存键
     * @param <T> 元素类型
     * @return 集合数据
     */
    public <T> Set<T> getSet(String key) {
        return (Set<T>) redisTemplate.opsForSet().members(key);
    }

    /**
     * 读取 Set 集合并将每个元素转换为指定类型。
     *
     * @param key   缓存键
     * @param clazz 元素目标类型 Class
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
                .map(item -> RedisValueUtils.convert(item, clazz))
                .collect(Collectors.toSet());
    }

    /**
     * 判断值是否是 Set 的成员。
     *
     * @param key   缓存键
     * @param value 要判断的值
     * @param <T>   元素类型
     * @return true 表示是成员
     */
    public <T> boolean isSetMember(String key, T value) {
        Boolean result = redisTemplate.opsForSet().isMember(key, value);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 获取 Set 的大小。
     *
     * @param key 缓存键
     * @return 集合大小
     */
    public long setSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0L;
    }

    /**
     * 从 Set 中移除指定值。
     *
     * @param key    缓存键
     * @param values 要移除的值
     * @param <T>    元素类型
     * @return 被移除的元素数量
     */
    public <T> long removeSetValue(String key, Object... values) {
        Long count = redisTemplate.opsForSet().remove(key, values);
        return count != null ? count : 0L;
    }

    // ============================== Hash 相关操作 ==============================

    /**
     * 批量写入 Hash 所有字段（覆盖整个 Hash）。
     *
     * @param key     缓存键
     * @param dataMap 字段映射（key 为 field，value 为值）
     * @param <T>     值类型
     */
    public <T> void setHash(String key, Map<String, T> dataMap) {
        if (MapUtil.isEmpty(dataMap)) {
            return;
        }
        redisTemplate.opsForHash().putAll(key, dataMap);
    }

    /**
     * 批量写入 Hash 所有字段并设置过期时间。
     *
     * @param key      缓存键
     * @param dataMap  字段映射
     * @param timeout  过期时长
     * @param timeUnit 时间单位
     * @param <T>      值类型
     */
    public <T> void setHash(String key, Map<String, T> dataMap, long timeout, TimeUnit timeUnit) {
        if (MapUtil.isEmpty(dataMap)) {
            return;
        }
        redisTemplate.opsForHash().putAll(key, dataMap);
        if (timeout > 0 && timeUnit != null) {
            redisTemplate.expire(key, timeout, timeUnit);
        }
    }

    /**
     * 读取 Hash 所有字段，并将每个值转换为指定类型。
     *
     * @param key       缓存键
     * @param valueType 值目标类型 Class
     * @param <T>       值类型
     * @return 转换后的字段映射（field -> value）
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
                        entry -> RedisValueUtils.convert(entry.getValue(), valueType)
                ));
    }

    /**
     * 读取 Hash 所有字段（不转换类型）。
     *
     * @param key 缓存键
     * @param <K> field 类型
     * @param <V> value 类型
     * @return 字段映射
     */
    public <K, V> Map<K, V> getHash(String key) {
        return (Map<K, V>) redisTemplate.opsForHash().entries(key);
    }

    /**
     * 写入 Hash 单个字段（若字段已存在则覆盖）。
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
     * 读取 Hash 单个字段值（不转换类型）。
     *
     * @param key  缓存键
     * @param hKey Hash 字段键
     * @param <T>  值类型
     * @return 字段值，可能为 null
     */
    public <T> T getHashValue(String key, String hKey) {
        return (T) redisTemplate.opsForHash().get(key, hKey);
    }

    /**
     * 读取 Hash 单个字段值并转换为指定类型。
     *
     * @param key   缓存键
     * @param hKey  Hash 字段键
     * @param clazz 值目标类型 Class
     * @param <T>   值类型
     * @return 转换后的字段值，可能为 null
     */
    public <T> T getHashValue(String key, String hKey, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, hKey);
        return RedisValueUtils.convert(value, clazz);
    }

    /**
     * 批量读取 Hash 的多个字段值（不转换类型）。
     *
     * @param key   缓存键
     * @param hKeys Hash 字段键集合
     * @param <T>   值类型
     * @return 字段值列表（顺序与 hKeys 一致，不存在的字段对应 null）
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getMultiHashValue(String key, Collection<String> hKeys) {
        if (CollectionUtils.isEmpty(hKeys)) {
            return Collections.emptyList();
        }
        List<Object> results = redisTemplate.opsForHash().multiGet(key, (Collection<Object>) (Collection<?>) hKeys);
        return (List<T>) results;
    }

    /**
     * 批量读取 Hash 的多个字段值，并将每个值转换为指定类型。
     *
     * @param key   缓存键
     * @param hKeys Hash 字段键集合
     * @param clazz 值目标类型 Class
     * @param <T>   值类型
     * @return 转换后的字段值列表（顺序与 hKeys 一致，不存在的字段对应 null）
     */
    public <T> List<T> getMultiHashValue(String key, Collection<String> hKeys, Class<T> clazz) {
        if (CollectionUtils.isEmpty(hKeys)) {
            return Collections.emptyList();
        }
        List<Object> values = redisTemplate.opsForHash().multiGet(key, (Collection<Object>) (Collection<?>) hKeys);
        if (CollectionUtils.isEmpty(values)) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(item -> RedisValueUtils.convert(item, clazz))
                .collect(Collectors.toList());
    }

    /**
     * 删除 Hash 中的单个字段。
     *
     * @param key  缓存键
     * @param hKey Hash 字段键
     * @return true 表示至少删除一个字段，false 表示字段不存在
     */
    public boolean deleteHashValue(String key, String hKey) {
        Long result = redisTemplate.opsForHash().delete(key, hKey);
        return result != null && result > 0;
    }

    /**
     * 获取 Hash 的字段数量。
     *
     * @param key 缓存键
     * @return 字段数量
     */
    public long hashSize(String key) {
        Long size = redisTemplate.opsForHash().size(key);
        return size != null ? size : 0L;
    }

    // ============================== Key 相关操作 ==============================

    /**
     * 设置键的过期时间（默认秒为单位）。
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
     * 获取键的剩余过期时间（单位为秒）。
     *
     * @param key 缓存键
     * @return 剩余时间（秒），-1 表示永不过期，-2 表示键不存在
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
     * @return 实际删除的数量
     */
    public long delete(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return 0L;
        }
        return redisTemplate.delete(keys);
    }

    /**
     * 按模式批量删除键（基于 SCAN 游标，避免 KEYS 命令阻塞 Redis）。
     * <p>示例：deleteByPattern("user:*") 会删除所有 user: 前缀的键。</p>
     *
     * @param pattern 匹配模式，支持通配符 * 和 ?
     * @return 实际删除的数量
     */
    public long deleteByPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return 0L;
        }
        Set<String> keys = new HashSet<>();
        ScanOptions scanOptions = ScanOptions.scanOptions().match(pattern).count(1000).build();
        RedisSerializer<?> keySerializer = redisTemplate.getKeySerializer();
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
     * 按模式查询键集合（警告：生产环境大数据量下慎用，建议使用 deleteByPattern）。
     * <p>此方法内部调用 KEYS 命令，会阻塞 Redis 直到扫描完成。</p>
     *
     * @param pattern 匹配模式，例如 "user:*"
     * @return 键集合
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    // ============================== 批量操作 ==============================

    /**
     * 批量读取多个键的值，并将每个值转换为指定类型。
     *
     * @param keys  键集合
     * @param clazz 值目标类型 Class
     * @param <T>   值类型
     * @return 键值映射（值可能为 null，若某个键不存在则不包含在结果中）
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
                    result.put(key, RedisValueUtils.convert(value, clazz));
                }
            }
            index++;
        }
        return result;
    }

    /**
     * 批量写入键值对，并通过 Pipeline 统一设置过期时间（避免循环调用）。
     * <p>注意：如果 timeout <= 0 或 timeUnit 为 null，则只写入数据，不设置过期时间。</p>
     *
     * @param keyValueMap 键值映射
     * @param timeout     过期时长（<= 0 表示不设置过期时间）
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
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keyValueMap.keySet()) {
                byte[] rawKey = redisTemplate.getStringSerializer().serialize(key);
                if (rawKey != null) {
                    connection.pExpire(rawKey, expireMillis);
                }
            }
            return null;
        });
    }

    // ============================== 其他辅助操作 ==============================

    /**
     * 对指定键的值执行自增操作（适用于计数器）。
     *
     * @param key   缓存键
     * @param delta 增量（正数）
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 对指定键的值执行自减操作。
     *
     * @param key   缓存键
     * @param delta 减量（正数）
     * @return 自减后的值
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }
}
