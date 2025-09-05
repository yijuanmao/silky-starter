<div align="center">

# silky-starter - 让每一次开发都如丝般顺滑

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

## 📚 简介

silky-starter 是一款专为提升开发效率而设计的丝滑组件库，取名"silky"
寓意开发体验如丝绸般顺滑。组件库提供统一的OSS文件操作接口和MongoDB数据访问接口，采用智能策略模式设计，让文件操作和数据库访问变得异常简单和高效。

丝滑体验，开箱即用 - 只需简单配置，即可享受丝滑般的开发体验，大幅减少重复代码编写，提升项目质量和稳定性。

## ✨ 丝滑特性

- 🧊 丝滑体验: 简洁API设计，让每次调用都如丝般顺滑
- 🚀 智能上传: 根据文件大小自动选择最佳上传方式，智能又省心
- ⚡ 多云支持: 阿里云OSS、华为云OBS等（可扩展），切换如丝顺滑
- 🔄 断点续传: 大文件分片上传和断点续传，网络波动不影响丝滑体验
- 📊 进度监控: 实时获取上传进度信息，进度展示清晰流畅
- 🔒 安全可控: 支持加密上传和权限控制，安全又不失便捷
- 📦 开箱即用: 简单配置即可快速集成，真正实现五分钟上手
- 🍃 MongoDB支持: 提供简洁高效的MongoDB操作接口，数据操作如丝顺滑

## 📋 目录

- [丝滑特性](#丝滑特性)
- [技术栈要求](#技术栈要求)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
    - [OSS快速开始](#OSS快速开始)
    - [MongoDB快速开始](#MongoDB快速开始)
- [API接口说明](#API接口说明)
    - [OSS API接口说明](#OSS API接口说明)
    - [MongoDB API接口说明](#MongoDB API接口说明)
- [使用示例](#使用示例)
    - [OSS使用示例](#OSS使用示例)
    - [MongoDB使用示例](#MongoDB使用示例)
- [扩展功能](#扩展功能)
- [注意事项](#注意事项)
- [获取帮助](#获取帮助)

## 📜技术栈要求

- JDK 1.8+
- Spring Boot 2.7.x
- Maven 3.10+

## 🗂️ 项目模块

silky-starter 包含以下独立组件模块：

``` text
silky-starter
├─ silky-oss-spring-boot-starter（OSS组件模块，文件、图片上传下载，支持：阿里云、华为云等）
├─ silky-mongodb-spring-boot-starter（MongoDB组件模块）
├─ silky-starter-core（核心模块,公共常用的工具包类）
├─ silky-starter-test（测试模块）
│  ├─ silky-starter-oss-test（OSS组件测试模块）
```

### 1. silky-oss-spring-boot-starter

    OSS文件操作组件，提供统一的文件上传、下载和管理接口，支持多种云存储平台。

功能特性：

- 统一的OSS文件操作接口
- 支持阿里云OSS、华为云OBS等云存储平台
- 智能上传策略（根据文件大小自动选择最佳上传方式）
- 大文件分片上传和断点续传
- 进度监控和回调机制
- 安全加密和权限控制
-

### 2. silky-mongodb-spring-boot-starter

    MongoDB数据访问组件，提供简洁高效的MongoDB操作接口。

功能特性：

- 简洁的CRUD操作接口
- 强大的条件查询和分页功能
- 灵活的排序和字段投影
- 批量操作支持
- 类型安全的查询构建
- 支持lambda表达式查询

### 3. silky-starter-core

    核心模块，包含公共常用的工具类和基础设施代码。

### 4. silky-starter-test

    测试模块，包含各组件的测试用例和示例代码。

## 🚀 快速开始

### OSS快速开始

### 1. 添加依赖：

在`pom.xml`中添加依赖，开启丝滑体验：

```xml

<dependencies>

    <dependency>
        <groupId>com.silky.starter</groupId>
        <artifactId>silky-oss-spring-boot-starter</artifactId>
        <version>${最新版本}</version>
    </dependency>

    <!-- 根据配置选择对应的依赖包 start -->
    <!-- 阿里云oss -->
    <dependency>
        <groupId>com.aliyun.oss</groupId>
        <artifactId>aliyun-sdk-oss</artifactId>
        <version>3.18.3</version>
    </dependency>

    <!-- 华为云 OBS -->
    <dependency>
        <groupId>com.huaweicloud</groupId>
        <artifactId>esdk-obs-java</artifactId>
        <version>3.25.7</version>
    </dependency>
    <!-- 根据配置选择对应的依赖包 end -->

</dependencies>
```

### 2. 启用组件

启动类添加注解，开启丝滑OSS功能：

```java

@SpringBootApplication
@ComponentScan({"com.silky.**"})
public class TestAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestAppApplication.class, args);
    }
}
```

### 3. 配置参数

在`application.yml`中添加配置：

```yaml
silky:
  oss:
    access-key: 你的AccessKey
    secret-key: 你的SecretKey
    bucket: 你的Bucket名称
    # 分片上传的阈值,默认128MB，单位字节，文件超过此大小将自动分片上传
    multipart-threshold: 134217728 # 128 * 1024 * 1024
    # 自动分片上传时每个分片大小，默认 32MB
    multipart-part-size: 33554432 # 32 * 1024 * 1024
    # 平台：aliyun、huawei
    provider: aliyun
    # 阿里云配置
    aliyun:
      endpoint: 阿里云OSS的Endpoint
    # 华为云配置
    huawei:
      endpoint: 华为云的Endpoint
```

### MongoDB组件快速开始

### 1. 添加依赖：

在`pom.xml`中添加依赖，开启丝滑体验：

```xml

<dependencies>

    <dependency>
        <groupId>com.silky.starter</groupId>
        <artifactId>silky-mongodb-spring-boot-starter</artifactId>
        <version>${最新版本}</version>
    </dependency>

</dependencies>
```

### 2. 启用组件

启动类添加注解，开启丝滑MongoDB功能：

```java

@SpringBootApplication
@ComponentScan({"com.silky.**"})
public class TestAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestAppApplication.class, args);
    }
}
```

### 3. 配置参数

在`application.yml`中添加配置：

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://用户名:密码@localhost:27017/数据库名称
      # 或者使用以下分开配置
      # host: localhost
      # port: 27017
      # database: test
      # username: user
      # password: pass
```

## 🔧 API 接口说明

### OSS API接口说明

### 核心方法

| 方法              | 说明               | 参数                   |
|-----------------|------------------|----------------------|
| smartUpload     | 智能上传（自动选择最佳上传方式） | OssUploadParam       |
| downloadFile    | 下载文件到本地          | DownloadFileOssParam |
| deleteFile      | 删除云端文件           | DeleteFileOssParam   |
| getFileMetadata | 获取文件元数据          | GetFileMetadataParam |
| genPreSignedUrl | 生成预签名URL         | GenPreSignedUrlParam |

### 高级上传功能

| 方法                         | 说明         |
|----------------------------|------------|
| standardUploadWithCallback | 带回调的标准上传   |
| initiateMultipartUpload    | 初始化分片上传    |
| uploadPart                 | 上传分片       | 
| completeMultipartUpload    | 完成分片上传     |
| abortMultipartUpload       | 终止分片上传     |
| listUploadedParts          | 获取已上传的分片列表 |

### 扩展功能

| 方法                       | 说明                |
|--------------------------|-------------------|
| switchProvider           | 切换云服务提供商(待完善)     |
| registerProvider         | 	注册自定义云服务提供商 待完善) |
| setDefaultUploadStrategy | 设置默认上传策略  待完善)    | 

### MongoDB API接口说明

MongodbTemplate提供了一系列简洁高效的MongoDB操作接口：

### 查询操作

| 方法                    | 说明           | 参数                                                                                                            |
|-----------------------|--------------|---------------------------------------------------------------------------------------------------------------|
| findPage              | 分页查询         | Page<?> page, Class<T> clazz                                                                                  |
| findPage              | 条件分页查询       | CriteriaWrapper criteriaWrapper, Page<?> page, Class<T> clazz                                                 |
| findPage              | 条件排序分页查询     | CriteriaWrapper criteriaWrapper, SortBuilder sort, Page<?> page, Class<T> clazz                               |
| findById              | 根据ID查询       | String id, Class<T> clazz                                                                                     |
| findOneByQuery        | 根据条件查询单个     | CriteriaWrapper criteriaWrapper, Class<T> clazz                                                               |
| findOneByQuery        | 根据条件和排序查询单个  | CriteriaWrapper criteriaWrapper, SortBuilder sortBuilder, Class<T> clazz                                      |
| findListByQuery       | 根据条件查询列表     | CriteriaWrapper criteria, Class<T> clazz                                                                      |
| findListByQuery       | 根据条件和排序查询列表  | CriteriaWrapper criteria, SortBuilder sort, Class<T> clazz                                                    |
| findPropertiesByQuery | 根据条件查找某个属性   | CriteriaWrapper criteria, Class<?> documentClass, SerializableFunction<E, R> property, Class<T> propertyClass |
| findPropertiesByIds   | 根据id集合查找某个属性 | List<String> ids, Class<?> clazz, SerializableFunction<E, R> property                                         |
| findPropertiesByQuery | 根据条件查找某个属性	  | CriteriaWrapper criteria, Class<?> documentClass, SerializableFunction<E, R> property                         |
| findIdsByQuery        | 根据条件查询ID列表   | CriteriaWrapper criteria, Class<?> clazz                                                                      |
| findListByIds         | 根据ID列表查询     | Collection<String> ids, Class<T> clazz                                                                        |
| findListByIds         | 根据ID列表和排序查询  | Collection<String> ids, SortBuilder sortBuilder, Class<T> clazz                                               |
| countByQuery          | 根据条件查询数量     | CriteriaWrapper criteriaWrapper, Class<?> clazz                                                               |

### 增删改操作

| 方法             | 说明       | 参数                                              |
|----------------|----------|-------------------------------------------------|
| insert         | 插入       | Object object                                   |
| batchInsert    | 批量插入     | List<T> list                                    |
| updateById     | 根据ID更新   | Object object                                   |
| insertOrUpdate | 插入或更新    | Object object                                   |
| deleteById     | 根据id删除   | String id, Class<?> clazz                       |
| deleteByIds    | 根据id列表删除 | List<String> ids, Class<?> clazz                |
| deleteByQuery  | 根据条件删除   | CriteriaWrapper criteriaWrapper, Class<?> clazz |

## 🧪 使用示例

### OSS使用示例

基本文件操作（丝滑体验）：

```java
package com.silky.starter.oss.template;

import com.silky.starter.oss.OssApplicationTest;
import com.silky.starter.oss.core.enums.OssFileTypeEnum;
import com.silky.starter.oss.model.metadata.OssFileMetadata;
import com.silky.starter.oss.model.param.DeleteFileOssParam;
import com.silky.starter.oss.model.param.DownloadFileOssParam;
import com.silky.starter.oss.model.param.GetFileMetadataParam;
import com.silky.starter.oss.model.param.OssUploadParam;
import com.silky.starter.oss.model.result.OssUploadResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.time.LocalDateTime;

/**
 * OSS模板测试类
 *
 * @author zy
 * @date 2025-08-14 15:52
 **/
public class OssTemplateTest extends OssApplicationTest {
    @Autowired
    private OssTemplate ossTemplate;

    /**
     * 智能上传测试方法
     */
    @Test
    public void testSmartUpload() {
        OssUploadParam param = new OssUploadParam();
        param.setObjectKey("test/1755681995122.png");
        //文件 File和 Stream 二选一
        param.setFile(new File("C:\\Users\\Administrator\\Desktop\\1755681995122.png"));
//        param.setStream();

        //以下参数可选
        param.setExpiration(LocalDateTime.now().plusMinutes(5)); // 设置过期时间为当前时间加5分钟
        param.setContentType(OssFileTypeEnum.PNG.getContentType()); // 设置文件类型为PNG
//        param.setMetadata(); // 设置自定义元数据，如果不需要可以不设置
//        param.setFileSize(); // 设置文件大小，如果不需要可以不设置
//        param.setPublicRead(); // 设置是否公开可读，默认为true
//        param.setEnableEncryption(); // 设置是否启用加密，默认为false
        OssUploadResult result = ossTemplate.smartUpload(param);
        log.info("智能上传测试方法oss上传结果: {}", result);
    }

    /**
     * 删除文件测试
     */
    @Test
    public void testDeleteFile() {
        DeleteFileOssParam param = new DeleteFileOssParam();
        param.setObjectKey("test/1755681995122.png");
        ossTemplate.deleteFile(param);
        log.info("删除文件测试成功");
    }

    /**
     * 将文件下载到本地测试
     */
    @Test
    public void testDownloadFile() {
        DownloadFileOssParam param = new DownloadFileOssParam();
        param.setObjectKey("test/1755681995122.png");
        param.setLocalFilePath("D:\\download\\11.png");
        ossTemplate.downloadFile(param);
        log.info("将文件下载到本地测试");
    }

    /**
     * 获取文件元数据测试
     */
    @Test
    public void testGetFileMetadata() {
        GetFileMetadataParam param = new GetFileMetadataParam();
        param.setObjectKey("test/1755681995122.png");
        OssFileMetadata fileMetadata = ossTemplate.getFileMetadata(param);
        log.info("获取文件元数据测试,响应结果：" + fileMetadata);
    }
}
```

高级功能示例（极致丝滑）：

```java
/**
 * OSS模板测试类
 *
 * @author zy
 * @date 2025-08-14 15:52
 **/
public class OssTemplateTest extends OssApplicationTest {

    // 生成预签名URL（用于临时访问）
    @Test
    public void testGenPreSignedUrl() {
        GenPreSignedUrlParam param = new GenPreSignedUrlParam();
        param.setObjectKey("test/1755681995122.png");
        param.setExpirationMinutes(30); // 30分钟有效期

        GenPreSignedUrlResult result = ossTemplate.genPreSignedUrl(param);
        log.info("预签名URL: {}", result.getUrl());
    }

    // 分片上传大文件
    @Test
    public void testMultipartUpload() {
        // 1. 初始化分片上传
        InitiateMultipartUploadParam initParam = new InitiateMultipartUploadParam();
        initParam.setObjectKey("largefile.zip");
        InitiateMultipartResult initResult = ossTemplate.initiateMultipartUpload(initParam);

        String uploadId = initResult.getUploadId();

        // 2. 上传各个分片
        for (int i = 0; i < partCount; i++) {
            UploadPartParam partParam = new UploadPartParam();
            partParam.setUploadId(uploadId);
            partParam.setPartNumber(i + 1);
            partParam.setInputStream(partInputStream);

            UploadPartResult partResult = ossTemplate.uploadPart(partParam);
            // 保存分片ETag信息
        }

        // 3. 完成分片上传
        CompleteMultipartUploadParam completeParam = new CompleteMultipartUploadParam();
        completeParam.setUploadId(uploadId);
        completeParam.setObjectKey("largefile.zip");
        completeParam.setPartEtags(partEtags);

        CompleteMultipartUploadResult result = ossTemplate.completeMultipartUpload(completeParam);
        log.info("分片上传完成: {}", result);
    }
}
``` 

### MongoDB使用示例

```java
package com.silky.starter.mongodb.example;

import com.silky.starter.mongodb.template.MongodbTemplate;
import com.silky.starter.mongodb.build.SortBuilder;
import com.silky.starter.mongodb.model.page.Page;
import com.silky.starter.mongodb.wrapper.CriteriaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MongoDB操作REST示例
 *
 * @author zy
 * @date 2025-09-04 16:00
 **/
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private MongodbTemplate mongodbTemplate;

    /**
     * 分页查询用户
     */
    @GetMapping
    public Page<User> getUsers(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        Page<User> pageParam = new Page<>();
        pageParam.setCurrent(page);
        pageParam.setSize(size);
        return mongodbTemplate.findPage(pageParam, User.class);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) {
        return mongodbTemplate.findById(id, User.class);
    }

    /**
     * 添加用户
     */
    @PostMapping
    public String addUser(@RequestBody User user) {
        return mongodbTemplate.insert(user);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public String updateUser(@PathVariable String id, @RequestBody User user) {
        user.setId(id);
        return mongodbTemplate.updateById(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable String id) {
        mongodbTemplate.deleteById(id, User.class);
    }

    /**
     * 根据条件查询用户
     */
    @GetMapping("/search")
    public List<User> searchUsers(@RequestParam String name,
                                  @RequestParam(required = false) Integer minAge) {
        CriteriaWrapper criteria = new CriteriaWrapper();
        criteria.eq(User::getName, name);
        if (minAge != null) {
            criteria.gte(User::age, minAge);
        }
        SortBuilder sort = new SortBuilder();
        sort.asc("age");
        return mongodbTemplate.findListByQuery(criteria, sort, User.class);
    }
}

``` 

## 🔄 扩展自定义云服务

```java
// 注册自定义云服务提供商
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

/**
 * OSS模板测试类
 *
 * @author zy
 * @date 2025-08-14 15:52
 **/
public class OssTemplateTest extends OssApplicationTest {

    // 切换云服务提供商
    @Test
    public void testSwitchProvider() {
        // 切换到华为云
        ossTemplate.switchProvider("huawei");

        // 执行上传操作
        OssUploadParam param = new OssUploadParam();
        // ... 设置参数
        OssUploadResult result = ossTemplate.smartUpload(param);
    }
}
```

## 📝 注意事项

### OSS组件注意事项

1. 上传大文件时（超过128MB），组件会自动启用分片上传功能。
2. 支持自定义上传策略，可通过setDefaultUploadStrategy方法设置。
3. 组件内置异常处理机制，所有操作都会抛出统一的OssException。
4. 支持通过switchProvider方法动态切换云服务提供商。

### MongoDB组件注意事项

1. MongoDB操作需要确保已正确配置MongoDB连接信息。
2. 使用MongoDB分页查询时，注意设置合理的分页参数以避免性能问题。
3. 批量操作时注意数据量，避免一次性操作过多数据导致内存溢出。
4. 复杂查询建议使用索引以提高查询性能。

## 🆘 获取帮助

1. 如在使用过程中遇到问题，可以提Issue，或通过以下方式获取支持：824414828@qq.com或者添加微信号:824414828
2. 查看组件源码：
    - OSS组件：silky-oss-spring-boot-starter
    - MongoDB组件：silky-starter-mongodb
3. 参考测试示例：
    - OSS测试：silky-starter-oss-test
    - MongoDB测试：silky-starter-mongodb-test
4. 联系开发团队获取技术支持。

### 💳捐赠

如果你觉得我的工作对你有帮助，可以点个 Star 或捐赠请作者喝杯咖啡~，在此表示感谢！

<img src="https://petsgo.oss-cn-shenzhen.aliyuncs.com/prod/wx.jpg" height="300px" alt="微信"><img src="https://petsgo.oss-cn-shenzhen.aliyuncs.com/prod/alipay.jpg" height="300px" alt="支付宝">

或者点击以下链接，将页面拉到最下方点击“捐赠”即可

[Gitee上捐赠](https://gitee.com/zeng_er/silky-starter)

-------
