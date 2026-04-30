# silky-rabbitmq-spring-boot-starter 使用手册

> **版本**：v1.0.4  
> **更新日期**：2026-03-16  
> **开源仓库**：[GitHub](https://github.com/yijuanmao/silky-starter) | [Gitee](https://gitee.com/zeng_er/silky-starter)

---

## 目录

1. [简介](#一简介)
2. [快速开始](#二快速开始)
    - [引入依赖](#21-引入依赖)
    - [配置文件](#22-配置文件)
3. [消息发送](#三消息发送)
    - [使用 SkRabbitMqTemplate 发送](#31-使用-skrabbitmqtemplate-发送)
    - [使用 @RabbitMessage 注解发送](#32-使用-rabbitmessage-注解发送)
4. [消息监听](#四消息监听)
    - [原生 @RabbitListener 监听](#41-原生-rabbitlistener-监听)
    - [继承 AbstractRabbitMQListener 监听](#42-继承-abstractrabbitmqlistener-监听)
    - [消费端重试与死信队列](#43-消费端重试与死信队列)
5. [消息持久化](#五消息持久化)
6. [序列化机制](#六序列化机制)
7. [配置属性参考](#七配置属性参考)
8. [核心架构解析](#八核心架构解析)
9. [API 速查表](#九api-速查表)
10. [变更日志](#十变更日志)

---

## 一、简介

`silky-rabbitmq-spring-boot-starter` 是一款基于 Spring Boot 的 RabbitMQ 增强组件，旨在简化消息队列的集成与使用。它在 Spring AMQP 原生能力基础上提供了一系列开箱即用的增强特性：

| 能力 | 说明 |
|---|---|
| **多种发送方式** | 模板类调用 + 注解驱动，支持同步/异步/延迟发送 |
| **消息头元数据** | 自动将业务类型、描述、来源等写入消息头 |
| **智能重试** | 发送端与消费端均支持可配置的重试机制 |
| **死信队列** | 消费失败超过阈值后自动转发到 DLX，附加完整链路信息 |
| **消息持久化** | 提供持久化接口，可接入 DB/Redis 等任意存储 |
| **高性能序列化** | 内置 FastJson2，默认忽略 null 字段，消息体更小 |
| **自动装配** | Spring Boot 自动配置，零侵入启动 |

**依赖关系：**

```
silky-rabbitmq-spring-boot-starter
├── spring-boot-starter-amqp
├── spring-boot-starter-aop
├── spring-boot-autoconfigure
└── silky-starter-core（公共模块）
```

---

## 二、快速开始

### 2.1 引入依赖

在 `pom.xml` 中添加以下依赖：

```xml
<dependency>
    <groupId>top.silky</groupId>
    <artifactId>silky-rabbitmq-spring-boot-starter</artifactId>
    <version>1.0.4</version>
</dependency>
```

> 请前往 [GitHub Releases](https://github.com/yijuanmao/silky-starter/releases) 或 [Maven Central](https://central.sonatype.com/) 查看最新版本。

### 2.2 配置文件

在 `application.yml` 中添加 RabbitMQ 连接配置及 Silky 组件配置：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin
    virtual-host: /

    # Silky 组件配置（前缀 spring.rabbitmq.silky）
    silky:
      enabled: true                          # 是否启用 Silky 组件（默认 true）
      send:
        enabled: true                        # 启用增强发送功能
        default-send-mode: SYNC              # 默认发送模式: SYNC | ASYNC | AUTO
        sync-timeout: 5000                   # 同步发送超时时间（ms）
        async-thread-pool-size: 10           # 异步线程池大小
        enable-retry: true                   # 启用发送端重试
        max-retry-count: 3                   # 最大重试次数
        retry-interval: 1000                 # 重试间隔（ms）
        use-timeout: true                    # 是否启用超时控制
      persistence:
        enabled: false                       # 是否启用消息持久化（默认 false）

    # 监听器配置
    listener:
      silky:
        enable-dlx: false                    # 是否启用死信队列（默认 false）
        dlx-exchange: silky.default.dlx.exchange
        dlx-routing-key: silky.default.dlx.routingKey
        dlx-message-ttl: 0                   # 死信消息 TTL（ms，0=不过期）
      simple:
        acknowledge-mode: manual             # 确认模式: auto | manual
        default-requeue-rejected: false      # 拒绝消息时是否重入队
        concurrency: 1                       # 消费端并发线程数
        max-concurrency: 20                  # 最大并发线程数
        prefetch: 10                         # 每次拉取消息数量
        retry:
          enabled: true                      # 启用消费端重试
          max-attempts: 5                    # 最大重试次数
          initial-interval: 3000             # 重试初始间隔（ms）
```

> **自动装配说明：** 组件通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 声明了 `SilkyRabbitMQAutoConfiguration`，无需任何额外注解，引入依赖即可自动生效。

---

## 三、消息发送

### 3.1 使用 SkRabbitMqTemplate 发送

注入 `SkRabbitMqTemplate` 后即可调用各种发送方法：

```java
@Autowired
private SkRabbitMqTemplate skRabbitMqTemplate;
```

#### 3.1.1 普通发送

```java
TradeOrder order = new TradeOrder(1L, LocalDateTime.now(), "订单备注", BigDecimal.TEN);

// 使用默认发送模式（由配置决定）
SendResult result = skRabbitMqTemplate.send("example-exchange", "example-routingKey", order);
log.info("发送结果: {}", result);
```

#### 3.1.2 指定发送模式

```java
// 强制同步发送
SendResult result = skRabbitMqTemplate.send(
    "example-exchange", "example-routingKey", order, SendMode.SYNC
);

// 强制异步发送（立即返回，结果通过 Future 等待）
SendResult result = skRabbitMqTemplate.send(
    "example-exchange", "example-routingKey", order, SendMode.ASYNC
);
```

#### 3.1.3 携带业务元数据

```java
SendResult result = skRabbitMqTemplate.send(
    "example-exchange",
    "example-routingKey",
    order,
    "TRADE",           // businessType（写入消息头）
    "订单创建事件",      // description（写入消息头）
    SendMode.SYNC
);
```

#### 3.1.4 使用 MassageSendParam 构建发送参数

```java
MassageSendParam param = MassageSendParam.builder()
    .body(order)
    .messageId(IdUtil.fastSimpleUUID())       // 自定义消息 ID（不填则自动生成 UUID）
    .exchange("example-exchange")
    .routingKey("example-routingKey")
    .sendDelay(false)
    .sendMode(SendMode.SYNC)
    .businessType("TRADE")
    .description("订单创建事件")
    .source("order-service")                 // 消息来源标识
    .extData(Map.of("orderId", "12345"))      // 扩展数据
    .build();

SendResult result = skRabbitMqTemplate.send(param);
```

`MassageSendParam` 字段说明：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|:---:|---|---|
| `body` | Object | ✅ | - | 消息体（任意对象） |
| `messageId` | String | ❌ | UUID | 消息唯一标识 |
| `exchange` | String | ✅ | - | 交换机名称 |
| `routingKey` | String | ✅ | - | 路由键 |
| `sendDelay` | boolean | ❌ | false | 是否延迟发送 |
| `delayMillis` | Long | 延迟时必填 | - | 延迟毫秒数 |
| `sendMode` | SendMode | ❌ | AUTO | 发送模式 |
| `businessType` | String | ❌ | - | 业务类型（写入消息头） |
| `description` | String | ❌ | - | 消息描述（写入消息头） |
| `source` | String | ❌ | - | 消息来源（写入消息头） |
| `extData` | Map | ❌ | - | 扩展属性（写入消息头） |

#### 3.1.5 发送延迟消息

> ⚠️ 需要 RabbitMQ 安装 `rabbitmq_delayed_message_exchange` 插件

```java
SendResult result = skRabbitMqTemplate.sendDelay(
    "example-delay-exchange",
    "example-delay-routingKey",
    order,
    7000L,              // 延迟 7 秒
    "TRADE",
    "延迟创建订单"
);
```

#### 3.1.6 异步发送（不关注结果）

```java
// 不关注发送结果
skRabbitMqTemplate.sendAsync("example-exchange", "example-routingKey", order);
```

#### 3.1.7 异步发送（带回调）

```java
skRabbitMqTemplate.sendAsync(
    "example-exchange",
    "example-routingKey",
    order,
    new SendCallback() {
        @Override
        public void onSuccess(SendResult result) {
            log.info("发送成功: messageId={}, costTime={}ms",
                result.getMessageId(), result.getCostTime());
        }
        @Override
        public void onFailure(SendResult result) {
            log.error("发送失败: {}", result.getErrorMessage());
        }
    }
);
```

也可以将回调类定义为 Spring Bean：

```java
@Slf4j
@Component
public class TradeOrderSendCallback implements SendCallback {
    @Override
    public void onSuccess(SendResult result) {
        log.info("订单消息发送成功: {}", result);
    }

    @Override
    public void onFailure(SendResult result) {
        log.error("订单消息发送失败: {}", result);
        // 可在此处实现告警、补偿等逻辑
    }
}
```

#### 3.1.8 发送结果 SendResult

所有发送方法均返回 `SendResult` 对象：

| 字段 | 类型 | 说明 |
|---|---|---|
| `success` | boolean | 是否发送成功 |
| `messageId` | String | 消息唯一 ID |
| `sendTime` | LocalDateTime | 发送时间 |
| `costTime` | long | 发送耗时（毫秒） |
| `errorMessage` | String | 失败原因（成功时为 null） |
| `correlationData` | Object | 相关联数据 |

---

### 3.2 使用 @RabbitMessage 注解发送

在 Spring Bean 的方法上添加 `@RabbitMessage`，在参数上添加 `@RabbitPayload` 标记消息体，切面会在方法执行前自动发送消息：

```java
@Slf4j
@Service
public class TradeOrderService {

    @RabbitMessage(
        exchange = "example-exchange",
        routingKey = "example-routingKey",
        businessType = "TRADE",
        description = "订单创建事件",
        sendMode = SendMode.SYNC,
        delay = 0,               // 延迟毫秒数，> 0 则发送延迟消息
        throwOnFailure = true,   // 发送失败时是否抛出异常（默认 true）
        retryCount = 2,          // AOP 层重试次数（独立于模板层重试）
        retryInterval = 500      // AOP 层重试间隔（ms）
    )
    public void createOrder(@RabbitPayload TradeOrder order) {
        // 业务逻辑正常执行
        log.info("创建订单: {}", order);
    }
}
```

**注解参数说明：**

| 参数 | 类型 | 默认值 | 说明 |
|---|---|:---:|---|
| `exchange` | String | 必填 | 交换机名称 |
| `routingKey` | String | 必填 | 路由键 |
| `businessType` | String | `""` | 业务类型（写入消息头） |
| `description` | String | `""` | 消息描述（写入消息头） |
| `sendMode` | SendMode | `AUTO` | 发送模式 |
| `delay` | long | `0` | 延迟毫秒数（> 0 则发送延迟消息） |
| `throwOnFailure` | boolean | `true` | 发送失败时是否抛出异常 |
| `enableFailureCallback` | boolean | `false` | 是否启用失败回调钩子 |
| `retryCount` | int | `0` | AOP 层重试次数 |
| `retryInterval` | long | `1000` | AOP 层重试间隔（ms） |

**注解执行流程：**

```
@RabbitMessage 方法调用
        ↓
[切面] 解析 @RabbitPayload 参数（找不到则回退为 MassageSendParam 类型参数）
        ↓
[切面] 调用 sendMessageWithRetry()（支持 retryCount 次重试）
        ↓
[切面] 持久化：更新消息发送结果（如已启用持久化）
        ↓
发送失败 → throwOnFailure=true → 抛出 RabbitMessageSendException
          → enableFailureCallback=true → 触发失败回调
        ↓
[原方法] 继续执行业务逻辑（joinPoint.proceed()）
```

> **注意：** 注解切面会在方法执行**之前**完成消息发送，然后再继续执行原始业务方法。

---

## 四、消息监听

### 4.1 原生 @RabbitListener 监听

兼容 Spring AMQP 原生用法，Silky 会自动为 `@RabbitListener` 方法注入 FastJson2 序列化器：

```java
@Component
public class NativeOrderListener {

    @RabbitListener(queues = "example-order-queue")
    public void onMessage(
        TradeOrder order,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        log.info("收到订单消息: {}", order);
        // 业务处理
        channel.basicAck(deliveryTag, false); // 手动确认
    }
}
```

#### 获取消息头元数据

```java
@RabbitListener(queues = "example-order-queue")
public void onMessageWithHeaders(
    TradeOrder order,
    @Header(SkAmqpHeaders.BUSINESS_TYPE) String businessType,
    @Header(SkAmqpHeaders.DESCRIPTION) String description,
    @Header(SkAmqpHeaders.SOURCE) String source
) {
    log.info("业务类型: {}, 描述: {}, 来源: {}", businessType, description, source);
    // 业务处理
}
```

**Silky 扩展消息头常量（`SkAmqpHeaders`）：**

| 常量 | 头 Key | 说明 |
|---|---|---|
| `SkAmqpHeaders.BUSINESS_TYPE` | `businessType` | 业务类型 |
| `SkAmqpHeaders.DESCRIPTION` | `description` | 消息描述 |
| `SkAmqpHeaders.SOURCE` | `source` | 消息来源 |
| `SkAmqpHeaders.EXT_DATA` | `extData` | 扩展数据 |

---

### 4.2 继承 AbstractRabbitMQListener 监听

这是 Silky 推荐的监听方式，继承 `AbstractRabbitMQListener<T>` 后，监听器会**自动注册**到 `ListenerRegistry`，由 `RabbitMQListenerContainer` 统一调度处理：

```java
@Slf4j
@Component
public class OrderQueueListener extends AbstractRabbitMQListener<TradeOrder> {

    public OrderQueueListener() {
        super("example-order-queue"); // 指定监听的队列名
    }

    @Override
    public void onMessage(TradeOrder order, Channel channel, Message amqpMessage) {
        log.info("收到订单消息: {}", order);

        // --- 业务处理 ---
        processOrder(order);

        // 注意：AbstractRabbitMQListener 模式下，容器会自动处理 Ack/Nack
        // 正常返回 → 自动 basicAck
        // 抛出异常 → 触发重试机制
    }

    private void processOrder(TradeOrder order) {
        // 业务逻辑...
    }
}
```

**泛型消息类型自动解析：** 基类构造函数通过反射 `ParameterizedType` 在实例化时自动提取泛型参数 `T`，无需手动指定类型。

**Ack 机制说明：**

| 情况 | 处理结果 |
|---|---|
| 方法正常返回 | 容器自动调用 `basicAck` |
| 方法抛出异常 + 未达到最大重试次数 | `basicReject(requeue=true)` 重入队重试 |
| 方法抛出异常 + 达到最大重试次数 + `enableDlx=true` | 发送到死信交换机 |
| 方法抛出异常 + 达到最大重试次数 + `enableDlx=false` | `basicReject(requeue=false)` 丢弃 |

如需在业务代码中手动控制 Ack，可使用 `ManualAckHelper`：

```java
@Override
public void onMessage(TradeOrder order, Channel channel, Message amqpMessage) {
    try {
        processOrder(order);
        ManualAckHelper.ackSuccess(channel, amqpMessage, "订单处理");
    } catch (BusinessException e) {
        // 业务异常 → 不重试，直接拒绝
        ManualAckHelper.rejectToDlx(channel, amqpMessage, e.getMessage());
    } catch (Exception e) {
        // 系统异常 → 重试
        ManualAckHelper.rejectAndRequeue(channel, amqpMessage, e.getMessage());
    }
}
```

---

### 4.3 消费端重试与死信队列

#### 消费端重试配置

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 5        # 最大重试 5 次
          initial-interval: 3000 # 首次重试等待 3 秒
```

#### 启用死信队列

```yaml
spring:
  rabbitmq:
    listener:
      silky:
        enable-dlx: true
        dlx-exchange: my.dlx.exchange
        dlx-routing-key: my.dlx.routingKey
        dlx-message-ttl: 60000   # 死信消息 TTL 60 秒
```

**死信消息头（进入 DLX 时自动附加）：**

| 消息头 Key | 说明 |
|---|---|
| `x-death` | 标准死亡信息列表 |
| `x-original-queue` | 原始队列名 |
| `x-original-exchange` | 原始交换机名 |
| `x-original-routing-key` | 原始路由键 |
| `x-failure-reason` | 失败原因（异常信息） |
| `x-failure-timestamp` | 失败时间戳 |
| `x-retry-count` | 已重试次数 |
| `x-dead-letter-reason` | 固定值 `max_retries_exceeded` |

**死信队列工作流程：**

```
消费失败
    ↓
重试（最多 max-attempts 次）
    ↓
超过最大重试次数
    ↓
enableDlx=true → 构建死信消息（附加上方头信息）→ 发送到 DLX 交换机
enableDlx=false → basicReject(requeue=false) → 消息丢弃
```

---

## 五、消息持久化

### 5.1 实现 MessagePersistenceService 接口

实现该接口可将消息全链路状态记录到数据库、Redis 等：

```java
@Slf4j
@Service
public class DatabaseMessagePersistenceService implements MessagePersistenceService {

    @Autowired
    private MessageRecordMapper messageRecordMapper;

    @Override
    public void saveMessageBeforeSend(MassageSendParam message, String exchange,
                                       String routingKey, SendMode sendMode,
                                       String businessType, String description) {
        // 发送前：插入状态为 PENDING 的消息记录
        MessageRecord record = MessageRecord.builder()
            .messageId(message.getMessageId())
            .exchange(exchange)
            .routingKey(routingKey)
            .sendMode(sendMode.name())
            .businessType(businessType)
            .status(SendStatus.PENDING.name())
            .createTime(LocalDateTime.now())
            .build();
        messageRecordMapper.insert(record);
    }

    @Override
    public void updateMessageAfterSend(String messageId, SendStatus status,
                                        Long costTime, String exception) {
        // 发送后：更新消息状态
        messageRecordMapper.updateStatus(messageId, status.name(), costTime, exception);
    }

    @Override
    public void consumeSuccess(String messageId, Long costTime) {
        // 消费成功：更新消费状态
        messageRecordMapper.updateConsumeStatus(messageId, "CONSUMED", costTime);
    }

    @Override
    public void consumeFailure(String messageId, String exception, Long costTime) {
        // 消费失败：记录失败信息
        messageRecordMapper.updateConsumeStatus(messageId, "CONSUME_FAILED", costTime);
    }

    // 可选实现死信相关回调
    @Override
    public void recordMessageSendToDLQ(String messageId, String queueName, String errorMessage) {
        log.warn("消息进入死信队列: messageId={}, queue={}, error={}", messageId, queueName, errorMessage);
    }
}
```

### 5.2 启用持久化

```yaml
spring:
  rabbitmq:
    silky:
      persistence:
        enabled: true
```

> **默认行为：** 如果未提供 `MessagePersistenceService` 的自定义实现，组件会自动注册 `NoOpMessagePersistenceService`（空实现，仅打印 debug 日志）。自定义实现优先于默认实现（`@ConditionalOnMissingBean`）。

### 5.3 持久化接口方法说明

| 方法 | 触发时机 | 是否必须实现 |
|---|---|:---:|
| `saveMessageBeforeSend()` | 消息发送前 | ✅ |
| `updateMessageAfterSend()` | 消息发送后 | ✅ |
| `consumeSuccess()` | 消息消费成功 | ✅ |
| `consumeFailure()` | 消息消费失败 | ✅ |
| `recordDLQSendSuccess()` | 死信发送成功 | ❌（default 空实现） |
| `recordDLQSendFailure()` | 死信发送失败 | ❌（default 空实现） |
| `recordMessageSendToDLQ()` | 消息进入死信 | ❌（default 空实现） |
| `initialize()` | 容器启动时 | ❌（default 空实现） |
| `destroy()` | 容器销毁时 | ❌（default 空实现） |

---

## 六、序列化机制

### 6.1 默认序列化器

组件内置 `FastJson2MessageSerializer`，基于 Alibaba FastJson2 实现：

- **序列化**：`JSON.toJSONBytes(object)` → 默认**忽略 null 字段**，消息体更精简
- **反序列化**：`JSON.parseObject(bytes, clazz)` → 支持复杂泛型类型（通过 `TypeReference`）
- **字符串序列化**：带 `WriteClassName` 特性，保留类型信息

```java
// null 字段被忽略示例
TradeOrder order = new TradeOrder(1L, null, null, null);
// 序列化结果：{"id":1}  （只保留非 null 字段）
```

### 6.2 自定义序列化器

实现 `RabbitMqMessageSerializer` 接口并注册为 Spring Bean：

```java
@Bean
public RabbitMqMessageSerializer customSerializer() {
    return new JacksonMessageSerializer(); // 自定义 Jackson 实现
}
```

> 由于使用了 `@ConditionalOnMissingBean`，自定义 Bean 会自动替换默认的 FastJson2 实现。

### 6.3 如需保留 null 字段

可使用 `serialize(object, features...)` 重载方法：

```java
@Autowired
private RabbitMqMessageSerializer serializer;

// 显式保留 null 字段
byte[] bytes = serializer.serialize(order, JSONWriter.Feature.WriteNulls);
```

---

## 七、配置属性参考

### 7.1 主配置（前缀：`spring.rabbitmq.silky`）

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `enabled` | boolean | `true` | 是否启用 Silky 组件 |
| `persistence.enabled` | boolean | `false` | 是否启用消息持久化 |

### 7.2 发送配置（前缀：`spring.rabbitmq.silky.send`）

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `enabled` | boolean | `true` | 启用增强发送功能 |
| `default-send-mode` | SendMode | `AUTO` | 默认发送模式 |
| `sync-timeout` | long | `3000` | 同步超时（ms） |
| `async-thread-pool-size` | int | `10` | 异步线程池大小 |
| `enable-retry` | boolean | `true` | 启用发送重试 |
| `max-retry-count` | int | `3` | 最大重试次数 |
| `retry-interval` | long | `1000` | 重试间隔（ms） |
| `use-timeout` | boolean | `true` | 启用超时控制 |

### 7.3 监听配置（前缀：`spring.rabbitmq.listener.silky`）

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `enable-dlx` | boolean | `false` | 启用死信队列 |
| `dlx-exchange` | String | `silky.default.dlx.exchange` | DLX 交换机 |
| `dlx-routing-key` | String | `silky.default.dlx.routingKey` | DLX 路由键 |
| `dlx-message-ttl` | long | `0` | 死信消息 TTL（ms，0=不过期） |

### 7.4 SendMode 枚举

| 值 | 说明 |
|---|---|
| `SYNC` | 同步发送，等待服务器确认，超时则失败 |
| `ASYNC` | 异步发送，不阻塞调用线程，通过回调感知结果 |
| `AUTO` | 自动模式，当前实现等同于 defaultSendMode 配置值 |

---

## 八、核心架构解析

### 8.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    silky-rabbitmq-spring-boot-starter                    │
├──────────────────────────────┬──────────────────────────────────────────┤
│          发  送  端            │               消  费  端                 │
│                              │                                          │
│  ┌──────────────────────┐    │  ┌─────────────────────────────────┐    │
│  │ @RabbitMessage (AOP) │    │  │  AbstractRabbitMQListener<T>    │    │
│  │  + @RabbitPayload    │    │  │  ↳ 继承后自动注册到 Registry      │    │
│  └────────┬─────────────┘    │  └──────────────┬──────────────────┘    │
│           │ 切面拦截           │                 ↓                       │
│  ┌────────▼─────────────┐    │  ┌──────────────────────────────────┐   │
│  │  SkRabbitMqTemplate  │    │  │       ListenerRegistry           │   │
│  │  (接口)               │    │  │  ConcurrentHashMap<queue,listener│   │
│  └────────┬─────────────┘    │  └──────────────┬───────────────────┘   │
│           ↓                  │                 ↓                       │
│  ┌────────────────────────┐  │  ┌──────────────────────────────────┐   │
│  │ DefaultSkRabbitMqTmpl  │  │  │   RabbitMQListenerContainer      │   │
│  │ 同步/异步/延迟/重试      │  │  │   @RabbitListener(动态队列数组)   │   │
│  └────────┬───────────────┘  │  │   → 反序列化 → onMessage()       │   │
│           ↓                  │  │   → Ack / Reject / DLX           │   │
│  ┌──────────────────────┐    │  └──────────────────────────────────┘   │
│  │   RabbitTemplate     │    │                                          │
│  │   (Spring AMQP 原生)  │    │                                          │
│  └──────────────────────┘    │                                          │
├──────────────────────────────┴──────────────────────────────────────────┤
│                            公  共  模  块                                 │
│  FastJson2MessageSerializer    FastJson2MessageConverter                 │
│  MessagePersistenceService     NoOpMessagePersistenceService（默认）     │
│  MassageSendParam   SendResult  SilkyRabbitMQProperties                 │
│  SilkyRabbitMQAutoConfiguration（Spring Boot 自动配置入口）               │
└─────────────────────────────────────────────────────────────────────────┘
```

### 8.2 自动配置 Bean 清单

| Bean | 条件 | 说明 |
|---|---|---|
| `RabbitTemplate` | `@ConditionalOnMissingBean` + `ConnectionFactory` 存在 | 自定义模板，支持超时/mandatory |
| `RabbitMqMessageSerializer` | `@ConditionalOnMissingBean` | FastJson2 序列化器 |
| `MessagePersistenceService` | `@ConditionalOnMissingBean` | 默认 NoOp 空实现 |
| `SkRabbitMqTemplate` | `@ConditionalOnMissingBean` | 消息发送模板 |
| `RabbitMessageAspect` | `@ConditionalOnMissingBean` | `@RabbitMessage` AOP 切面 |
| `ListenerRegistry` | `@ConditionalOnMissingBean` | 监听器注册表 |
| `RabbitMQListenerContainer` | `@ConditionalOnMissingBean` | 统一消息监听容器 |
| `SimpleRabbitListenerContainerFactory` | `@ConditionalOnMissingBean` | Listener 容器工厂 |

> 所有 Bean 均使用 `@ConditionalOnMissingBean`，用户自定义的同类型 Bean 会自动替换默认实现。

### 8.3 消息发送流程

```
send(MassageSendParam)
    │
    ├─ checkDependencies()         // 参数校验
    ├─ generateMessageId()         // 生成/使用消息 ID
    ├─ determineSendMode()         // AUTO → defaultSendMode
    ├─ saveMessageBeforeSend()     // 持久化前置（如启用）
    ├─ buildMessage()              // 序列化 + 设置消息头
    │
    ├─ sendDelay=true ─────────── convertAndSend（x-delay 头）
    │
    └─ sendDelay=false
        ├─ SYNC ─── doSyncSend()
        │   ├─ useTimeout=false → doSimpleSyncSend()
        │   │   └─ enableRetry=true → doSyncSendWithRetry()
        │   └─ useTimeout=true  → doTimeoutSyncSend()（CompletableFuture + get(timeout)）
        │
        └─ ASYNC ── doAsyncSend()（CompletableFuture，不阻塞）
    │
    └─ updateMessageAfterSend()    // 持久化后置（如启用）
```

### 8.4 消息消费流程

```
RabbitMQListenerContainer.handleMessage()
    │
    ├─ validateListener()          // 查找队列对应的监听器
    ├─ deserializeMessage()        // FastJson2 反序列化为业务对象
    ├─ processMessage()            // 调用 listener.onMessage()
    │   └─ 成功 → acknowledgeMessage()
    │       ├─ autoAck=true  → Spring 自动处理
    │       └─ autoAck=false → basicAck
    │
    └─ 失败 → handleManualAckFailure()
        ├─ shouldRetry() → basicReject(requeue=true) 重入队
        └─ maxRetries exceeded
            ├─ enableDlx=true  → handleDeadLetterQueue() → 发送到 DLX
            └─ enableDlx=false → basicReject(requeue=false) 丢弃
```

---

## 九、API 速查表

### SkRabbitMqTemplate 方法

| 方法签名 | 说明 |
|---|---|
| `send(exchange, routingKey, message)` | 使用默认模式发送 |
| `send(exchange, routingKey, message, SendMode)` | 指定发送模式 |
| `send(exchange, routingKey, message, businessType, description, SendMode)` | 携带元数据发送 |
| `sendDelay(exchange, routingKey, message, delayMillis, businessType, description)` | 发送延迟消息 |
| `sendAsync(exchange, routingKey, message)` | 异步发送（不关注结果） |
| `sendAsync(exchange, routingKey, message, SendCallback)` | 异步发送（带回调） |
| `send(MassageSendParam)` | 使用完整参数发送 |

### ManualAckHelper 工具方法

| 方法签名 | 说明 |
|---|---|
| `ackSuccess(channel, amqpMessage, business)` | 手动确认成功 |
| `rejectAndRequeue(channel, amqpMessage, reason)` | 拒绝并重入队（重试） |
| `rejectToDlx(channel, amqpMessage, reason)` | 拒绝并不重入队（进死信） |
| `batchAck(channel, deliveryTag, multiple)` | 批量确认 |

### 常用注解

| 注解 | 作用域 | 说明 |
|---|---|---|
| `@RabbitMessage` | 方法 | 标记方法为消息发送触发器，切面自动执行发送 |
| `@RabbitPayload` | 参数 | 标记方法参数为消息体（配合 @RabbitMessage 使用） |

---

## 十、变更日志

### v1.0.4（2026-03-13）

- 重命名 `RabbitSendTemplate` 为 `SkRabbitMqTemplate` 及相关实现类
- 新增 `@RabbitPayload` 注解，用于在 `@RabbitMessage` 中精确标记消息体参数
- 重构消息发送方法，支持延迟消息和异步发送
- 新增 `SkAmqpHeaders` 常量类，扩展自定义消息头

---

## 附录：常见问题

**Q: 引入后启动报错 `No listener found`？**  
A: 检查 `AbstractRabbitMQListener` 的实现类是否被 Spring 扫描到（添加 `@Component` 且在扫描路径下）。

**Q: 消息序列化后 null 字段丢失，消费端反序列化失败？**  
A: FastJson2 默认忽略 null 字段。消费端实体类建议为字段设置默认值，或在发送时使用 `serialize(object, JSONWriter.Feature.WriteNulls)` 保留 null 字段。

**Q: 延迟消息不生效？**  
A: 需要在 RabbitMQ Server 中安装并启用 `rabbitmq_delayed_message_exchange` 插件，并创建类型为 `x-delayed-message` 的交换机。

**Q: 如何替换 FastJson2 序列化器为 Jackson？**  
A: 实现 `RabbitMqMessageSerializer` 接口并注册为 Spring Bean，Silky 会自动使用你的实现（`@ConditionalOnMissingBean` 保证）。

**Q: 如何在不启用 Silky 的情况下使用原生 RabbitTemplate？**  
A: 将 `spring.rabbitmq.silky.send.enabled=false`，此时 `SkRabbitMqTemplate` 内部会降级为原生 `RabbitTemplate` 发送。

---

> **官方文档**：https://github.com/yijuanmao/silky-starter  
> **问题反馈**：[GitHub Issues](https://github.com/yijuanmao/silky-starter/issues) | [Gitee Issues](https://gitee.com/zeng_er/silky-starter/issues)
