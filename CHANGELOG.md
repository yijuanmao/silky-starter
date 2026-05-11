
## 2026-05-07
### ⚡ 重要变更
* **groupId 迁移**: `io.github.yijuanmao` → `top.silky`，涉及所有模块 pom.xml 及文档

### 💎 功能优化
* **silky-redis-spring-boot-starter**: v1.0.6 => v1.0.7
  - 新增 Redis Stream 消息队列模块（`@RedisStreamListener` 注解驱动消费者、`RedisMessageQueueTemplate` 生产者、Consumer Group 自动创建、Pending 回收、死信队列、`MessageRepository` SPI 持久化）
* **silky-excel-spring-boot-starter**: v1.0.5 => v1.0.6
  - 新增 `@ExcelEnum`、`@ExcelDict`、`@ExcelMask` 注解驱动字段转换
  - 支持多 Sheet 导出
  - StorageStrategy 返回 StorageObject
  - maxRowsPerSheet 参数校验
* **silky-redis-spring-boot-starter**: v1.0.5 => v1.0.6
  - 修复 GEO `radius()` Distance 为 0 的问题（改用 StringRedisTemplate，Issue #7）
  - 新增 WGS-84 / GCJ-02 / BD-09 坐标系转换支持（`CoordinateSystem`、`CoordinateUtils`、`GeoProperties`）
  - 新增 `RedisGeoTemplate.remove()` 方法
  - 新增 `RedisCacheTemplate` 带类型参数的获取方法（getObject/getList/getSet/getHashValue 等）
  - 优化 `FastJson2RedisSerializer`，去除 WriteClassName 和 SupportAutoType
  - 完善 Redis 缓存模板功能（RedisCacheException、RedisValueUtils）
* **silky-rabbitmq-spring-boot-starter**: v1.0.6 => v1.0.7
  - 完善消息消费失败时的可观测性，确保开启/关闭死信队列场景下统计一致性（Issue #6）

### ⚠️ Breaking Changes
* **RedisGeoTemplate**: 泛型 `GeoLocation<Object>` → `GeoLocation<String>`，底层切换为 StringRedisTemplate
* **FastJson2RedisSerializer**: 不再写入 `@type` 类名标记，反序列化请使用带 Class 参数的重载方法

## 2026-04-29
### 💎 功能优化
* 增加 `silky-starter-bom`模块

### 🐛 Bug Fixes

- **silky-rabbitmq**: 修复 `RabbitMQListenerContainer#sendToDeadLetterExchange()` 死信队列发送消息，序列化异常
    - 关联 Issue：[#5](https://github.com/yijuanmao/silky-starter/issues/5)


## 2026-04-28
### 💎 功能优化
* 所有组件 hutool工具类 =》 5.8.44版本

### 🐛 Bug Fixes

- **silky-rabbitmq**: 修复 `SimpleRabbitListenerContainerFactory` 未加载 yml 手动确认配置，导致重复消费及 `PRECONDITION_FAILED - unknown delivery tag` 报错
    - 关联 Issue：[#4](https://github.com/yijuanmao/silky-starter/issues/4)


## 2026-03-14
### 💎 功能优化
* 组件的pom版本统一维护改为单独版本管理
* silky-rabbitmq-spring-boot-starter：添加RabbitPayload注解用于标记消息体参数重构消息发送方法，支持延迟消息和异步发送等


## 2026-03-11
### 💎 功能优化
* silky-excel-spring-boot-starter pom文件指定 silky-starter-core版本
* silky-mongodb-spring-boot-starter pom文件指定 silky-starter-core版本
* silky-oss-spring-boot-starter pom文件指定 silky-starter-core版本
* silky-rabbitmq-spring-boot-starter pom文件指定 silky-starter-core版本
* silky-redis-spring-boot-starter pom文件指定 silky-starter-core版本
* silky-statemachine-spring-boot-starter pom文件指定 silky-starter-core版本
* silky-mongodb-spring-boot-starter 组件重构
* hutool-all pom版本5.8.39 => 5.8.41


## 2025-10-22
### ✨ 新特性

* 新增 silky-redis-spring-boot-starter redis组件模块
* 新增 silky-excel-spring-boot-starter excel大文件导出组件模块

### 💎 功能优化
* spring boot pom版本2.7.7 => 2.7.8


## 2025-10-13
### ✨ 新特性

* 新增 silky-rabbitmq-spring-boot-starter rabbitmq组件模块

### 💎 功能优化
* 优化 silky-statemachine-spring-boot-starter resources目录下没有配置文件AutoConfiguration文件
* silky-starter-core 增加logback-classic日志实现框架
* alibaba.fastjson2 pom版本2.0.58 => 2.0.59

## 2025-10-12

### ✨ 新特性

* 新增 silky-statemachine-spring-boot-starter 状态机组件
* 新增 silky-oss-spring-boot-starter oss 组件
* 新增 silky-mongodb-spring-boot-starter mongodb 组件
