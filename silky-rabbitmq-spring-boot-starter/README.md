
## 一、环境准备

### 1.1 依赖引入

在 Spring Boot 项目中添加以下依赖：


```xml
<dependency>
    <groupId>io.github.yijuanmao</groupId>
    <artifactId>silky-rabbitmq-spring-boot-starter</artifactId>
    <version>${latest.version}</version> <!-- 建议使用最新版本 -->
</dependency>
```

> **注意**：请将`${latest.version}`替换为最新版本号，可在[GitHub](https://github.com/yijuanmao/silky-starter)或[Gitee](https://gitee.com/zeng_er/silky-starter)查看最新版本。

### 1.2 启动类配置

在 Spring Boot 启动类中添加`@EnableSilkyRabbitMQ`注解：


```java

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.silky.starter.rabbitmq.EnableSilkyRabbitMQ;

@SpringBootApplication
//添加 com.silky 扫描路径
@ComponentScan({"com.silky.**"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.out.println("===================================" +
                "======== Silky Rabbit MQ 启动成功=========" +
                "===================================");
    }
}
```

### 1.3 配置文件

在`application.yml`中配置 RabbitMQ 连接和 Silky 组件：

```yaml
spring:
  #mq配置
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin
    virtual-host: /

    #silky组件配置
    silky:
      # 启用Silky组件
      enabled: true
      send:
        enabled: true                  # 启用增强发送功能
        default-send-mode: SYNC        # 默认发送模式: SYNC, ASYNC, AUTO
        sync-timeout: 5000             # 同步发送超时时间(ms)
        async-thread-pool-size: 10     # 异步线程池大小
        enable-retry: true             # 启用重试机制
        max-retry-count: 3             # 最大重试次数
        retry-interval: 0           # 重试间隔(ms)
      persistence:
        enabled: true                 # 禁用持久化（使用空实现）

    listener:
      # silky组件监听配置
      silky:
        enable_dlx: true               # 启用死信队列功能，默认不启用
        dlx-message-ttl: 1000
      simple:
        default-requeue-rejected: false
        #设置手动确认,auto：自动，manual:手动
        acknowledge-mode: manual
        # 消费端的监听个数(即@RabbitListener开启几个线程去处理数据。)
        concurrency: 1
        # 消费端的监听最大个数
        max-concurrency: 20
        prefetch: 10
        retry:
          # 开启消费者(程序出现异常)重试机制，默认开启并一直重试
          enabled: true
          # 最大重试次数
          max-attempts: 5
          # 重试间隔时间(毫秒)
          initial-interval: 3000
```

> **配置说明**：
>
> -   `default-send-mode`：默认发送模式，可选`SYNC`（同步）、`ASYNC`（异步）、`AUTO`（自动）
> -   `enable-retry`：是否启用发送重试
> -   `max-retry-count`：最大重试次数
> -   `dlx-message-ttl`：死信消息 TTL（毫秒）
> -   `retry.max-attempts`：消费端重试最大次数
> -   `retry.initial-interval`：消费端重试初始间隔（毫秒）

## 二、消息发送

### 2.1 使用`SkRabbitMqTemplate`发送消息

注入`SkRabbitMqTemplate`，直接调用方法发送消息：


```java
@Autowired
private SkRabbitMqTemplate skRabbitMqTemplate;
```

#### 2.1.1 普通发送消息


```java
TradeOrder order = new TradeOrder(3L, LocalDateTime.now(), "测试MQ发送3", BigDecimal.ONE);
SendResult sendResult = skRabbitMqTemplate.send("example-exchange", "example-routingKey", order);
log.info("发送结果: {}", sendResult);
```

#### 2.1.2 指定发送模式



```java
// 指定发送模式为 ASYNC（异步）
SendResult sendResult = skRabbitMqTemplate.send("example-exchange", "example-routingKey", order, SendMode.ASYNC);
```

#### 2.1.3 带业务类型和描述


```java
String businessType = "TRADE";
String description = "silky-测试描述";
SendResult sendResult = skRabbitMqTemplate.send(
    "example-exchange", 
    "example-routingKey", 
    order, 
    businessType, 
    description, 
    SendMode.ASYNC
);
```

#### 2.1.4 使用`MassageSendParam`参数

```java
String messageId = IdUtil.fastSimpleUUID();
MassageSendParam param = MassageSendParam.builder()
    .body(order)
    .messageId(messageId)
    .exchange("example-exchange")
    .routingKey("example-routingKey")
    .sendDelay(false)
    .sendMode(SendMode.SYNC)
    .businessType("TRADE")
    .description("silky-测试描述")
    .build();
SendResult sendResult = skRabbitMqTemplate.send(param);
```

#### 2.1.5 发送延迟消息

```java
String businessType = "TRADE";
String description = "silky-延迟-测试描述";
TradeOrder order = new TradeOrder(5L, LocalDateTime.now(), "测试MQ发送-延迟消息测试", BigDecimal.ONE);
SendResult sendResult = skRabbitMqTemplate.sendDelay(
    "example-delay-exchange", 
    "example-delay-routingKey", 
    order, 
    7000L, // 延迟7秒
    businessType, 
    description
);
```

#### 2.1.6 异步发送 + 回调


```java
skRabbitMqTemplate.sendAsync(
    "example-exchange", 
    "example-routingKey", 
    order, 
    new TestSendCallback() // 实现 SendCallback 接口
);
```

**`TestSendCallback`实现**：

 ```java
 @Slf4j
 @Component
 public class TestSendCallback implements SendCallback {
     @Override
     public void onSuccess(SendResult result) {
         log.info("消息发送成功回调: {}", result);
     }
     
     @Override
     public void onFailure(SendResult result) {
         log.info("消息发送失败回调: {}", result);
     }
 }
 ```

#### 2.1.7 发送结果处理

所有发送方法均返回`SendResult`对象，包含以下信息：

| 属性             | 说明       |
| -------------- | -------- |
| `messageId`    | 生成的消息ID  |
| `success`      | 是否成功发送   |
| `costTime`     | 发送耗时（毫秒） |
| `errorMessage` | 失败原因     |

### 2.2 使用`@RabbitMessage`注解发送消息

在任意 Spring Bean 的方法上添加注解，切面会自动将标记的参数作为消息发送：

```java
@Service
public class TradeOrderService {
    @RabbitMessage(
        exchange = "example-exchange",
        routingKey = "example-routingKey",
        businessType = "TRADE",
        description = "订单创建事件",
        delay = 0, // 延迟毫秒数，0表示不延迟
        retryCount = 0, // 发送重试次数
        retryInterval = 0 // 重试间隔（毫秒）
    )
    public void testSendMq(@RabbitPayload TradeOrder order) {
        log.info("接收到订单消息: {}", order);
        // 业务逻辑
    }
}
```

**注解参数详解**：

| 参数               | 说明               | 默认值  |
 | ------------------- | ------------------- | ------- |
| `exchange`       | 交换机名称            | -    |
| `routingKey`     | 路由键              | -    |
| `businessType`   | 业务类型（存入消息头）      | -    |
| `description`    | 描述（存入消息头）        | -    |
| `sendMode`       | 发送模式，默认 AUTO     | AUTO |
| `delay`          | 延迟毫秒数，大于0则发送延迟消息 | 0    |
| `throwOnFailure` | 发送失败时是否抛出异常      | true |
| `retryCount`     | 发送重试次数           | 0    |
| `retryInterval`  | 重试间隔（毫秒）         | 0    |

## 三、消息监听

### 3.1 原生`@RabbitListener`监听

在任何`@Component`类中使用`@RabbitListener`：


```java
@Component
public class RabbitMQListener {
    @RabbitListener(queues = "example-order-queue")
    public void onMessage(TradeOrder message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        log.info("收到订单消息: {}", message);
        // 业务处理...
        channel.basicAck(tag, false); // 手动确认
    }
}
```

#### 3.1.1 获取消息头元数据

```java
@RabbitListener(queues = "example-order-queue")
public void handleWithHeaders(
    TradeOrder order,
    @Header("businessType") String businessType,
    @Header("description") String description,
    @Header("source") String source,
    @Header("extData") Map<String, Object> extData
) {
    log.info("业务类型: {}, 描述: {}, 来源: {}, 扩展数据: {}", 
        businessType, description, source, extData);
}
```

### 3.2 继承`AbstractRabbitMQListener`监听

实现一个监听器类，继承`AbstractRabbitMQListener<T>`，泛型指定消息类型：

```java
@Component
public class OrderQueueListener extends AbstractRabbitMQListener<TradeOrder> {
    public OrderQueueListener() {
        super("example-order-queue"); // 指定监听的队列名
    }

    @Override
    public void onMessage(TradeOrder message, Channel channel, Message amqpMessage) {
        log.info("收到订单消息: {}", message);
        // 业务处理...
        // 方法正常执行 ⇒ 自动确认
        // 若抛出异常 ⇒ 触发重试
    }
}
```

**自动确认机制**：

-   方法正常执行完毕 → 容器自动调用`basicAck`
-   方法抛出异常 → 容器根据重试配置决定是重试还是最终拒绝

### 3.3 消费端重试与死信队列

#### 3.3.1 消费端重试配置

在`application.yml`中配置消费端重试：

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        retry:
          enabled: true
          max-attempts: 5
          initial-interval: 3000
```

#### 3.3.2 启用死信队列

```yaml
silky:
  listener:
    silky:
      enable-dlx: true
      dlx-message-ttl: 1000
```

**死信队列工作流程**：

1.  消费端重试失败（达到最大重试次数）
1.  消息被拒绝并进入死信队列
1.  死信队列消息转发到指定死信交换机
1.  可配置死信交换机和路由键

## 四、消息持久化

### 4.1 实现`MessagePersistenceService`接口

组件定义了`MessagePersistenceService`接口，实现该接口可将消息状态存入数据库、Redis 等：

```java
@Slf4j
@Service
public class DatabaseMessagePersistenceService implements MessagePersistenceService {
    @Override
    public void saveMessageBeforeSend(MassageSendParam message, String exchange, String routingKey, SendMode sendMode, String businessType, String description) {
        log.info("保存消息记录: messageId={}, exchange={}, routingKey={}, sendMode={}, businessType={}, description={}", 
            message.getMessageId(), exchange, routingKey, sendMode, businessType, description);
    }
    
    @Override
    public void updateMessageAfterSend(String messageId, SendStatus status, Long costTime, String exception) {
        log.info("更新消息发送结果: messageId={}, status={}, costTime={}, exception={}", 
            messageId, status, costTime, exception);
    }
    
    @Override
    public void consumeSuccess(String messageId, Long costTime) {
        log.info("消息消费成功: messageId={}, costTime={}", messageId, costTime);
    }
    
    @Override
    public void consumeFailure(String messageId, String exception, Long costTime) {
        log.info("消息消费失败: messageId={}, exception={}, costTime={}", 
            messageId, exception, costTime);
    }
}
```

### 4.2 启用消息持久化

在`application.yml`中启用持久化：


```yaml
silky:
  persistence:
    enabled: true
```

## 五、序列化特性

Silky RabbitMQ Starter 内置`FastJson2MessageSerializer`，使用 fastjson2 进行序列化，默认忽略值为`null`的字段，大幅减少消息体积：

```java
TradeOrder order = new TradeOrder(1L, null); // remark 为 null
// 序列化后的 JSON 不包含 remark 字段
```

> **如需保留 null 字段**，可自定义序列化器或修改现有实现。

## 六、总结

Silky RabbitMQ Starter 提供了以下核心特性：

| 功能      | 优势                                       |
| ------- | ---------------------------------------- |
| **发送端** | 模板类 + 注解两种方式，支持同步/异步/延迟，元数据自动存入消息头，重试可配置 |
| **监听端** | 原生注解灵活处理，继承类简化开发；均支持自动重试和死信              |
| **序列化** | fastjson2 高性能，默认忽略 null 字段，消息体更小         |
| **扩展性** | 持久化接口允许记录全链路消息状态                         |

## 七、参考示例

完整的示例代码可在 GitHub 和 Gitee 仓库中查看：

-   [GitHub: silky-starter](https://github.com/yijuanmao/silky-starter/tree/master/silky-starter-test/silky-starter-rabbitmq-test)
-   [Gitee: silky-starter](https://gitee.com/zeng_er/silky-starter/tree/master/silky-starter-test/silky-starter-rabbitmq-test)

> **重要提示**：本组件基于 RabbitMQ 2.7.x+ 版本开发，建议使用最新稳定版 RabbitMQ 以获得最佳体验。

* * *

**文档更新日期**：2026-03-16  
**组件版本**： $ {1.0.4}  
**官方文档**：<https://github.com/yijuanmao/silky-starter>  
**开源仓库**：[GitHub](https://github.com/yijuanmao/silky-starter)|[Gitee](https://gitee.com/zeng_er/silky-starter)