package com.silky.starter.redis.test.cache;

import cn.hutool.core.collection.ListUtil;
import com.silky.starter.redis.cache.template.RedisCacheTemplate;
import com.silky.starter.redis.test.RedisApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RedisCacheTemplate 全量测试类
 *
 * @author zy
 * @date 2025-10-22 15:37
 */
public class RedisCacheTemplateTest extends RedisApplicationTest {

    private static final String KEY_OBJECT = "silky:test:object";
    private static final String KEY_LIST = "silky:test:list";
    private static final String KEY_SET = "silky:test:set";
    private static final String KEY_HASH = "silky:test:hash";
    private static final String KEY_BATCH = "silky:test:batch";
    private static final String KEY_COUNTER = "silky:test:counter";
    private static final String KEY_PATTERN = "silky:test:pattern:*";

    @Autowired
    private RedisCacheTemplate redisCacheTemplate;

    // ============================== String 相关测试 ==============================

    /**
     * 测试设置和获取单个对象（带过期时间）
     */
    @Test
    public void testSetObjectWithExpire() {
        TradeOrder order = buildTradeOrder("测试订单-过期");
        redisCacheTemplate.setObject(KEY_OBJECT, order, 1, TimeUnit.HOURS);
        log.info("设置对象成功，key={}", KEY_OBJECT);
    }

    /**
     * 测试设置和获取单个对象（不带过期时间）
     */
    @Test
    public void testSetObjectWithoutExpire() {
        TradeOrder order = buildTradeOrder("测试订单-永不过期");
        redisCacheTemplate.setObject(KEY_OBJECT + ":permanent", order);
        log.info("设置永久对象成功");
    }

    /**
     * 测试获取对象（不指定类型转换）
     */
    @Test
    public void testGetObject() {
        TradeOrder order = redisCacheTemplate.getObject(KEY_OBJECT);
        log.info("获取到的对象：{}", order);
    }

    /**
     * 测试获取对象并按指定类型转换
     */
    @Test
    public void testGetObjectWithClass() {
        TradeOrder order = redisCacheTemplate.getObject(KEY_OBJECT, TradeOrder.class);
        log.info("类型转换后获取到的对象：{}", order);
    }

    /**
     * 测试 setIfAbsent（仅当键不存在时设置）
     */
    @Test
    public void testSetIfAbsent() {
        Boolean result = redisCacheTemplate.setIfAbsent(KEY_OBJECT + ":absent", "新值", 1, TimeUnit.HOURS);
        log.info("setIfAbsent 结果：{}", result);

        // 再次调用，键已存在，应返回 false
        Boolean result2 = redisCacheTemplate.setIfAbsent(KEY_OBJECT + ":absent", "覆盖值", 1, TimeUnit.HOURS);
        log.info("setIfAbsent 二次调用结果：{}", result2);
    }

    /**
     * 测试 getOrSet（缓存不存在时回源加载）
     */
    @Test
    public void testGetOrSet() {
        // 第一次调用：缓存不存在，触发回源
        TradeOrder order = redisCacheTemplate.getOrSet(KEY_OBJECT + ":orset",
                () -> buildTradeOrder("回源加载的订单"), 30, TimeUnit.MINUTES);
        log.info("getOrSet 第一次获取：{}", order);

        // 第二次调用：缓存命中，不触发回源
        TradeOrder order2 = redisCacheTemplate.getOrSet(KEY_OBJECT + ":orset",
                () -> buildTradeOrder("不应出现的订单"), 30, TimeUnit.MINUTES);
        log.info("getOrSet 第二次获取：{}", order2);
    }

    // ============================== List 相关测试 ==============================

    /**
     * 测试写入 List 数据（带过期时间）
     */
    @Test
    public void testSetListWithExpire() {
        List<TradeOrder> orders = buildTradeOrders();
        long count = redisCacheTemplate.setList(KEY_LIST, orders, 1, TimeUnit.HOURS);
        log.info("写入 List 成功，元素数量：{}", count);
    }

    /**
     * 测试写入 List 数据（不带过期时间）
     */
    @Test
    public void testSetListWithoutExpire() {
        List<TradeOrder> orders = buildTradeOrders();
        long count = redisCacheTemplate.setList(KEY_LIST + ":permanent", orders);
        log.info("写入永久 List 成功，元素数量：{}", count);
    }

    /**
     * 测试写入空 List（边界情况）
     */
    @Test
    public void testSetEmptyList() {
        long count = redisCacheTemplate.setList(KEY_LIST + ":empty", Collections.emptyList());
        log.info("写入空 List 结果：{}", count);
    }

    /**
     * 测试获取 List（不指定类型转换）
     */
    @Test
    public void testGetList() {
        List<Object> list = redisCacheTemplate.getList(KEY_LIST);
        log.info("获取到的原始 List：{}", list);
    }

    /**
     * 测试获取 List 并按指定类型转换每个元素
     */
    @Test
    public void testGetListWithClass() {
        List<TradeOrder> list = redisCacheTemplate.getList(KEY_LIST, TradeOrder.class);
        for (TradeOrder order : list) {
            log.info("订单对象：{}", order);
        }
        log.info("获取到的对象列表，数量：{}", list.size());
    }

    /**
     * 测试按范围读取 List（不转换类型）
     */
    @Test
    public void testGetListRange() {
        List<Object> list = redisCacheTemplate.getListRange(KEY_LIST, 0, -1);
        log.info("范围读取 List：{}", list);
    }

    /**
     * 测试按范围读取 List 并按指定类型转换
     */
    @Test
    public void testGetListRangeWithClass() {
        List<TradeOrder> list = redisCacheTemplate.getListRange(KEY_LIST, 0, 0, TradeOrder.class);
        log.info("范围读取并转换后的 List（第一个元素）：{}", list);
    }

    /**
     * 测试向 List 右侧追加单个元素
     */
    @Test
    public void testRightPush() {
        TradeOrder newOrder = buildTradeOrder("追加的订单");
        long size = redisCacheTemplate.rightPush(KEY_LIST, newOrder);
        log.info("追加元素后 List 长度：{}", size);
    }

    /**
     * 测试获取 List 长度
     */
    @Test
    public void testListSize() {
        long size = redisCacheTemplate.listSize(KEY_LIST);
        log.info("List 长度：{}", size);
    }

    /**
     * 测试移除 List 中与给定值相等的元素
     */
    @Test
    public void testRemoveListValue() {
        TradeOrder order = buildTradeOrder("追加的订单");
        long removed = redisCacheTemplate.removeListValue(KEY_LIST, order);
        log.info("移除元素数量：{}", removed);
    }

    // ============================== Set 相关测试 ==============================

    /**
     * 测试写入 Set 数据（带过期时间）
     */
    @Test
    public void testSetSetWithExpire() {
        Set<String> dataSet = new HashSet<>(Arrays.asList("apple", "banana", "cherry"));
        long count = redisCacheTemplate.setSet(KEY_SET, dataSet, 1, TimeUnit.HOURS);
        log.info("写入 Set 成功，新增元素数量：{}", count);
    }

    /**
     * 测试写入 Set 数据（不带过期时间）
     */
    @Test
    public void testSetSetWithoutExpire() {
        Set<TradeOrder> dataSet = new HashSet<>(buildTradeOrders());
        long count = redisCacheTemplate.setSet(KEY_SET + ":obj", dataSet);
        log.info("写入对象 Set 成功，新增数量：{}", count);
    }

    /**
     * 测试获取 Set（不转换类型）
     */
    @Test
    public void testGetSet() {
        Set<Object> set = redisCacheTemplate.getSet(KEY_SET);
        log.info("获取到的原始 Set：{}", set);
    }

    /**
     * 测试获取 Set 并按指定类型转换
     */
    @Test
    public void testGetSetWithClass() {
        Set<TradeOrder> set = redisCacheTemplate.getSet(KEY_SET + ":obj", TradeOrder.class);
        for (TradeOrder order : set) {
            log.info("Set 中的订单：{}", order);
        }
        log.info("获取到的对象 Set 大小：{}", set.size());
    }

    /**
     * 测试判断值是否是 Set 的成员
     */
    @Test
    public void testIsSetMember() {
        boolean isMember = redisCacheTemplate.isSetMember(KEY_SET, "apple");
        log.info("'apple' 是否是 Set 成员：{}", isMember);

        boolean notMember = redisCacheTemplate.isSetMember(KEY_SET, "grape");
        log.info("'grape' 是否是 Set 成员：{}", notMember);
    }

    /**
     * 测试获取 Set 的大小
     */
    @Test
    public void testSetSize() {
        long size = redisCacheTemplate.setSize(KEY_SET);
        log.info("Set 大小：{}", size);
    }

    /**
     * 测试从 Set 中移除指定值
     */
    @Test
    public void testRemoveSetValue() {
        long removed = redisCacheTemplate.removeSetValue(KEY_SET, "cherry");
        log.info("从 Set 中移除元素数量：{}", removed);
    }

    // ============================== Hash 相关测试 ==============================

    /**
     * 测试批量写入 Hash（带过期时间）
     */
    @Test
    public void testSetHashWithExpire() {
        Map<String, TradeOrder> dataMap = new HashMap<>();
        dataMap.put("order:001", buildTradeOrder("Hash订单1"));
        dataMap.put("order:002", buildTradeOrder("Hash订单2"));
        redisCacheTemplate.setHash(KEY_HASH, dataMap, 1, TimeUnit.HOURS);
        log.info("批量写入 Hash 成功");
    }

    /**
     * 测试批量写入 Hash（不带过期时间）
     */
    @Test
    public void testSetHashWithoutExpire() {
        Map<String, TradeOrder> dataMap = new HashMap<>();
        dataMap.put("order:003", buildTradeOrder("Hash订单3"));
        redisCacheTemplate.setHash(KEY_HASH + ":permanent", dataMap);
        log.info("批量写入永久 Hash 成功");
    }

    /**
     * 测试读取 Hash 所有字段（不转换类型）
     */
    @Test
    public void testGetHash() {
        Map<Object, Object> hash = redisCacheTemplate.getHash(KEY_HASH);
        log.info("获取到的原始 Hash：{}", hash);
    }

    /**
     * 测试读取 Hash 所有字段并按指定类型转换值
     */
    @Test
    public void testGetHashWithClass() {
        Map<String, TradeOrder> hash = redisCacheTemplate.getHash(KEY_HASH, TradeOrder.class);
        hash.forEach((k, v) -> log.info("Hash field={}, value={}", k, v));
        log.info("获取到的 Hash 大小：{}", hash.size());
    }

    /**
     * 测试写入 Hash 单个字段
     */
    @Test
    public void testSetHashValue() {
        TradeOrder order = buildTradeOrder("Hash单个字段订单");
        redisCacheTemplate.setHashValue(KEY_HASH, "order:004", order);
        log.info("写入 Hash 单个字段成功");
    }

    /**
     * 测试读取 Hash 单个字段值（不转换类型）
     */
    @Test
    public void testGetHashValue() {
        Object value = redisCacheTemplate.getHashValue(KEY_HASH, "order:001");
        log.info("Hash 字段 order:001 的值：{}", value);
    }

    /**
     * 测试读取 Hash 单个字段值并按指定类型转换
     */
    @Test
    public void testGetHashValueWithClass() {
        TradeOrder order = redisCacheTemplate.getHashValue(KEY_HASH, "order:001", TradeOrder.class);
        log.info("类型转换后的 Hash 字段值：{}", order);
    }

    /**
     * 测试批量读取 Hash 多个字段值（不转换类型）
     */
    @Test
    public void testGetMultiHashValue() {
        List<Object> values = redisCacheTemplate.getMultiHashValue(KEY_HASH,
                Arrays.asList("order:001", "order:002", "order:999"));
        for (int i = 0; i < values.size(); i++) {
            log.info("multiGet[{}]: {}", i, values.get(i));
        }
    }

    /**
     * 测试批量读取 Hash 多个字段值并按指定类型转换
     */
    @Test
    public void testGetMultiHashValueWithClass() {
        List<TradeOrder> orders = redisCacheTemplate.getMultiHashValue(KEY_HASH,
                Arrays.asList("order:001", "order:002"), TradeOrder.class);
        for (TradeOrder order : orders) {
            log.info("multiGet 转换后的订单：{}", order);
        }
    }

    /**
     * 测试删除 Hash 单个字段
     */
    @Test
    public void testDeleteHashValue() {
        boolean deleted = redisCacheTemplate.deleteHashValue(KEY_HASH, "order:004");
        log.info("删除 Hash 字段结果：{}", deleted);

        // 删除不存在的字段
        boolean deleted2 = redisCacheTemplate.deleteHashValue(KEY_HASH, "order:999");
        log.info("删除不存在的 Hash 字段结果：{}", deleted2);
    }

    /**
     * 测试获取 Hash 的字段数量
     */
    @Test
    public void testHashSize() {
        long size = redisCacheTemplate.hashSize(KEY_HASH);
        log.info("Hash 字段数量：{}", size);
    }

    // ============================== Key 相关测试 ==============================

    /**
     * 测试设置键的过期时间
     */
    @Test
    public void testExpire() {
        boolean result = redisCacheTemplate.expire(KEY_OBJECT, 30, TimeUnit.MINUTES);
        log.info("设置过期时间结果：{}", result);
    }

    /**
     * 测试获取键的剩余过期时间
     */
    @Test
    public void testGetExpire() {
        Long ttl = redisCacheTemplate.getExpire(KEY_OBJECT);
        log.info("键 {} 剩余过期时间（秒）：{}", KEY_OBJECT, ttl);
    }

    /**
     * 测试判断键是否存在
     */
    @Test
    public void testHasKey() {
        boolean exists = redisCacheTemplate.hasKey(KEY_OBJECT);
        log.info("键 {} 是否存在：{}", KEY_OBJECT, exists);

        boolean notExists = redisCacheTemplate.hasKey("silky:not:exist");
        log.info("键 silky:not:exist 是否存在：{}", notExists);
    }

    /**
     * 测试删除单个键
     */
    @Test
    public void testDelete() {
        redisCacheTemplate.delete(KEY_OBJECT + ":absent");
        log.info("删除单个键成功");
    }

    /**
     * 测试批量删除键
     */
    @Test
    public void testDeleteBatch() {
        long count = redisCacheTemplate.delete(Arrays.asList(
                KEY_OBJECT + ":permanent",
                KEY_LIST + ":permanent",
                KEY_SET + ":obj"
        ));
        log.info("批量删除键数量：{}", count);
    }

    /**
     * 测试按模式批量删除键（基于 SCAN）
     */
    @Test
    public void testDeleteByPattern() {
        // 先准备数据
        for (int i = 1; i <= 3; i++) {
            redisCacheTemplate.setObject(KEY_PATTERN.replace("*", String.valueOf(i)), "value-" + i);
        }
        log.info("准备了 3 个模式匹配的键");

        // 按模式删除
        long count = redisCacheTemplate.deleteByPattern("silky:test:pattern:*");
        log.info("按模式删除键数量：{}", count);
    }

    /**
     * 测试 keys 命令查询键集合
     */
    @Test
    public void testKeys() {
        Set<String> keys = redisCacheTemplate.keys("silky:test:*");
        log.info("匹配 silky:test:* 的键集合：{}", keys);
    }

    // ============================== 批量操作测试 ==============================

    /**
     * 测试批量读取多个键的值并按指定类型转换
     */
    @Test
    public void testMultiGet() {
        // 先准备数据
        redisCacheTemplate.setObject(KEY_BATCH + ":1", buildTradeOrder("批量订单1"), 10, TimeUnit.MINUTES);
        redisCacheTemplate.setObject(KEY_BATCH + ":2", buildTradeOrder("批量订单2"), 10, TimeUnit.MINUTES);
        redisCacheTemplate.setObject(KEY_BATCH + ":3", buildTradeOrder("批量订单3"), 10, TimeUnit.MINUTES);

        Map<String, TradeOrder> result = redisCacheTemplate.multiGet(
                Arrays.asList(KEY_BATCH + ":1", KEY_BATCH + ":2", KEY_BATCH + ":3", KEY_BATCH + ":999"),
                TradeOrder.class);
        result.forEach((k, v) -> log.info("multiGet key={}, value={}", k, v));
        log.info("批量读取结果数量：{}", result.size());
    }

    /**
     * 测试批量写入键值对并统一设置过期时间（Pipeline）
     */
    @Test
    public void testMultiSet() {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put(KEY_BATCH + ":m1", "value1");
        dataMap.put(KEY_BATCH + ":m2", "value2");
        dataMap.put(KEY_BATCH + ":m3", "value3");
        redisCacheTemplate.multiSet(dataMap, 1, TimeUnit.HOURS);
        log.info("批量写入（Pipeline）成功");

        // 验证过期时间是否设置
        Long ttl = redisCacheTemplate.getExpire(KEY_BATCH + ":m1");
        log.info("键 {} 剩余过期时间（秒）：{}", KEY_BATCH + ":m1", ttl);
    }

    // ============================== 其他辅助操作测试 ==============================

    /**
     * 测试自增操作
     */
    @Test
    public void testIncrement() {
        // 先重置
        redisCacheTemplate.delete(KEY_COUNTER);
        long val1 = redisCacheTemplate.increment(KEY_COUNTER, 1);
        log.info("自增 1 后的值：{}", val1);
        long val2 = redisCacheTemplate.increment(KEY_COUNTER, 5);
        log.info("再自增 5 后的值：{}", val2);
    }

    /**
     * 测试自减操作
     */
    @Test
    public void testDecrement() {
        long val1 = redisCacheTemplate.decrement(KEY_COUNTER, 3);
        log.info("自减 3 后的值：{}", val1);
        long val2 = redisCacheTemplate.decrement(KEY_COUNTER, 1);
        log.info("再自减 1 后的值：{}", val2);
    }

    // ============================== 数据构建方法 ==============================

    /**
     * 构建单个 TradeOrder 对象
     */
    private TradeOrder buildTradeOrder(String name) {
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(System.currentTimeMillis());
        tradeOrder.setCreateTime(java.time.LocalDateTime.now());
        tradeOrder.setOrderName(name);
        tradeOrder.setAmount(BigDecimal.ONE);
        return tradeOrder;
    }

    /**
     * 构建多个 TradeOrder 对象
     */
    private List<TradeOrder> buildTradeOrders() {
        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setOrderId(System.currentTimeMillis());
        tradeOrder.setCreateTime(java.time.LocalDateTime.now());
        tradeOrder.setOrderName("测试订单-缓存");
        tradeOrder.setAmount(BigDecimal.ONE);

        TradeOrder tradeOrder1 = new TradeOrder();
        tradeOrder1.setOrderId(System.currentTimeMillis());
        tradeOrder1.setCreateTime(java.time.LocalDateTime.now());
        tradeOrder1.setOrderName("测试订单1-缓存");
        tradeOrder1.setAmount(BigDecimal.valueOf(20));

        return ListUtil.of(tradeOrder, tradeOrder1);
    }
}
