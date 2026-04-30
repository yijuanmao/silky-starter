# Silky Starter BOM

## 简介

`silky-starter-bom` (Bill of Materials) 是 Silky Starter 生态的版本统一管理模块。它允许开发者通过单一的 BOM 依赖，即可引入 Silky Starter 的所有组件，同时保证各组件版本的高度一致性。

## 特性

- **统一版本管理**：一次声明，管理所有组件版本
- **按需引入**：开发者仍可按需引入具体组件
- **依赖优化**：BOM 会自动处理组件间的依赖传递
- **开发友好**：简化 pom.xml 配置，提升开发效率

## 使用方式

### 方式一：通过 BOM 引入所有组件（推荐）

在项目的 `pom.xml` 中添加 BOM：

```xml
<dependencyManagement>
    <dependencies>
        <!-- 引入 Silky Starter BOM -->
        <dependency>
            <groupId>top.silky</groupId>
            <artifactId>silky-starter-bom</artifactId>
            <version>${silky.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- 现在引入任何 silky 组件时，无需指定版本号 -->
    <dependency>
        <groupId>top.silky</groupId>
        <artifactId>silky-redis-spring-boot-starter</artifactId>
    </dependency>
    
    <dependency>
        <groupId>top.silky</groupId>
        <artifactId>silky-rabbitmq-spring-boot-starter</artifactId>
    </dependency>
    
    <dependency>
        <groupId>top.silky</groupId>
        <artifactId>silky-mongodb-spring-boot-starter</artifactId>
    </dependency>
    
    <dependency>
        <groupId>top.silky</groupId>
        <artifactId>silky-oss-spring-boot-starter</artifactId>
    </dependency>
    
    <dependency>
        <groupId>top.silky</groupId>
        <artifactId>silky-statemachine-spring-boot-starter</artifactId>
    </dependency>
    
    <dependency>
        <groupId>top.silky</groupId>
        <artifactId>silky-excel-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 方式二：按需引入特定组件

开发者也可以根据项目需求，只引入需要的组件：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.silky</groupId>
            <artifactId>silky-starter-bom</artifactId>
            <version>${silky.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- 只需要 Redis 功能 -->
    <dependency>
        <groupId>top.silky</groupId>
        <artifactId>silky-redis-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 方式三：Gradle 项目使用 BOM

```groovy
dependencies {
    implementation platform('top.silky:silky-starter-bom:1.0.0')
    
    // 引入组件时无需指定版本
    implementation 'top.silky:silky-redis-spring-boot-starter'
    implementation 'top.silky:silky-rabbitmq-spring-boot-starter'
}
```

## 可用组件

| 组件 | 描述 |
|------|------|
| `silky-starter-core` | 核心工具模块 |
| `silky-redis-spring-boot-starter` | Redis 全能组件 |
| `silky-rabbitmq-spring-boot-starter` | RabbitMQ 消息队列组件 |
| `silky-mongodb-spring-boot-starter` | MongoDB 增强组件 |
| `silky-oss-spring-boot-starter` | 云存储 OSS 组件 |
| `silky-statemachine-spring-boot-starter` | 状态机组件 |
| `silky-excel-spring-boot-starter` | Excel 导入导出组件 |

## 版本管理

在项目中统一管理 Silky 版本：

```xml
<properties>
    <silky.version>1.0.0</silky.version>
</properties>
```

这样，当 Silky 发布新版本时，只需要修改一处版本号，所有组件都会自动使用新版本。

## 与 Spring Boot BOM 配合使用

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Boot BOM -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.7.8</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- Silky Starter BOM -->
        <dependency>
            <groupId>top.silky</groupId>
            <artifactId>silky-starter-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 常见问题

### Q: 引入 BOM 后，还需要单独指定组件版本吗？

**不需要**。BOM 会统一管理所有组件的版本，引入组件时无需再指定 `<version>`。

### Q: 如何覆盖 BOM 中的某个组件版本？

如果需要使用特定组件的自定义版本，可以在 `<dependencyManagement>` 中先声明该依赖：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>top.silky</groupId>
            <artifactId>silky-starter-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- 覆盖 Redis 组件版本 -->
        <dependency>
            <groupId>top.silky</groupId>
            <artifactId>silky-redis-spring-boot-starter</artifactId>
            <version>1.0.1</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Q: Spring Boot 2.x 和 3.x 如何选择？

- **Spring Boot 2.7.x**：使用 Silky Starter 1.0.x 版本
- **Spring Boot 3.x**：请关注后续版本更新

## 许可证

本项目采用 Apache License 2.0 许可证。
