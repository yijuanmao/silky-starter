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


