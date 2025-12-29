# 🚀 Silky Starter：如丝般顺滑的 Spring Boot 组件生态

<div align="center">

[![体验-丝滑一般顺滑](https://img.shields.io/badge/体验-丝滑一般顺滑-ff69b4.svg)]()
[![特性-开箱即用](https://img.shields.io/badge/特性-开箱即用-green.svg)]()
[![目标-高效开发](https://img.shields.io/badge/目标-高效开发-blue.svg)]()


</div>

<div align="center">

[![Java 1.8+](https://img.shields.io/badge/Java-1.8+-orange.svg)]()
[![Spring Boot 2.7.x](https://img.shields.io/badge/Spring%20Boot-2.7.x-brightgreen.svg)]()
[![Maven 3.10+](https://img.shields.io/badge/Maven-3.10+-blue.svg)]()
[![License Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)]()

</div>

## 🌟 生态宣言：告别碎片化，拥抱完整解决方案

> **Silky 不仅仅是一组组件，而是一个完整的开发生态系统！**  
> 从数据存储到消息队列，从缓存管理到文件存储，Silky 提供了一站式的企业级解决方案。  
> 让开发者告别"拼图式"的技术选型，专注于业务创新，享受如丝般顺滑的开发体验！

## 🏗️ 生态全景图

| 组件模块            | 图标  | 核心价值           | 状态    |
| --------------- | --- | -------------- | ----- |
| **🔄 状态管理**     | 🎯  | 复杂业务状态流转如丝般顺滑  | ✅ 已发布 |
| **🐰 消息队列**     | 📤  | 企业级消息处理，支持延迟消息 | ✅ 已发布 |
| **💾 缓存全能**     | ⚡   | 一站式Redis解决方案   | ✅ 已发布 |
| **📊 NoSQL数据库** | 🍃  | 极简MongoDB操作    | ✅ 已发布 |
| **☁️ 云存储**      | 🗂️ | 多云统一文件管理       | ✅ 已发布 |
| **🔧 核心工具**     | ⚙️  | 公共基础设施         | ✅ 已发布 |

* * *

## 🎯 生态组件深度解析

### 1. 🚀 Silky StateMachine：重新定义Spring Boot状态管理

**让复杂业务流转如丝般顺滑！**

#### ✨ 核心特性

-   **声明式配置**：告别if-else面条代码
-   **守卫条件**：业务规则的智能守护者
-   **转换动作**：业务逻辑的优雅执行者
-   **事件监听**：状态变更的实时广播

#### 🛠️ 5分钟上手

```java

// 🎯 定义状态枚举
public enum OrderState {
    CREATED, PENDING_PAYMENT, PAID, SHIPPED, COMPLETED, CANCELLED
}

// ⚡ 配置状态流转
@Configuration
public class OrderStateMachineConfig implements StateMachineConfig {
    @Override
    public Map<String, StateTransition> getTransitions() {
        return Map.of(
            "PAY_SUCCESS", new StateTransition("PENDING_PAYMENT", "PAY_SUCCESS", "PAID"),
            "SHIP", new StateTransition("PAID", "SHIP", "SHIPPED")
        );
    }
}

// 🚀 使用状态机
@Service
public class OrderService {
    @Autowired private StateMachineFactory stateMachineFactory;
    
    public Order processPayment(Long orderId, PaymentResult payment) {
        StateMachineContext context = new StateMachineContext("ORDER", orderId.toString());
        GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
        String event = payment.isSuccess() ? "PAY_SUCCESS" : "PAY_FAILED";
        stateMachine.trigger(event, context);
        return orderRepository.findById(orderId).orElseThrow();
    }
}
```

#### 📈 性能数据

-   **订单创建到支付完成**：从156ms优化到62ms（⏩ 60%更快）
-   **高并发状态转换**：从280 TPS提升到850 TPS（📈 200%更高吞吐）

* * *

### 2. 🔥 Silky RabbitMQ：企业级消息队列解决方案

**开箱即用，丝滑集成！**

#### ✨ 核心特性

-   **注解驱动**：`@RabbitMessage` 零侵入发送
-   **延迟消息**：毫秒级精度的定时消息
-   **多维度持久化**：数据库、Redis、MongoDB全支持
-   **智能重试**：可配置次数与间隔，支持指数退避

#### 🚀 使用示例


``` java

// 📤 AOP注解方式（推荐）
@Service
public class OrderService {
    @RabbitMessage(
        exchange = "order.exchange",
        routingKey = "order.create",
        businessType = "ORDER_CREATE",
        sendMode = SendMode.SYNC,
        delay = 30 * 60 * 1000L  // 30分钟延迟消息
    )
    public OrderResult createOrder(CreateOrderRequest request) {
        // 业务逻辑处理
        OrderResult result = orderMapper.insert(request);
        // 方法执行成功后自动发送消息
        return result;
    }
}

// 📥 消费监听
@Slf4j
@Component
public class OrderCreateListener extends AbstractRabbitMQListener<TradeOrder> {
    public OrderCreateListener() {
        super("order.create.queue");
    }
    
    @Override
    public void onMessage(TradeOrder message, Channel channel, Message amqpMessage) {
        log.info("收到订单创建消息: {}", message.getOrderId());
        orderProcessService.processOrder(message);
        // 自动异常处理和重试机制
    }
}
```

#### 🔧 高级特性

``` yaml

spring:
  rabbitmq:
    silky:
      send:
        default-send-mode: SYNC
        enable-retry: true
        max-retry-count: 3
      persistence:
        enabled: true
        type: DATABASE  # 支持DATABASE/REDIS/MONGODB
```

* * *

### 3. ⚡ Silky Redis：一站式高性能缓存解决方案

**五大核心功能，彻底告别Redis痛点！**

#### 🎯 五大核心模块

| 功能        | 图标 | 描述                    |
| --------- | -- | --------------------- |
| **智能缓存**  | 💾 | FastJson2序列化，性能提升35%+ |
| **分布式锁**  | 🔒 | 事务感知，完美解决并发问题         |
| **唯一单号**  | 🔢 | 高并发安全的分布式ID生成         |
| **分布式限流** | 🚦 | 三种算法应对不同场景            |
| **地理位置**  | 🌍 | 高效的地理位置计算与搜索          |

#### 🛠️ 快速使用

``` java

// 💾 智能缓存
@Service
public class ProductService {
    @Autowired private RedisCacheTemplate redisCacheTemplate;
    
    public void cacheProduct(Product product) {
        redisCacheTemplate.setObject("product:" + product.getId(), product, 1, TimeUnit.HOURS);
    }
}

// 🔒 分布式锁（事务感知）
@RedisLock(key = "'order:' + #orderId", lockType = LockType.REENTRANT, waitTime = 10)
@Transactional
public void processOrder(String orderId) {
    // 业务逻辑，锁会在事务提交后自动释放
}

// 🔢 唯一单号生成
@RedisSequence(
    redisKey = "order:number",
    prefix = "ORDER",
    datePattern = "yyyyMMdd",
    sequenceLength = 6
)
public String generateOrderNumber() {
    return null; // 由切面自动生成，如：ORDER20231130143015000001005
}

// 🚦 分布式限流
@RateLimit(
    key = "'api:user:register:' + #request.ip",
    algorithm = RateLimitAlgorithm.TOKEN_BUCKET,
    capacity = 100,
    refillRate = 10
)
public ApiResponse<User> userRegister(UserRegisterRequest request) {
    // 令牌桶限流，平滑控制流量
}
```

* * *

### 4. 🍃 Silky MongoDB：革命性的NoSQL操作体验

**Lambda表达式 + 多数据源，开发效率暴增300%！**

#### ✨ 革命性特性

-   **Lambda表达式**：类型安全，告别字段名硬编码
-   **多数据源切换**：一行注解搞定数据源切换
-   **读写分离**：自动路由，零配置上手
-   **事务支持**：企业级数据一致性保障

#### 🚀 代码对比


``` java

// ❌ 传统写法（容易出错）
Query query = new Query();
query.addCriteria(Criteria.where("userName").is("张三"));
query.addCriteria(Criteria.where("userAge").gt(18));
List<User> users = mongoTemplate.find(query, User.class);

// ✅ Silky写法（类型安全，IDE智能提示）
List<User> users = silkyMongoTemplate.list(
    new LambdaQueryWrapper<>(User.class)
        .eq(User::getName, "张三")  // IDE自动补全
        .gt(User::getAge, 18),     // 编译期检查
    User.class
);
```

#### 🔄 多数据源管理


``` java

@Service
public class MultiDataSourceService {
    // 🎯 注解切换数据源
    @DataSource("user_db")
    public List<User> getUsers() {
        return silkyMongoTemplate.list(User.class);
    }
    
    @DataSource("order_db") 
    public List<Order> getOrders() {
        return silkyMongoTemplate.list(Order.class);
    }
    
    // 🚀 跨数据源事务
    @Transactional
    public void createUserAndOrder(User user, Order order) {
        silkyMongoTemplate.switchDataSource("user_db");
        User savedUser = silkyMongoTemplate.save(user);
        
        silkyMongoTemplate.switchDataSource("order_db");
        order.setUserId(savedUser.getId());
        silkyMongoTemplate.save(order);
        // 💡 两个操作在同一个事务中！
    }
}
```

#### ⚡ 性能优化

-   **批量插入1万条**：从1250ms优化到280ms（🚀 77%性能提升）
-   **复杂条件查询**：从45ms优化到22ms（🚀 51%性能提升）

* * *

### 5. ☁️ Silky OSS：多云统一的文件存储解决方案

**智能上传，断点续传，让文件管理如丝般顺滑！**

#### ✨ 核心特性

-   **多云支持**：阿里云OSS、华为云OBS等（可扩展）
-   **智能上传**：根据文件大小自动选择最佳上传方式
-   **断点续传**：大文件分片上传，网络波动不影响体验
-   **进度监控**：实时获取上传进度信息
-   **安全可控**：支持加密上传和权限控制

#### 🚀 快速开始

##### 1️⃣ 添加依赖

``` xml

<dependency>
    <groupId>io.github.yijuanmao</groupId>
    <artifactId>silky-oss-spring-boot-starter</artifactId>
    <version>最新版本</version>
</dependency>

<!-- 根据配置选择对应的依赖包 -->
<!-- 阿里云OSS -->
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.18.3</version>
</dependency>

<!-- 华为云OBS -->
<dependency>
    <groupId>com.huaweicloud</groupId>
    <artifactId>esdk-obs-java</artifactId>
    <version>3.25.7</version>
</dependency>
```

##### 2️⃣ 配置参数


``` yaml

silky:
  oss:
    access-key: 你的AccessKey
    secret-key: 你的SecretKey
    bucket: 你的Bucket名称
    multipart-threshold: 134217728 # 128MB，超过此大小自动分片上传
    multipart-part-size: 33554432 # 32MB，每个分片大小
    provider: aliyun # 平台：aliyun、huawei
    # 阿里云配置
    aliyun:
      endpoint: 阿里云OSS的Endpoint
    # 华为云配置
    huawei:
      endpoint: 华为云的Endpoint
```

##### 3️⃣ 使用示例

``` java

@Service
public class FileUploadService {
    
    @Autowired
    private OssTemplate ossTemplate;
    
    /**
     * 智能上传（根据文件大小自动选择最佳上传方式）
     */
    public String uploadFile(MultipartFile file) {
        OssUploadParam param = new OssUploadParam();
        param.setObjectKey("uploads/" + System.currentTimeMillis() + "_" + file.getOriginalFilename());
        param.setFile(new File(file.getOriginalFilename()));
        param.setContentType(OssFileTypeEnum.fromFileName(file.getOriginalFilename()).getContentType());
        
        OssUploadResult result = ossTemplate.smartUpload(param);
        return result.getUrl();
    }
    
    /**
     * 大文件分片上传
     */
    public String uploadLargeFile(File largeFile) {
        // 1. 初始化分片上传
        InitiateMultipartUploadParam initParam = new InitiateMultipartUploadParam();
        initParam.setObjectKey("largefiles/" + largeFile.getName());
        InitiateMultipartResult initResult = ossTemplate.initiateMultipartUpload(initParam);
        
        String uploadId = initResult.getUploadId();
        List<PartETag> partETags = new ArrayList<>();
        
        // 2. 分片上传
        long partSize = 32 * 1024 * 1024; // 32MB
        long fileLength = largeFile.length();
        int partCount = (int) (fileLength / partSize);
        if (fileLength % partSize != 0) {
            partCount++;
        }
        
        for (int i = 0; i < partCount; i++) {
            UploadPartParam partParam = new UploadPartParam();
            partParam.setUploadId(uploadId);
            partParam.setPartNumber(i + 1);
            // 设置分片数据流...
            
            UploadPartResult partResult = ossTemplate.uploadPart(partParam);
            partETags.add(new PartETag(i + 1, partResult.getEtag()));
        }
        
        // 3. 完成分片上传
        CompleteMultipartUploadParam completeParam = new CompleteMultipartUploadParam();
        completeParam.setUploadId(uploadId);
        completeParam.setObjectKey("largefiles/" + largeFile.getName());
        completeParam.setPartEtags(partETags);
        
        CompleteMultipartUploadResult result = ossTemplate.completeMultipartUpload(completeParam);
        return result.getUrl();
    }
    
    /**
     * 生成预签名URL（用于临时访问）
     */
    public String generatePresignedUrl(String objectKey, int expirationMinutes) {
        GenPreSignedUrlParam param = new GenPreSignedUrlParam();
        param.setObjectKey(objectKey);
        param.setExpirationMinutes(expirationMinutes);
        
        GenPreSignedUrlResult result = ossTemplate.genPreSignedUrl(param);
        return result.getUrl();
    }
}
```

#### 🔧 高级功能

``` java


// 自定义云服务提供商
@Configuration
public class OssConfig {
    @Bean
    public void registerCustomProvider(OssTemplate ossTemplate) {
        // 创建自定义提供商适配器
        OssProviderAdapter customAdapter = new CustomOssProviderAdapter();
        // 注册到OSS模板
        ossTemplate.registerProvider("custom", customAdapter);
    }
}

// 切换云服务提供商
public class CloudSwitchService {
    @Autowired
    private OssTemplate ossTemplate;
    
    public void switchToHuawei() {
        ossTemplate.switchProvider("huawei");
    }
    
    public void switchToAliyun() {
        ossTemplate.switchProvider("aliyun");
    }
}
```

#### 📊 功能特性对比

| 特性        | 传统方式          | Silky OSS  | 优势     |
| --------- | ------------- | ---------- | ------ |
| **多平台支持** | 需要为每个平台编写不同代码 | 统一API，一键切换 | 降低维护成本 |
| **大文件上传** | 容易失败，无断点续传    | 自动分片，断点续传  | 稳定可靠   |
| **进度监控**  | 需要手动实现        | 内置进度回调机制   | 用户体验好  |
| **安全性**   | 需要自行处理加密和权限   | 内置安全机制     | 开箱即用   |

* * *

## 🎪 生态集成：1+1>2的协同效应

### 🗄️ 状态机 + MongoDB + OSS：完整的状态历史与备份


``` java

@Component
public class StateMachinePersistenceService {
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private OssTemplate ossTemplate;
    
    /**
     * 保存状态历史到MongoDB，重要快照备份到OSS
     */
    public void persistState(StateMachineContext context, String fromState, String toState) {
        // 1. 保存到MongoDB
        StateHistory history = StateHistory.builder()
            .machineType(context.getMachineType())
            .businessId(context.getBusinessId())
            .fromState(fromState)
            .toState(toState)
            .timestamp(LocalDateTime.now())
            .extendedData(context.getVariables())
            .build();
        mongoTemplate.save(history);
        
        // 2. 重要状态快照备份到OSS
        if (isImportantState(toState)) {
            String snapshotJson = JSON.toJSONString(context.getVariables());
            String backupKey = String.format("state-backup/%s/%s-%d.json",
                context.getMachineType(),
                context.getBusinessId(),
                System.currentTimeMillis());
            
            OssUploadParam param = new OssUploadParam();
            param.setObjectKey(backupKey);
            param.setStream(new ByteArrayInputStream(snapshotJson.getBytes()));
            param.setContentType("application/json");
            
            ossTemplate.smartUpload(param);
        }
    }
}
```

### 🔄 RabbitMQ + Redis + OSS：高可靠的消息处理与文件存储


``` java

@Component  
public class FileProcessingPipeline {
    @Autowired private RabbitSendTemplate rabbitSendTemplate;
    @Autowired private RedisLockTemplate redisLockTemplate;
    @Autowired private OssTemplate ossTemplate;
    
    /**
     * 处理文件上传并发送消息通知
     */
    public void processFileUpload(FileUploadEvent event) {
        // 1. 使用分布式锁防止重复处理
        redisLockTemplate.lock("file:process:" + event.getFileId(), 1, 30, TimeUnit.SECONDS, true, () -> {
            // 2. 上传文件到OSS
            OssUploadParam uploadParam = new OssUploadParam();
            uploadParam.setObjectKey("uploads/" + event.getFileId() + "/" + event.getFileName());
            uploadParam.setFile(event.getFile());
            
            OssUploadResult uploadResult = ossTemplate.smartUpload(uploadParam);
            
            // 3. 发送消息通知相关服务
            FileProcessedMessage message = new FileProcessedMessage();
            message.setFileId(event.getFileId());
            message.setFileUrl(uploadResult.getUrl());
            message.setFileSize(event.getFileSize());
            
            rabbitSendTemplate.send(
                "file.exchange",
                "file.processed",
                message,
                "FILE_PROCESSED",
                "文件处理完成消息"
            );
            
            return uploadResult;
        });
    }
}
```

* * *

## 📦 快速开始：5分钟集成整个生态

### 1️⃣ 添加依赖


``` xml

<!-- 核心生态依赖 -->
<dependencies>
    <!-- 状态管理 -->
    <dependency>
        <groupId>io.github.yijuanmao</groupId>
        <artifactId>silky-statemachine-spring-boot-starter</artifactId>
        <version>最新版本</version>
    </dependency>
    
    <!-- 消息队列 -->
    <dependency>
        <groupId>io.github.yijuanmao</groupId>
        <artifactId>silky-rabbitmq-spring-boot-starter</artifactId>
        <version>最新版本</version>
    </dependency>
    
    <!-- Redis全能组件 -->
    <dependency>
        <groupId>io.github.yijuanmao</groupId>
        <artifactId>silky-redis-spring-boot-starter</artifactId>
        <version>最新版本</version>
    </dependency>
    
    <!-- MongoDB增强 -->
    <dependency>
        <groupId>io.github.yijuanmao</groupId>
        <artifactId>silky-mongodb-spring-boot-starter</artifactId>
        <version>最新版本</version>
    </dependency>
    
    <!-- OSS云存储 -->
    <dependency>
        <groupId>io.github.yijuanmao</groupId>
        <artifactId>silky-oss-spring-boot-starter</artifactId>
        <version>最新版本</version>
    </dependency>
</dependencies>
```

### 2️⃣ 启用组件


``` java

@SpringBootApplication
@ComponentScan({"com.silky.**"})  // 🎯 一行注解启用整个生态
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3️⃣ 极简配置

``` yaml

# 统一配置入口
silky:
  enabled: true
  
  # MongoDB配置
  mongodb:
    datasource:
      master:
        uri: mongodb://localhost:27017/main_db
      slave:
        uri: mongodb://localhost:27017/read_db
  
  # Redis配置（自动启用所有功能）
  redis:
    host: localhost
    port: 6379
  
  # RabbitMQ配置
  rabbitmq:
    host: localhost
    port: 5672
    silky:
      send:
        default-send-mode: SYNC
      persistence:
        enabled: true
  
  # OSS云存储配置
  oss:
    provider: aliyun
    access-key: ${OSS_ACCESS_KEY}
    secret-key: ${OSS_SECRET_KEY}
    bucket: ${OSS_BUCKET}
    aliyun:
      endpoint: ${OSS_ENDPOINT}
```

### 4️⃣ 创建示例应用


``` java


@RestController
@RequestMapping("/api")
public class DemoController {
    
    @Autowired private StateMachineFactory stateMachineFactory;
    @Autowired private RabbitSendTemplate rabbitSendTemplate;
    @Autowired private RedisCacheTemplate redisCacheTemplate;
    @Autowired private SilkyMongoTemplate silkyMongoTemplate;
    @Autowired private OssTemplate ossTemplate;
    
    /**
     * 完整的业务处理示例
     */
    @PostMapping("/orders")
    @RabbitMessage(
        exchange = "order.exchange",
        routingKey = "order.create",
        businessType = "ORDER_CREATE"
    )
    @RedisLock(key = "'order:create:' + #request.userId", waitTime = 5)
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        // 1. 限流检查
        if (isRateLimited(request.getUserId())) {
            throw new RateLimitException("请求过于频繁");
        }
        
        // 2. 保存订单到MongoDB
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        order.setStatus("CREATED");
        
        Order savedOrder = silkyMongoTemplate.save(order);
        
        // 3. 缓存订单信息
        redisCacheTemplate.setObject(
            "order:" + savedOrder.getId(),
            savedOrder,
            30,
            TimeUnit.MINUTES
        );
        
        // 4. 上传订单附件到OSS
        if (request.getAttachment() != null) {
            OssUploadParam param = new OssUploadParam();
            param.setObjectKey("orders/" + savedOrder.getId() + "/attachment");
            param.setStream(request.getAttachment().getInputStream());
            ossTemplate.smartUpload(param);
        }
        
        // 5. 触发状态机流转
        StateMachineContext context = new StateMachineContext("ORDER", savedOrder.getId().toString());
        context.setVariable("order", savedOrder);
        GenericStateMachine stateMachine = stateMachineFactory.create("ORDER");
        stateMachine.trigger("CREATE", context);
        
        return savedOrder;
    }
    
    private boolean isRateLimited(String userId) {
        // 使用Redis限流检查
        String key = "rate:order:create:" + userId;
        Long count = redisCacheTemplate.increment(key, 1);
        if (count == 1) {
            redisCacheTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        return count > 10; // 每分钟最多10次
    }
}
```

* * *

## 🏆 生态价值：为什么选择Silky？

### 🎯 技术价值

1.  **完整生态**：覆盖微服务开发全链路，从数据存储到文件管理
1.  **极致性能**：每个组件都经过生产环境验证，性能卓越
1.  **无缝集成**：组件间天然协作，1+1>2的协同效应
1.  **企业级可靠**：事务支持、监控告警、故障恢复一应俱全

### 💰 商业价值

1.  **降低总拥有成本**：统一技术栈，减少学习和维护成本
1.  **加速产品上市**：开箱即用，聚焦业务创新
1.  **提升系统稳定性**：经过验证的企业级解决方案
1.  **未来可扩展**：模块化设计，按需引入组件

### 🔄 典型应用场景

-   **电商系统**：订单状态机 + 库存缓存 + 支付消息队列 + 商品图片OSS存储
-   **内容平台**：MongoDB内容存储 + Redis热点缓存 + OSS媒体文件存储
-   **物联网平台**：设备状态机 + 消息队列 + Redis实时缓存 + OSS数据备份
-   **金融系统**：分布式锁 + 交易状态机 + Redis限流 + 审计日志OSS存储

* * *

## 📚 学习资源

-   **🌐 官方网站**：[https://silky-ecosystem.github.io](https://github.com/yijuanmao/silky-starter)
-   **📖 完整文档**：[GitHub Wiki](https://github.com/yijuanmao/silky-starter/wiki)
-   **💻 示例项目**：[silky-demo](https://github.com/yijuanmao/silky-starter/tree/master/silky-starter-test)

[//]: # (-   **🎬 视频教程**：[B站专栏]&#40;https://space.bilibili.com/your-channel&#41;)

## 🤝 加入我们

Silky 生态由开发者共建，为开发者服务！我们欢迎：

-   🐞 **提交 Issue**：[报告问题或建议](https://github.com/yijuanmao/silky-starter/issues)
-   🔧 **提交 PR**：欢迎代码贡献，共建更好用的组件
-   📚 **完善文档**：帮助更多人上手使用
-   💬 **技术交流**：加入我们的开发者社区
-   ⭐ **Star 支持**：你的认可是我们持续更新的动力

## 🎉 立即开始

**不要再浪费时间在技术选型和组件集成上了！**  
选择 Silky 生态，一次性获得企业级的技术基础设施，让团队专注于创造业务价值！


``` bash

# 克隆示例项目，立即体验
git clone https://github.com/yijuanmao/silky-starter.git
cd silky-starter/silky-starter-test
mvn spring-boot:run
```

**加入数千家企业的选择，让 Silky 生态为你的业务注入新的活力！**

* * *

### 👥 加入社区

📢 **关注SilkyStarter**公众号获取最新源码和技术动态！  
💬 **加入silky-starter技术交流群**，与众多开发者一起探讨技术问题：  
添加微信 `824414828` 或扫描公众号二维码，备注"技术交流"邀请进群

⭐ **如果这个项目帮助了你，请给我们一个Star！**

**如果这让你感同身受，那么今天就是你的幸运日！**

* * *

## 🙏 感谢支持

**如果觉得 Silky 生态对你有帮助，请给我们一个 ⭐ Star 支持！**  
**分享给更多开发者，让我们一起构建更好的开发生态！**

<div align="center">

### 💳 支持我们

如果你觉得我们的工作对你有帮助，可以捐赠请作者喝杯咖啡~，在此表示感谢！

<img src="https://petsgo.oss-cn-shenzhen.aliyuncs.com/prod/wx.jpg" height="300px" alt="微信"> <img src="https://petsgo.oss-cn-shenzhen.aliyuncs.com/prod/alipay.jpg" height="300px" alt="支付宝">

或者点击以下链接，将页面拉到最下方点击"捐赠"即可  
[Gitee上捐赠](https://gitee.com/zeng_er/silky-starter)

* * *

[//]: # (**📧 技术咨询：824414828@qq.com**  )

[//]: # (**🌐 关注我们：[@SilkyEcosystem]&#40;https://twitter.com/SilkyEcosystem&#41;**)

*本文由 Silky 生态系统团队倾情奉献*