<p align="center"> 
<strong>让开发如丝般顺滑的开箱即用组件库</strong> 
</p>
<div align="center">

[![Java 1.8+](https://img.shields.io/badge/Java-1.8+-orange.svg)]()
[![Spring Boot 2.7.x](https://img.shields.io/badge/Spring%20Boot-2.7.x-brightgreen.svg)]()
[![Maven 3.10+](https://img.shields.io/badge/Maven-3.10+-blue.svg)]()
[![License Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)]()

</div>

## 📚 简介

silky-starter 是一款专为提升开发效率而设计的丝滑组件库，取名"silky"寓意开发体验如丝绸般顺滑。组件库提供统一的OSS文件操作接口，支持多种云存储平台，采用智能策略模式设计，让文件上传、下载和管理变得异常简单和高效。

丝滑体验，开箱即用 - 只需简单配置，即可享受丝滑般的开发体验，大幅减少重复代码编写，提升项目质量和稳定性。

## ✨ 丝滑特性
- 🧊 丝滑体验: 简洁API设计，让每次调用都如丝般顺滑
- 🚀 智能上传: 根据文件大小自动选择最佳上传方式，智能又省心
- ⚡ 多云支持: 阿里云OSS、华为云OBS等（可扩展），切换如丝顺滑
- 🔄 断点续传: 大文件分片上传和断点续传，网络波动不影响丝滑体验
- 📊 进度监控: 实时获取上传进度信息，进度展示清晰流畅
- 🔒 安全可控: 支持加密上传和权限控制，安全又不失便捷
- 📦 开箱即用: 简单配置即可快速集成，真正实现五分钟上手

## 📋 目录
- [丝滑特性](#丝滑特性)
- [技术栈要求](#技术栈要求)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [API接口说明](#API接口说明)
- [使用示例](#使用示例)
- [扩展功能](#扩展功能)
- [注意事项](#注意事项)
- [获取帮助](#获取帮助)

## 📜技术栈要求

- JDK 1.8+
- Spring Boot 2.7.x
- Maven 3.10+

## 🗂️ 项目结构

``` text
silky-starter
├─ silky-oss-spring-boot-starter（OSS组件模块，文件、图片上传下载，支持：阿里云、华为云等）
├─ silky-starter-core（核心模块,公共常用的工具包类）
├─ silky-starter-test（测试模块）
│  ├─ silky-starter-oss-test（OSS组件测试模块）
```

## 🚀 快速开始

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

## 🔧 API 接口说明

### 核心方法

| 方法              | 说明               | 参数                   |
|-----------------|------------------|----------------------|
| smartUpload     | 智能上传（自动选择最佳上传方式） | OssUploadParam       |
| downloadFile    | 下载文件到本地          | DownloadFileOssParam |
| getFileMetadata | 删除云端文件           | DeleteFileOssParam   |
| deleteFile      | 获取文件元数据          | GetFileMetadataParam |
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

## 🧪 使用示例

基本文件操作（丝滑体验）：：

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

1. 上传大文件时（超过128MB），组件会自动启用分片上传功能。
2. 支持自定义上传策略，可通过setDefaultUploadStrategy方法设置。
3. 组件内置异常处理机制，所有操作都会抛出统一的OssException。
4. 支持通过switchProvider方法动态切换云服务提供商。

## 🆘 获取帮助

1. 如在使用过程中遇到问题，可通过以下方式获取支持：824414828@qq.com
2. 查看组件源码：silky-oss-spring-boot-starter。
3. 参考测试示例：silky-starter-oss-test。
4. 联系开发团队获取技术支持。


<div align="center">

# silky-starter - 让每一次开发都如丝般顺滑

[![体验-丝滑一般顺滑](https://img.shields.io/badge/体验-丝滑一般顺滑-ff69b4.svg)]()
[![特性-开箱即用](https://img.shields.io/badge/特性-开箱即用-green.svg)]()
[![目标-高效开发](https://img.shields.io/badge/目标-高效开发-blue.svg)]()
</div>


