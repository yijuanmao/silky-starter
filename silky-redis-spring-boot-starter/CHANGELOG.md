## v1.0.7 (2026-05-07)

### 🐛 Bug Fixes

* **RedisGeoTemplate**: 彻底修复 `radius()` 搜索距离全为 0 的问题（Issue #7）
  - 改用 `StringRedisTemplate` 执行 Geo 操作，避免 `FastJson2RedisSerializer` 对 Geo 反序列化结果的干扰
  - 泛型从 `GeoLocation<Object>` 变更为 `GeoLocation<String>`
  - 关联 Issue：[#7](https://github.com/yijuanmao/silky-starter/issues/7)

### ✨ 新功能

* **坐标系支持**: `RedisGeoTemplate` 新增 WGS-84、GCJ-02、BD-09 三种坐标系支持
  - 新增 `CoordinateSystem` 枚举和 `CoordinateUtils` 坐标转换工具类
  - 新增 `GeoProperties` 配置属性，支持全局配置 `spring.redis.geo.coordinate-system`
  - 所有读写方法支持通过 `CoordinateSystem` 参数单独指定坐标系，覆盖全局配置
  - 写入时自动将输入坐标转为 WGS-84 存储，读取时自动将 WGS-84 转为目标坐标系返回
  - 适用场景：高德地图(GCJ02)、腾讯地图(GCJ02)、百度地图(BD09)、国际地图(WGS84)

* **RedisGeoTemplate** 新增 `remove(String key, String... members)` 方法

### ⚠️ Breaking Changes

* **RedisGeoTemplate**: 底层从 `RedisTemplate<String, Object>` 切换为 `StringRedisTemplate`
  - 泛型签名变更：`GeoLocation<Object>` → `GeoLocation<String>`
  - 如已有代码使用了 `location.getName().toString()`，可直接改为 `location.getName()`（已为 String）

## v1.0.6 (2026-05-06)

### 🐛 Bug Fixes

* **FastJson2RedisSerializer**: 修复 `RedisGeoTemplate.radius()` 使用 FastJson2 序列化时 Distance 距离全为 0 的问题
  - 去除 `WriteClassName` 和 `SupportAutoType`，避免 `@type` 标记干扰 Spring Data Redis 内部的 `GeoResultsConverterFactory` 解析
  - 关联 Issue：[#7](https://github.com/yijuanmao/silky-starter/issues/7)

### 💎 功能优化

* **RedisCacheTemplate**: 新增带类型参数的获取方法，确保去除 `WriteClassName` 后反序列化类型正确
  - `getObject(String key, Class<T> clazz)`
  - `getList(String key, Class<T> clazz)`
  - `getListRange(String key, long start, long end, Class<T> clazz)`
  - `getSet(String key, Class<T> clazz)`
  - `getHashValue(String key, String hKey, Class<T> clazz)`
  - `getMultiHashValue(String key, Collection<String> hKeys, Class<T> clazz)`

### ⚠️ Breaking Changes

* **FastJson2RedisSerializer**: 序列化不再写入 `@type` 类名标记，反序列化不再启用 `SupportAutoType`
  - 如需类型安全的反序列化，请使用 `redisCacheTemplate.getObject(key, YourClass.class)` 替代 `redisCacheTemplate.getObject(key)`

## v1.0.5 (2026-04-28)
### 💎 功能优化
* hutool工具类 =》 5.8.44版本


