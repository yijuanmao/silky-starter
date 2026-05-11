# silky-redis-spring-boot-starter

<p align="center">
  <img src="https://img.shields.io/badge/Silky%20Starter-Redis-brightgreen" alt="Silky Starter Redis"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7+-blue" alt="Spring Boot 2.7+"/>
  <img src="https://img.shields.io/badge/Redis-5.0+-orange" alt="Redis 5.0+"/>
  <img src="https://img.shields.io/badge/Redisson-supported-red" alt="Redisson"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-green" alt="License"/>
</p>

> 全能 Redis 组件，集成缓存、分布式锁、限流、序列号生成、地理位置等多种功能。

## 特性

- **缓存操作**：丰富的数据结构支持（String、List、Set、Hash）
- **分布式锁**：基于 Redisson 的高可靠分布式锁
- **限流控制**：支持令牌桶、固定窗口、滑动窗口算法
- **序列号生成**：分布式环境下的唯一序列号生成
- **地理位置**：基于 Redis GEO 的地理位置服务
- **消息队列**：基于 Redis Stream 的轻量级消息队列，注解驱动消费者、死信队列、Pending 回收

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>top.silky</groupId>
    <artifactId>silky-redis-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 基础配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    # 连接池配置
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms

# Redisson 配置（用于分布式锁）
redisson:
  single-server-config:
    address: "redis://localhost:6379"
    password: 
    database: 0
```

## 核心API详解

### RedisCacheTemplate 缓存操作

`RedisCacheTemplate` 提供了丰富的 Redis 数据类型操作。

#### String 操作

```java
public class RedisCacheTemplate {
    
    /**
     * 设置缓存（支持泛型）
     * 
     * @param key   缓存键
     * @param value 缓存值
     */
    public <T> void setObject(String key, T value);
    
    /**
     * 设置缓存（带过期时间）
     * 
     * @param key      缓存键
     * @param value    缓存值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    public <T> void setObject(String key, T value, long timeout, TimeUnit timeUnit);
    
    /**
     * 设置缓存（如果不存在）
     * 
     * @param key      缓存键
     * @param value    缓存值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return true 表示设置成功
     */
    public <T> Boolean setIfAbsent(String key, T value, long timeout, TimeUnit timeUnit);
    
    /**
     * 获取缓存对象
     * 
     * @param key 缓存键
     * @return 缓存值
     */
    public <T> T getObject(String key);
    
    /**
     * 获取缓存对象，如果不存在则设置
     * 原子操作，防止缓存穿透
     * 
     * @param key      缓存键
     * @param supplier 数据提供者
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 缓存值
     */
    public <T> T getOrSet(String key, Supplier<T> supplier, long timeout, TimeUnit timeUnit);
    
    /**
     * 删除缓存
     * 
     * @param key 缓存键
     * @return true 表示删除成功
     */
    public boolean delete(String key);
    
    /**
     * 根据模式匹配删除
     * 
     * @param pattern 匹配模式，如 "user:*"
     * @return 删除的数量
     */
    public long deleteByPattern(String pattern);
    
    /**
     * 设置过期时间
     * 
     * @param key     缓存键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true 表示设置成功
     */
    public boolean expire(String key, long timeout, TimeUnit unit);
    
    /**
     * 获取剩余过期时间
     * 
     * @param key 缓存键
     * @return 剩余时间（秒），-2 表示 key 不存在，-1 表示永久有效
     */
    public Long getExpire(String key);
    
    /**
     * 判断 key 是否存在
     * 
     * @param key 缓存键
     * @return true 表示存在
     */
    public Boolean hasKey(String key);
    
    /**
     * 自增操作
     * 
     * @param key   缓存键
     * @param delta 增量
     * @return 增加后的值
     */
    public Long increment(String key, long delta);
    
    /**
     * 自减操作
     * 
     * @param key   缓存键
     * @param delta 减量
     * @return 减少后的值
     */
    public Long decrement(String key, long delta);
}
```

#### List 操作

```java
public class RedisCacheTemplate {
    
    /**
     * 缓存 List 数据
     * 
     * @param key      缓存键
     * @param dataList 数据列表
     * @return 添加的元素数量
     */
    public <T> long setList(String key, List<T> dataList);
    
    /**
     * 缓存 List 数据（带过期时间）
     * 
     * @param key      缓存键
     * @param dataList 数据列表
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 添加的元素数量
     */
    public <T> long setList(String key, List<T> dataList, long timeout, TimeUnit timeUnit);
    
    /**
     * 获取缓存的 List
     * 
     * @param key 缓存键
     * @return 数据列表
     */
    public <T> List<T> getList(String key);
    
    /**
     * 获取 List 指定范围数据
     * 
     * @param key   缓存键
     * @param start 起始位置（0 开始）
     * @param end   结束位置（-1 表示到最后）
     * @return 数据列表
     */
    public <T> List<T> getListRange(String key, long start, long end);
}
```

#### Set 操作

```java
public class RedisCacheTemplate {
    
    /**
     * 缓存 Set
     * 
     * @param key     缓存键
     * @param dataSet 数据集合
     * @return 添加的元素数量
     */
    public <T> long setSet(String key, Set<T> dataSet);
    
    /**
     * 获取缓存的 Set
     * 
     * @param key 缓存键
     * @return 数据集合
     */
    public <T> Set<T> getSet(String key);
}
```

#### Hash 操作

```java
public class RedisCacheTemplate {
    
    /**
     * 缓存 Map
     * 
     * @param key     缓存键
     * @param dataMap 数据 Map
     */
    public <T> void setHash(String key, Map<String, T> dataMap);
    
    /**
     * 获取缓存的 Map
     * 
     * @param key       缓存键
     * @param valueType 值类型
     * @return 数据 Map
     */
    public <T> Map<String, T> getHash(String key, Class<T> valueType);
    
    /**
     * 往 Hash 中存入单个字段
     * 
     * @param key   缓存键
     * @param hKey  字段名
     * @param value 字段值
     */
    public <T> void setHashValue(String key, String hKey, T value);
    
    /**
     * 获取 Hash 中的单个字段
     * 
     * @param key  缓存键
     * @param hKey 字段名
     * @return 字段值
     */
    public <T> T getHashValue(String key, String hKey);
    
    /**
     * 获取多个 Hash 字段
     * 
     * @param key   缓存键
     * @param hKeys 字段名列表
     * @return 字段值列表
     */
    public <T> List<T> getMultiHashValue(String key, Collection<String> hKeys);
    
    /**
     * 删除 Hash 中的字段
     * 
     * @param key   缓存键
     * @param hKey  字段名
     * @return true 表示删除成功
     */
    public boolean deleteHashValue(String key, String hKey);
}
```

#### 批量操作

```java
public class RedisCacheTemplate {
    
    /**
     * 批量获取缓存
     * 
     * @param keys  缓存键集合
     * @param clazz 值类型
     * @return key-value Map
     */
    public <T> Map<String, T> multiGet(Collection<String> keys, Class<T> clazz);
    
    /**
     * 批量设置缓存
     * 
     * @param keyValueMap key-value Map
     * @param timeout     过期时间
     * @param timeUnit    时间单位
     */
    public <T> void multiSet(Map<String, T> keyValueMap, long timeout, TimeUnit timeUnit);
    
    /**
     * 批量删除
     * 
     * @param keys 缓存键集合
     * @return 删除的数量
     */
    public long delete(Collection<String> keys);
}
```

### RedisLockTemplate 分布式锁

```java
public class RedisLockTemplate {
    
    /**
     * 执行带锁的操作（推荐使用）
     * 自动获取锁、执行业务、释放锁
     * 
     * @param key       锁的 key
     * @param waitTime  获取锁的最大等待时间
     * @param leaseTime 锁的租期时间
     * @param timeUnit  时间单位
     * @param supplier  业务逻辑函数
     * @return 业务逻辑返回值
     * @throws RedissonException 获取锁失败时抛出
     */
    public <T> T lock(String key, long waitTime, long leaseTime,
                      TimeUnit timeUnit, Supplier<T> supplier);
    
    /**
     * 执行带锁的操作（事务完成后释放锁）
     * 适用于事务性操作，确保事务提交后才释放锁
     * 
     * @param key                     锁的 key
     * @param waitTime                获取锁的最大等待时间
     * @param leaseTime               锁的租期时间
     * @param timeUnit                时间单位
     * @param releaseAfterTransaction 是否在事务完成后释放锁
     * @param supplier                业务逻辑函数
     * @return 业务逻辑返回值
     */
    public <T> T lock(String key, long waitTime, long leaseTime,
                      TimeUnit timeUnit, boolean releaseAfterTransaction,
                      Supplier<T> supplier);
    
    /**
     * 执行带锁的操作（无返回值）
     * 
     * @param key                     锁的 key
     * @param waitTime                获取锁的最大等待时间
     * @param leaseTime               锁的租期时间
     * @param timeUnit                时间单位
     * @param releaseAfterTransaction 是否在事务完成后释放锁
     * @param runnable                业务逻辑
     */
    public void lock(String key, long waitTime, long leaseTime,
                     TimeUnit timeUnit, boolean releaseAfterTransaction,
                     Runnable runnable);
    
    /**
     * 直接获取 RLock 对象
     * 提供最大的灵活性，需手动释放锁
     * 
     * @param key 锁的 key
     * @return RLock 对象
     */
    public RLock getRLock(String key);
    
    /**
     * 强制解锁（谨慎使用）
     * 
     * @param key 锁的 key
     */
    public void forceUnlock(String key);
    
    /**
     * 检查锁是否被当前线程持有
     * 
     * @param key 锁的 key
     * @return true 表示当前线程持有锁
     */
    public boolean isHeldByCurrentThread(String key);
    
    /**
     * 获取锁剩余租期时间
     * 
     * @param key 锁的 key
     * @return 剩余时间（毫秒）
     */
    public long remainTimeToLive(String key);
}
```

### RedisRateLimitTemplate 限流控制

```java
public class RedisRateLimitTemplate {
    
    /**
     * 使用默认配置执行限流操作
     * 默认使用令牌桶算法
     * 
     * @param key      限流 key
     * @param supplier 业务逻辑函数
     * @return 业务逻辑返回值
     * @throws RateLimitExceededException 限流时抛出
     */
    public <T> T execute(String key, Supplier<T> supplier);
    
    /**
     * 使用默认配置执行限流操作（支持降级）
     * 
     * @param key              限流 key
     * @param supplier         业务逻辑函数
     * @param fallbackSupplier 降级逻辑函数
     * @return 业务逻辑或降级逻辑返回值
     */
    public <T> T execute(String key, Supplier<T> supplier, Supplier<T> fallbackSupplier);
    
    /**
     * 使用指定配置执行限流操作
     * 
     * @param key      限流 key
     * @param config   限流配置
     * @param supplier 业务逻辑函数
     * @return 业务逻辑返回值
     */
    public <T> T execute(String key, RateLimitConfig config, Supplier<T> supplier);
    
    /**
     * 使用指定配置执行限流操作（支持降级）
     * 
     * @param key              限流 key
     * @param config           限流配置
     * @param supplier         业务逻辑函数
     * @param fallbackSupplier 降级逻辑函数
     * @return 业务逻辑或降级逻辑返回值
     */
    public <T> T execute(String key, RateLimitConfig config,
                         Supplier<T> supplier, Supplier<T> fallbackSupplier);
    
    /**
     * 带超时的限流操作
     * 限流时等待直到获取令牌或超时
     * 
     * @param key      限流 key
     * @param config   限流配置
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @param supplier 业务逻辑函数
     * @return 业务逻辑返回值
     */
    public <T> T executeWithTimeout(String key, RateLimitConfig config,
                                    long timeout, TimeUnit timeUnit, Supplier<T> supplier);
    
    // ========== 快速创建配置方法 ==========
    
    /**
     * 创建令牌桶配置
     * 
     * @param capacity   桶容量
     * @param refillRate 补充速率
     * @param timeUnit   时间单位
     */
    public RateLimitConfig createTokenBucketConfig(int capacity, int refillRate, TimeUnit timeUnit);
    
    /**
     * 创建固定窗口配置
     * 
     * @param windowSize  窗口大小（秒）
     * @param maxRequests 最大请求数
     */
    public RateLimitConfig createFixedWindowConfig(int windowSize, int maxRequests);
    
    /**
     * 创建滑动窗口配置
     * 
     * @param windowSize  窗口大小（秒）
     * @param maxRequests 最大请求数
     */
    public RateLimitConfig createSlidingWindowConfig(int windowSize, int maxRequests);
}
```

### RedisSequenceTemplate 序列号生成

```java
public class RedisSequenceTemplate {
    
    /**
     * 生成序列号（默认格式）
     * 格式：前缀 + 时间(yyyyMMddHHmmss) + 序列号 + 随机数
     * 
     * @param redisKey       Redis 缓存 key
     * @param prefix         业务前缀
     * @param sequenceLength 序列号长度
     * @return 序列号字符串
     */
    public String generate(String redisKey, String prefix, int sequenceLength);
    
    /**
     * 生成序列号（自定义格式）
     * 
     * @param redisKey       Redis 缓存 key
     * @param prefix         业务前缀
     * @param pattern        日期格式
     * @param sequenceLength 序列号长度
     * @param randomLength   随机数长度
     * @return 序列号字符串
     */
    public String generate(String redisKey, String prefix, String pattern, int sequenceLength, int randomLength);
    
    /**
     * 生成序列号（带过期时间）
     * 
     * @param redisKey       Redis 缓存 key
     * @param prefix         业务前缀
     * @param pattern        日期格式
     * @param sequenceLength 序列号长度
     * @param randomLength   随机数长度
     * @param expire         过期时间
     * @param timeUnit       过期时间单位
     * @return 序列号字符串
     */
    public String generate(String redisKey, String prefix, String pattern, 
                           int sequenceLength, int randomLength, 
                           long expire, TimeUnit timeUnit);
    
    /**
     * 生成当天序列号（当天 23:59:59 过期）
     * 
     * @param redisKey Redis 缓存 key
     * @param prefix   业务前缀
     * @param len      序列号长度
     * @return 序列号字符串
     */
    public String genNowSerialNumber(String redisKey, String prefix, int len);
}
```

### RedisGeoTemplate 地理位置

支持 WGS-84、GCJ-02、BD-09 三种坐标系，可通过全局配置或方法参数指定。

#### 全局坐标系配置

```yaml
spring:
  redis:
    geo:
      coordinate-system: GCJ02  # 可选值：WGS84（默认）、GCJ02、BD09
```

#### 核心 API

```java
public class RedisGeoTemplate {

    // ========== 写入操作 ==========

    /**
     * 添加地理位置（使用全局坐标系）
     */
    public Long add(String key, double longitude, double latitude, String member);

    /**
     * 添加地理位置（指定坐标系）
     * 输入坐标会自动转换为 WGS-84 存储
     */
    public Long add(String key, double longitude, double latitude, String member,
                    CoordinateSystem coordinateSystem);

    /**
     * 批量添加地理位置（使用全局坐标系）
     */
    public Long addAll(String key, Map<String, Point> members);

    /**
     * 批量添加地理位置（指定坐标系）
     */
    public Long addAll(String key, Map<String, Point> members, CoordinateSystem coordinateSystem);

    // ========== 查询操作 ==========

    /**
     * 获取地理位置（使用全局坐标系）
     */
    public Point get(String key, String member);

    /**
     * 获取地理位置（指定输出坐标系）
     * 返回坐标已从 WGS-84 转换为目标坐标系
     */
    public Point get(String key, String member, CoordinateSystem coordinateSystem);

    /**
     * 计算两个成员之间的距离（距离计算不受坐标系影响）
     */
    public Distance distance(String key, String member1, String member2, Metric metric);

    /**
     * 搜索指定成员附近的位置（使用全局坐标系）
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> radius(
            String key, String member, double radius, Metric metric, long limit);

    /**
     * 搜索指定成员附近的位置（指定坐标系）
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> radius(
            String key, String member, double radius, Metric metric, long limit,
            CoordinateSystem coordinateSystem);

    /**
     * 根据坐标搜索附近的位置（使用全局坐标系）
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> radius(
            String key, double longitude, double latitude,
            double radius, Metric metric, long limit);

    /**
     * 根据坐标搜索附近的位置（指定坐标系）
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> radius(
            String key, double longitude, double latitude,
            double radius, Metric metric, long limit,
            CoordinateSystem coordinateSystem);

    // ========== 其他操作 ==========

    /**
     * 获取 GEO Hash
     */
    public List<String> hash(String key, String... members);

    /**
     * 删除地理位置成员
     */
    public Long remove(String key, String... members);
}
```

#### 坐标系说明

| 坐标系 | 适用场景 | 说明 |
|--------|----------|------|
| `WGS84` | GPS、国际地图（Google Maps 国际版） | Redis GEO 底层存储坐标系，默认值 |
| `GCJ02` | 高德地图、腾讯地图、Google Maps 中国版 | 国测局坐标系，又称"火星坐标系" |
| `BD09` | 百度地图 | 百度在 GCJ02 基础上的二次加密 |

### RedisMessageQueueTemplate 消息队列（Stream）

基于 Redis Stream 的轻量级消息队列，支持注解驱动消费者、并发消费、Pending 回收、死信队列。

#### 启用配置

```yaml
silky:
  redis:
    stream:
      enabled: true                    # 必须，默认 false
      default-group: silky-default-group  # 全局默认消费者组名
      consumer:
        batch-size: 10                 # 每次 XREADGROUP 拉取最大消息数
        poll-timeout-ms: 2000          # 阻塞读取超时（毫秒）
        max-retries: 3                 # 最大重试次数（超过后进入死信队列）
        pending-timeout-ms: 30000      # Pending 消息超时阈值（毫秒）
        concurrency: 1                 # 每个 streamKey 默认并发消费者数量
        thread-pool:
          core-size: 5
          max-size: 20
          queue-capacity: 100
          thread-name-prefix: silky-stream-
      dead-letter:
        enabled: true                  # 是否启用死信队列
        suffix: ":dead"                # 死信 Stream Key 后缀
      monitor:
        enabled: true                  # 是否启用慢消费监控
        slow-threshold-ms: 500         # 慢消费告警阈值（毫秒）
```

#### 生产者 API

```java
public class RedisMessageQueueTemplate {

    /**
     * 发送消息到指定 Stream
     *
     * @param streamKey Stream Key（队列名称）
     * @param payload   业务对象（自动序列化为 JSON）
     * @return Redis 生成的消息 ID（格式：timestamp-sequence）
     */
    public String send(String streamKey, Object payload);

    /**
     * 发送消息并携带扩展 Headers
     *
     * @param streamKey Stream Key
     * @param payload   业务对象
     * @param headers   扩展属性（如 traceId、bizType 等）
     * @return 消息 ID
     */
    public String send(String streamKey, Object payload, Map<String, String> headers);
}
```

#### StreamMessage 消息体

```java
public class StreamMessage {

    /** Redis 消息 ID（格式：timestamp-sequence） */
    private String id;
    /** Stream Key（队列名称） */
    private String streamKey;
    /** 消费者组名 */
    private String groupId;
    /** 原始 payload JSON 字符串 */
    private String payloadJson;
    /** 扩展属性（可存储 traceId、bizType 等） */
    private Map<String, String> headers;
    /** 消息发送时间戳（毫秒） */
    private long timestamp;
    /** 已投递次数（从 XPENDING 解析，首次消费为 1） */
    private long deliveryCount;

    /**
     * 反序列化 payload 为指定类型
     */
    public <T> T getPayload(Class<T> clazz);

    /**
     * 获取 / 设置 Header
     */
    public String getHeader(String key);
    public void putHeader(String key, String value);
}
```

#### MessageRepository SPI（可选实现）

用户可注入自定义 `MessageRepository` 实现，在消息生命周期的关键节点执行持久化逻辑：

```java
public interface MessageRepository {

    /** 生产者发送前回调（可入库记录，返回值会存入 headers["dbId"]） */
    default String onSend(StreamMessage message) { return null; }

    /** 消费者接收到消息、业务处理前回调 */
    default void onReceive(StreamMessage message, String consumerName) {}

    /** 业务处理成功且 ACK 之后回调 */
    default void onSuccess(StreamMessage message, Object result) {}

    /** 业务处理失败回调，返回 true 继续重试，false 放入死信队列 */
    default boolean onFailure(StreamMessage message, Exception exception, int retryCount) { return true; }

    /** 消息进入死信队列时回调 */
    default void onDeadLetter(StreamMessage message, String reason) {}
}
```

#### MessageListenerContainerRegistry 编程式注册

```java
public class MessageListenerContainerRegistry {

    /**
     * 注册并启动消费者容器
     *
     * @param streamKey   Stream Key
     * @param groupName   消费者组名
     * @param concurrency 并发数（创建 concurrency 个消费者实例，Redis 自动分配消息）
     * @param listener    消息处理逻辑（函数式接口）
     */
    public void register(String streamKey, String groupName, int concurrency, MessageListener listener);

    /**
     * 注册消费者容器（自定义消费者名前缀）
     */
    public void register(String streamKey, String groupName, String consumerPrefix,
                         int concurrency, MessageListener listener);

    /** 停止指定 Stream 的所有消费者 */
    public void stop(String streamKey);

    /** 获取所有已注册的容器 */
    public List<DefaultStreamMessageListenerContainer> getAllContainers();
}
```

## 注解使用

### @RedisStreamListener Stream 消费者注解

标注在方法上，使该方法自动注册为 Redis Stream 消费者。方法参数必须为 `StreamMessage` 类型。

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisStreamListener {

    /** Stream Key（队列名称），必填 */
    String stream();

    /** 消费者组名，默认使用全局配置 */
    String group() default "";

    /** 消费者名称前缀，默认使用 hostname，并发时自动追加序号后缀 */
    String consumer() default "";

    /** 并发消费者数量，默认 1 */
    int concurrency() default 1;

    /** 每次拉取的最大消息数，0 表示使用全局配置 */
    int batchSize() default 0;

    /** 是否自动创建消费者组（不存在时），默认 true */
    boolean autoCreateGroup() default true;
}
```

### @RedisLock 分布式锁注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {
    
    /**
     * 锁的 key，支持 SpEL 表达式
     * 如："order:#{#orderId}"
     */
    String key();
    
    /**
     * 锁类型，默认可重入锁
     */
    LockType lockType() default LockType.REENTRANT;
    
    /**
     * 等待获取锁时间，默认 30 秒
     */
    long waitTime() default 30;
    
    /**
     * 锁自动释放时间，默认 10 秒
     */
    long leaseTime() default 10;
    
    /**
     * 时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 在事务提交后释放锁，默认 true
     */
    boolean releaseAfterTransaction() default true;
}
```

### @RateLimit 限流注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 限流 key，支持 SpEL 表达式
     */
    String key();
    
    /**
     * 限流算法，默认令牌桶
     */
    RateLimitAlgorithm algorithm() default RateLimitAlgorithm.TOKEN_BUCKET;
    
    /**
     * 令牌桶容量（令牌桶算法专用）
     */
    int capacity() default 100;
    
    /**
     * 令牌填充速率（令牌桶算法专用）
     */
    int refillRate() default 10;
    
    /**
     * 时间单位（令牌桶算法专用）
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 时间窗口大小，单位秒（固定/滑动窗口算法专用）
     */
    int windowSize() default 60;
    
    /**
     * 最大请求数（固定/滑动窗口算法专用）
     */
    int maxRequests() default 100;
    
    /**
     * 限流后的降级方法名
     */
    String fallbackMethod() default "";
    
    /**
     * 是否阻塞等待令牌
     */
    boolean block() default false;
    
    /**
     * 阻塞等待超时时间（秒）
     */
    long timeout() default 0;
}
```

### @RedisSequence 序列号注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisSequence {
    
    /**
     * Redis 缓存 key
     */
    String redisKey() default "";
    
    /**
     * 业务前缀
     */
    String prefix();
    
    /**
     * 日期格式，默认 yyyyMMdd
     */
    String datePattern() default DatePattern.PURE_DATE_PATTERN;
    
    /**
     * 序列号长度
     */
    int sequenceLength() default 6;
    
    /**
     * 随机数长度
     */
    int randomLength() default 3;
    
    /**
     * 过期时间（天），默认 1 天
     */
    int expire() default 1;
    
    /**
     * 过期时间单位，默认天
     */
    TimeUnit timeUnit() default TimeUnit.DAYS;
}
```

## 使用示例

### 缓存操作示例

```java
@Service
public class UserService {
    
    @Autowired
    private RedisCacheTemplate redisCache;
    
    // 基础缓存
    public void cacheUser(User user) {
        redisCache.setObject("user:" + user.getId(), user, 30, TimeUnit.MINUTES);
    }
    
    public User getUser(String userId) {
        return redisCache.getObject("user:" + userId);
    }
    
    // 防止缓存穿透
    public User getUserWithCache(String userId) {
        return redisCache.getOrSet(
            "user:" + userId,
            () -> userDao.findById(userId),  // 数据库查询
            30, TimeUnit.MINUTES
        );
    }
    
    // List 缓存
    public void cacheUserList(List<User> users) {
        redisCache.setList("users:all", users, 1, TimeUnit.HOURS);
    }
    
    public List<User> getUserList() {
        return redisCache.getList("users:all");
    }
    
    // Hash 缓存
    public void cacheUserMap(Map<String, User> userMap) {
        redisCache.setHash("users:map", userMap);
    }
    
    public User getUserFromMap(String userId) {
        return redisCache.getHashValue("users:map", userId);
    }
    
    // 批量操作
    public Map<String, User> getUsersByIds(List<String> userIds) {
        List<String> keys = userIds.stream()
            .map(id -> "user:" + id)
            .collect(Collectors.toList());
        return redisCache.multiGet(keys, User.class);
    }
}
```

### 分布式锁示例

```java
@Service
public class OrderService {
    
    @Autowired
    private RedisLockTemplate lockTemplate;
    
    /**
     * 创建订单（带分布式锁）
     */
    public Order createOrder(String userId, OrderRequest request) {
        String lockKey = "order:create:" + userId;
        
        return lockTemplate.lock(lockKey, 10, 30, TimeUnit.SECONDS, () -> {
            // 检查用户是否有未完成的订单
            if (hasUnfinishedOrder(userId)) {
                throw new BusinessException("您有未完成的订单");
            }
            
            // 创建订单
            Order order = new Order();
            order.setUserId(userId);
            order.setStatus(OrderStatus.CREATED);
            
            return orderDao.save(order);
        });
    }
    
    /**
     * 扣减库存（带分布式锁，事务完成后释放）
     */
    @Transactional
    public void deductStock(String productId, int quantity) {
        String lockKey = "stock:" + productId;
        
        lockTemplate.lock(lockKey, 5, 10, TimeUnit.SECONDS, true, () -> {
            // 查询库存
            Stock stock = stockDao.findByProductId(productId);
            
            // 检查库存
            if (stock.getQuantity() < quantity) {
                throw new BusinessException("库存不足");
            }
            
            // 扣减库存
            stock.setQuantity(stock.getQuantity() - quantity);
            stockDao.save(stock);
            
            return null;
        });
    }
}
```

### 分布式锁注解示例

```java
@Service
public class InventoryService {
    
    /**
     * 扣减库存（使用注解）
     */
    @RedisLock(key = "inventory:#{#productId}", waitTime = 5, leaseTime = 10)
    public void deductInventory(String productId, int quantity) {
        Inventory inventory = inventoryDao.findByProductId(productId);
        
        if (inventory.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }
        
        inventory.setStock(inventory.getStock() - quantity);
        inventoryDao.save(inventory);
    }
    
    /**
     * 用户级别的锁
     */
    @RedisLock(key = "user:operation:#{#userId}", waitTime = 3, leaseTime = 5)
    public void userOperation(String userId, OperationRequest request) {
        // 业务逻辑
    }
}
```

### 限流示例

```java
@Service
public class ApiService {
    
    @Autowired
    private RedisRateLimitTemplate rateLimitTemplate;
    
    /**
     * API 调用（带限流）
     */
    public ApiResponse callApi(String apiKey) {
        String limitKey = "api:" + apiKey;
        
        return rateLimitTemplate.execute(limitKey, () -> {
            // 实际 API 调用
            return doCallApi(apiKey);
        }, () -> {
            // 降级处理
            return ApiResponse.tooManyRequests();
        });
    }
    
    /**
     * 使用令牌桶限流
     */
    public ApiResponse callWithTokenBucket(String userId) {
        RateLimitConfig config = RateLimitConfig.tokenBucket(100, 10, TimeUnit.SECONDS);
        
        return rateLimitTemplate.execute("api:user:" + userId, config, () -> {
            return doCallApi(userId);
        });
    }
    
    /**
     * 使用固定窗口限流
     */
    public ApiResponse callWithFixedWindow(String ip) {
        RateLimitConfig config = RateLimitConfig.fixedWindow(60, 100);  // 每分钟 100 次
        
        return rateLimitTemplate.execute("api:ip:" + ip, config, () -> {
            return doCallApi(ip);
        });
    }
}
```

### 限流注解示例

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    /**
     * 用户级别限流（每分钟 60 次）
     */
    @RateLimit(key = "api:user:#{#userId}", algorithm = RateLimitAlgorithm.FIXED_WINDOW,
               windowSize = 60, maxRequests = 60)
    @GetMapping("/user/{userId}")
    public User getUser(@PathVariable String userId) {
        return userService.getById(userId);
    }
    
    /**
     * 接口级别限流（令牌桶）
     */
    @RateLimit(key = "api:order:create", algorithm = RateLimitAlgorithm.TOKEN_BUCKET,
               capacity = 100, refillRate = 10)
    @PostMapping("/order")
    public Order createOrder(@RequestBody OrderRequest request) {
        return orderService.create(request);
    }
    
    /**
     * IP 级别限流（滑动窗口）
     */
    @RateLimit(key = "api:ip:#{#request.getRemoteAddr()}", algorithm = RateLimitAlgorithm.SLIDING_WINDOW,
               windowSize = 60, maxRequests = 100)
    @GetMapping("/public")
    public String publicApi(HttpServletRequest request) {
        return "success";
    }
}
```

### 序列号生成示例

```java
@Service
public class OrderService {
    
    @Autowired
    private RedisSequenceTemplate sequenceTemplate;
    
    /**
     * 生成订单号
     * 格式：ORD20240115000001001
     */
    public String generateOrderNo() {
        return sequenceTemplate.generate(
            "seq:order",      // Redis key
            "ORD",            // 前缀
            "yyyyMMdd",       // 日期格式
            8,                // 序列号长度
            3                 // 随机数长度
        );
    }
    
    /**
     * 生成当天序列号（当天有效）
     */
    public String generateDailySerial() {
        return sequenceTemplate.genNowSerialNumber(
            "seq:daily",
            "D",
            6
        );
    }
}
```

### 序列号注解示例

```java
@Service
public class PaymentService {
    
    /**
     * 生成支付流水号
     */
    @RedisSequence(redisKey = "seq:payment", prefix = "PAY", sequenceLength = 8)
    public String generatePaymentNo() {
        // 方法返回值会被替换为生成的序列号
        return null;
    }
    
    /**
     * 自定义格式的序列号
     */
    @RedisSequence(
        redisKey = "seq:refund",
        prefix = "REF",
        datePattern = "yyyyMMddHH",
        sequenceLength = 6,
        randomLength = 2,
        expire = 1,
        timeUnit = TimeUnit.HOURS
    )
    public String generateRefundNo() {
        return null;
    }
}
```

### 地理位置示例

```java
@Service
public class LocationService {

    @Autowired
    private RedisGeoTemplate geoTemplate;

    /**
     * 添加商家位置（使用全局坐标系配置）
     */
    public void addStoreLocation(String storeId, double longitude, double latitude) {
        geoTemplate.add("stores", longitude, latitude, storeId);
    }

    /**
     * 添加商家位置（使用高德地图坐标系）
     * 输入的 GCJ02 坐标会自动转为 WGS-84 存储
     */
    public void addStoreLocationGcj02(String storeId, double longitude, double latitude) {
        geoTemplate.add("stores", longitude, latitude, storeId, CoordinateSystem.GCJ02);
    }

    /**
     * 搜索附近的商家（使用全局坐标系）
     */
    public List<Store> findNearbyStores(double longitude, double latitude, double radiusKm) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
            geoTemplate.radius("stores", longitude, latitude, radiusKm, Metrics.KILOMETERS, 10);

        List<Store> stores = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            String storeId = result.getContent().getName();
            double distance = result.getDistance().getValue();

            Store store = storeService.getById(storeId);
            store.setDistance(distance);
            stores.add(store);
        }

        return stores;
    }

    /**
     * 搜索附近的商家（高德地图场景，指定 GCJ02 坐标系）
     * 输入坐标从 GCJ02 转 WGS84 查询，返回坐标从 WGS84 转 GCJ02
     */
    public List<Store> findNearbyStoresGcj02(double longitude, double latitude, double radiusKm) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
            geoTemplate.radius("stores", longitude, latitude, radiusKm,
                    Metrics.KILOMETERS, 10, CoordinateSystem.GCJ02);

        List<Store> stores = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            String storeId = result.getContent().getName();
            double distance = result.getDistance().getValue();
            // result.getContent().getPoint() 返回的坐标已为 GCJ02

            Store store = storeService.getById(storeId);
            store.setDistance(distance);
            stores.add(store);
        }

        return stores;
    }

    /**
     * 获取位置坐标并转换为指定坐标系
     */
    public Point getStorePoint(String storeId, CoordinateSystem cs) {
        return geoTemplate.get("stores", storeId, cs);
    }

    /**
     * 计算两地距离（距离计算不受坐标系影响）
     */
    public double calculateDistance(String storeId1, String storeId2) {
        Distance distance = geoTemplate.distance("stores", storeId1, storeId2, Metrics.KILOMETERS);
        return distance.getValue();
    }
}
```

### Stream 消息队列示例

#### 发送消息

```java
@Service
public class OrderService {

    @Autowired
    private RedisMessageQueueTemplate messageQueue;

    /**
     * 发送订单消息
     */
    public String sendOrderEvent(Order order) {
        return messageQueue.send("order-stream", order);
    }

    /**
     * 发送消息并携带追踪信息
     */
    public String sendOrderEventWithTrace(Order order, String traceId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("traceId", traceId);
        headers.put("bizType", "ORDER_CREATE");
        return messageQueue.send("order-stream", order, headers);
    }
}
```

#### 注解驱动消费（推荐）

```java
@Component
public class OrderStreamConsumer {

    /**
     * 消费订单消息（单消费者）
     * 方法正常返回 → 框架自动 ACK
     * 方法抛出异常 → 框架触发重试或死信流程
     */
    @RedisStreamListener(stream = "order-stream", group = "order-group")
    public void onOrderMessage(StreamMessage message) {
        Order order = message.getPayload(Order.class);
        String traceId = message.getHeader("traceId");
        log.info("收到订单消息: orderNo={}, traceId={}", order.getOrderNo(), traceId);
        // 处理业务...
    }

    /**
     * 并发消费（3 个消费者实例共享同一个 Consumer Group）
     */
    @RedisStreamListener(stream = "order-stream", group = "order-group", concurrency = 3)
    public void onConcurrentOrderMessage(StreamMessage message) {
        Order order = message.getPayload(Order.class);
        // 并发处理...
    }

    /**
     * 处理失败超过 maxRetries 次后，消息将自动进入死信队列 "order-stream:dead"
     */
    @RedisStreamListener(stream = "order-stream", group = "order-group")
    public void onFailOrderMessage(StreamMessage message) {
        Order order = message.getPayload(Order.class);
        if (order.getAmount() <= 0) {
            throw new BusinessException("订单金额异常");
        }
        // 处理业务...
    }
}
```

#### 编程式消费

```java
@Configuration
public class StreamConsumerConfig {

    @Autowired
    private MessageListenerContainerRegistry registry;

    @PostConstruct
    public void registerConsumers() {
        // 编程式注册消费者（3 并发）
        registry.register("order-stream", "order-group", 3, message -> {
            Order order = message.getPayload(Order.class);
            // 处理业务...
            return null;
        });
    }
}
```

#### 自定义持久化（MessageRepository）

```java
@Component
public class OrderMessageRepository implements MessageRepository {

    @Autowired
    private OrderMessageLogMapper logMapper;

    @Override
    public String onSend(StreamMessage message) {
        // 发送前入库记录
        OrderMessageLog log = new OrderMessageLog();
        log.setPayload(message.getPayloadJson());
        log.setStatus("SENT");
        logMapper.insert(log);
        return String.valueOf(log.getId());  // 返回 dbId，框架自动存入 headers
    }

    @Override
    public void onSuccess(StreamMessage message, Object result) {
        // 消费成功后更新状态
        String dbId = message.getHeader("dbId");
        logMapper.updateStatus(Long.parseLong(dbId), "CONSUMED");
    }

    @Override
    public void onDeadLetter(StreamMessage message, String reason) {
        // 死信消息告警
        log.error("消息进入死信: stream={}, id={}, reason={}",
                message.getStreamKey(), message.getId(), reason);
    }
}
```

## 常见问题

### Q1: 分布式锁的看门狗机制？

A: 组件基于 Redisson 实现，默认启用看门狗机制。如果业务执行时间超过锁的租期时间，看门狗会自动续期，直到业务完成。

```java
// 设置 leaseTime 为 -1 启用看门狗
lockTemplate.lock("myKey", 10, -1, TimeUnit.SECONDS, supplier);
```

### Q2: 限流算法如何选择？

A: 
- **令牌桶**：适合平滑流量，允许突发流量
- **固定窗口**：实现简单，但可能出现临界突发
- **滑动窗口**：精度高，避免临界问题，但计算开销大

### Q3: 序列号生成会重复吗？

A: 基于 Redis 原子自增操作，不会重复。但需要注意：
- 序列号长度要足够（建议 6 位以上）
- 合理设置过期时间，避免 Redis 内存溢出

### Q4: 如何监控 Redis 操作？

A: 可以通过 Spring Boot Actuator 监控 Redis 连接池状态，或自定义拦截器记录操作日志。

### Q5: Redis Stream 消费失败如何处理？

A: 框架内置了完整的失败处理机制：
- 消费异常后消息不 ACK，留在 Pending Entry List（PEL）中
- 超过 `pending-timeout-ms` 后，框架自动回收并重新投递
- 重试次数超过 `max-retries` 后，消息转入死信队列（`{streamKey}:dead`）
- 可通过 `MessageRepository.onFailure` 自定义重试策略，返回 `false` 可提前放弃重试

### Q6: Redis Stream 支持并发消费吗？

A: 支持。通过 `@RedisStreamListener(concurrency = 3)` 注解或编程式 `registry.register(stream, group, 3, listener)` 配置，多个消费者实例共享同一个 Consumer Group，Redis 自动分配消息，保证同一条消息不会被重复消费。

## 相关链接

- [GitHub 仓库](https://github.com/yijuanmao/silky-starter)
- [Gitee 仓库](https://gitee.com/zeng_er/silky-starter)
- [Redisson 文档](https://redisson.org/documentation.html)
- [Redis 命令参考](https://redis.io/commands)

## License

Apache License 2.0
