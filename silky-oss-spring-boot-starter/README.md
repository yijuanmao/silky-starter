# silky-oss-spring-boot-starter

<p align="center">
  <img src="https://img.shields.io/badge/Silky%20Starter-OSS-brightgreen" alt="Silky Starter OSS"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7+-blue" alt="Spring Boot 2.7+"/>
  <img src="https://img.shields.io/badge/Aliyun%20OSS-supported-orange" alt="Aliyun OSS"/>
  <img src="https://img.shields.io/badge/Huawei%20OBS-supported-orange" alt="Huawei OBS"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-green" alt="License"/>
</p>

> 统一云存储操作组件，支持阿里云 OSS、华为云 OBS，提供智能上传、断点续传、分片上传等高级功能。

## 特性

- **多厂商支持**：阿里云 OSS、华为云 OBS 统一 API
- **智能上传**：根据文件大小自动选择上传策略
- **断点续传**：大文件上传中断后可恢复
- **分片上传**：支持大文件分片并行上传
- **预签名 URL**：生成临时访问链接
- **多线程加速**：内置线程池提升上传效率
- **进度监控**：实时获取上传进度

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.yijuanmao</groupId>
    <artifactId>silky-oss-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 阿里云 OSS SDK（按需引入） -->
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.4</version>
</dependency>

<!-- 华为云 OBS SDK（按需引入） -->
<dependency>
    <groupId>com.huaweicloud</groupId>
    <artifactId>esdk-obs-java</artifactId>
    <version>3.23.3</version>
</dependency>
```

### 2. 基础配置

#### 阿里云 OSS 配置

```yaml
silky:
  oss:
    # 服务商类型
    provider: aliyun
    # AccessKey ID
    access-key: your-access-key-id
    # AccessKey Secret
    secret-key: your-access-key-secret
    # 默认存储桶
    bucket: your-bucket-name
    # 自动分片上传阈值（字节），默认 128MB
    multipart-threshold: 134217728
    # 分片大小（字节），默认 32MB
    multipart-part-size: 33554432
    # 阿里云特有配置
    aliyun:
      endpoint: oss-cn-hangzhou.aliyuncs.com
```

#### 华为云 OBS 配置

```yaml
silky:
  oss:
    provider: huawei
    access-key: your-access-key-id
    secret-key: your-access-key-secret
    bucket: your-bucket-name
    multipart-threshold: 134217728
    multipart-part-size: 33554432
    # 华为云特有配置
    hua-wei:
      endpoint: obs.cn-north-4.myhuaweicloud.com
```

## 核心API详解

### OssTemplate 接口

`OssTemplate` 是组件的核心接口，提供了统一的云存储操作 API。

#### 智能上传

```java
public interface OssTemplate {
    
    /**
     * 智能上传
     * 根据文件大小自动选择上传策略：
     * - 小文件：标准上传
     * - 大文件：自动分片上传
     * - 已上传部分：断点续传
     * 
     * @param param 上传参数
     * @return 上传结果
     * @throws OssException 上传异常
     */
    OssUploadResult smartUpload(OssUploadParam param) throws OssException;
    
    /**
     * 带回调的标准上传
     * 支持上传进度监控
     * 
     * @param param    上传参数
     * @param callback 上传回调接口
     * @return 上传结果
     */
    OssUploadResult standardUploadWithCallback(OssUploadParam param, OssUploadCallback callback) throws OssException;
}
```

#### 分片上传

```java
public interface OssTemplate {
    
    /**
     * 初始化分片上传
     * 获取 uploadId，用于后续分片上传
     * 
     * @param param 初始化参数
     * @return 初始化结果，包含 uploadId
     */
    InitiateMultipartResult initiateMultipartUpload(InitiateMultipartUploadParam param) throws OssException;
    
    /**
     * 上传分片
     * 
     * @param param 分片上传参数
     * @return 分片上传结果
     */
    UploadPartResult uploadPart(UploadPartParam param) throws OssException;
    
    /**
     * 完成分片上传
     * 合并所有已上传的分片
     * 
     * @param param 完成参数
     * @return 完成结果
     */
    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadParam param) throws OssException;
    
    /**
     * 终止分片上传
     * 取消并清理已上传的分片
     * 
     * @param param 终止参数
     */
    void abortMultipartUpload(AbortMultipartUploadParam param) throws OssException;
    
    /**
     * 获取已上传的分片列表
     * 用于断点续传时获取已上传进度
     * 
     * @param param 查询参数
     * @return 已上传的分片信息列表
     */
    List<UploadedPart> listUploadedParts(ListPartsParam param) throws OssException;
}
```

#### 断点续传

```java
public interface OssTemplate {
    
    /**
     * 创建断点续传检查点
     * 保存当前上传进度到检查点
     * 
     * @param param 上传参数
     * @return 检查点信息
     */
    Checkpoint createCheckpoint(OssUploadParam param) throws OssException;
    
    /**
     * 断点续传上传
     * 从检查点恢复上传
     * 
     * @param param      上传参数
     * @param checkpoint 检查点信息
     * @return 上传结果
     */
    OssUploadResult resumeUpload(OssUploadParam param, Checkpoint checkpoint) throws OssException;
}
```

#### 文件管理

```java
public interface OssTemplate {
    
    /**
     * 下载文件到本地
     * 
     * @param param 下载参数
     */
    void downloadFile(DownloadFileOssParam param) throws OssException;
    
    /**
     * 删除文件
     * 
     * @param param 删除参数
     */
    void deleteFile(DeleteFileOssParam param) throws OssException;
    
    /**
     * 获取文件元数据
     * 
     * @param param 查询参数
     * @return 文件元数据
     */
    OssFileMetadata getFileMetadata(GetFileMetadataParam param) throws OssException;
    
    /**
     * 生成预签名 URL
     * 生成带过期时间的临时访问链接
     * 
     * @param param 参数
     * @return 预签名 URL 结果
     */
    GenPreSignedUrlResult genPreSignedUrl(GenPreSignedUrlParam param) throws OssException;
}
```

#### 进度监控

```java
public interface OssTemplate {
    
    /**
     * 获取上传进度
     * 
     * @param uploadId 上传任务 ID
     * @return 上传进度信息
     */
    UploadProgress getUploadProgress(String uploadId) throws OssException;
    
    /**
     * 取消上传任务
     * 
     * @param uploadId 上传任务 ID
     */
    void cancelUpload(String uploadId) throws OssException;
}
```

#### 多厂商切换

```java
public interface OssTemplate {
    
    /**
     * 切换服务提供商
     * 动态切换阿里云/华为云等厂商
     * 
     * @param provider 服务提供商名称，参照 ProviderType 枚举
     */
    void switchProvider(String provider) throws OssException;
    
    /**
     * 注册自定义服务提供商
     * 
     * @param providerName 服务商名称
     * @param adapter      服务商适配器
     */
    void registerProvider(String providerName, OssProviderAdapter adapter) throws OssException;
}
```

### OssUploadParam 上传参数

```java
public class OssUploadParam implements Serializable {
    
    /**
     * 文件上传（与 stream 二选一）
     */
    private File file;
    
    /**
     * 输入流上传（与 file 二选一）
     */
    private InputStream stream;
    
    /**
     * OSS 存储的相对路径
     * 如："upload/2024/01/15/"
     */
    private String path;
    
    /**
     * 保存的文件名
     * 如："document.pdf"
     */
    private String fileName;
    
    /**
     * 过期时间
     * 设置后文件将在指定时间后过期
     */
    private LocalDateTime expiration;
    
    /**
     * 内容类型（MIME Type）
     * 参照 OssFileTypeEnum 枚举
     */
    private String contentType;
    
    /**
     * 自定义元数据
     */
    private Map<String, String> metadata;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 是否公开可读
     * 默认 true
     */
    private boolean publicRead = true;
    
    /**
     * 是否启用服务端加密
     * 默认 false
     */
    private boolean enableEncryption = false;
}
```

### OssUploadResult 上传结果

```java
public class OssUploadResult {
    
    /**
     * 上传是否成功
     */
    private boolean success;
    
    /**
     * 文件访问 URL
     */
    private String url;
    
    /**
     * 文件在 OSS 上的路径
     */
    private String ossPath;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 上传任务 ID（分片上传时使用）
     */
    private String uploadId;
    
    /**
     * 上传耗时（毫秒）
     */
    private Long costTime;
    
    /**
     * 上传类型
     */
    private String uploadType;
    
    /**
     * 元数据
     */
    private Map<String, String> metadata;
}
```

### OssFileMetadata 文件元数据

```java
public class OssFileMetadata {
    
    /**
     * 文件路径
     */
    private String path;
    
    /**
     * 文件大小
     */
    private Long size;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 最后修改时间
     */
    private Date lastModified;
    
    /**
     * ETag
     */
    private String etag;
    
    /**
     * 自定义元数据
     */
    private Map<String, String> userMetadata;
}
```

## 使用示例

### 基础文件上传

```java
import com.silky.starter.oss.template.OssTemplate;
import com.silky.starter.oss.model.param.OssUploadParam;
import com.silky.starter.oss.model.result.OssUploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/oss")
public class OssController {
    
    @Autowired
    private OssTemplate ossTemplate;
    
    /**
     * 文件上传（MultipartFile）
     */
    @PostMapping("/upload")
    public OssUploadResult upload(@RequestParam("file") MultipartFile file) throws IOException {
        OssUploadParam param = new OssUploadParam();
        param.setStream(file.getInputStream());
        param.setFileName(file.getOriginalFilename());
        param.setPath("upload/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/");
        param.setContentType(file.getContentType());
        param.setFileSize(file.getSize());
        
        return ossTemplate.smartUpload(param);
    }
    
    /**
     * 本地文件上传
     */
    @PostMapping("/upload-file")
    public OssUploadResult uploadFile(@RequestParam String localPath) {
        File file = new File(localPath);
        
        OssUploadParam param = new OssUploadParam();
        param.setFile(file);
        param.setFileName(file.getName());
        param.setPath("files/");
        
        return ossTemplate.smartUpload(param);
    }
}
```

### 带进度监控的上传

```java
@RestController
@RequestMapping("/oss")
public class OssController {
    
    @Autowired
    private OssTemplate ossTemplate;
    
    @PostMapping("/upload-with-progress")
    public OssUploadResult uploadWithProgress(@RequestParam("file") MultipartFile file) throws IOException {
        
        OssUploadParam param = new OssUploadParam();
        param.setStream(file.getInputStream());
        param.setFileName(file.getOriginalFilename());
        param.setPath("upload/");
        param.setFileSize(file.getSize());
        
        // 创建进度回调
        OssUploadCallback callback = new OssUploadCallback() {
            @Override
            public void onProgress(long uploadedBytes, long totalBytes) {
                double percent = (double) uploadedBytes / totalBytes * 100;
                log.info("上传进度: {}/{} ({:.1f}%)", uploadedBytes, totalBytes, percent);
                
                // 可以推送 WebSocket 消息给前端
                // websocketService.sendProgress(uploadedBytes, totalBytes);
            }
            
            @Override
            public void onSuccess(OssUploadResult result) {
                log.info("上传成功: {}", result.getUrl());
            }
            
            @Override
            public void onError(Exception e) {
                log.error("上传失败", e);
            }
        };
        
        return ossTemplate.standardUploadWithCallback(param, callback);
    }
}
```

### 大文件分片上传

```java
@RestController
@RequestMapping("/oss/multipart")
public class MultipartUploadController {
    
    @Autowired
    private OssTemplate ossTemplate;
    
    /**
     * 初始化分片上传
     */
    @PostMapping("/init")
    public InitiateMultipartResult initiateUpload(@RequestParam String fileName) {
        InitiateMultipartUploadParam param = new InitiateMultipartUploadParam();
        param.setFileName(fileName);
        param.setPath("large-files/");
        
        return ossTemplate.initiateMultipartUpload(param);
    }
    
    /**
     * 上传分片
     */
    @PostMapping("/upload-part")
    public UploadPartResult uploadPart(
            @RequestParam String uploadId,
            @RequestParam String fileName,
            @RequestParam int partNumber,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        UploadPartParam param = new UploadPartParam();
        param.setUploadId(uploadId);
        param.setFileName(fileName);
        param.setPartNumber(partNumber);
        param.setInputStream(file.getInputStream());
        param.setPartSize(file.getSize());
        
        return ossTemplate.uploadPart(param);
    }
    
    /**
     * 完成分片上传
     */
    @PostMapping("/complete")
    public CompleteMultipartUploadResult completeUpload(
            @RequestParam String uploadId,
            @RequestParam String fileName,
            @RequestBody List<UploadedPart> parts) {
        
        CompleteMultipartUploadParam param = new CompleteMultipartUploadParam();
        param.setUploadId(uploadId);
        param.setFileName(fileName);
        param.setParts(parts);
        
        return ossTemplate.completeMultipartUpload(param);
    }
}
```

### 断点续传实现

```java
@Service
public class ResumeUploadService {
    
    @Autowired
    private OssTemplate ossTemplate;
    
    /**
     * 创建断点续传任务
     */
    public Checkpoint createResumeTask(String filePath) {
        File file = new File(filePath);
        
        OssUploadParam param = new OssUploadParam();
        param.setFile(file);
        param.setFileName(file.getName());
        param.setPath("large-files/");
        
        return ossTemplate.createCheckpoint(param);
    }
    
    /**
     * 执行断点续传
     */
    public OssUploadResult resumeUpload(Checkpoint checkpoint) {
        OssUploadParam param = new OssUploadParam();
        param.setFile(new File(checkpoint.getFilePath()));
        param.setFileName(checkpoint.getFileName());
        param.setPath(checkpoint.getPath());
        
        return ossTemplate.resumeUpload(param, checkpoint);
    }
    
    /**
     * 保存检查点到本地（用于恢复）
     */
    public void saveCheckpoint(Checkpoint checkpoint, String savePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(savePath), checkpoint);
    }
    
    /**
     * 从本地加载检查点
     */
    public Checkpoint loadCheckpoint(String savePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(savePath), Checkpoint.class);
    }
}
```

### 文件下载

```java
@RestController
@RequestMapping("/oss")
public class OssController {
    
    @Autowired
    private OssTemplate ossTemplate;
    
    /**
     * 下载文件到本地
     */
    @GetMapping("/download")
    public String downloadFile(@RequestParam String ossPath, @RequestParam String localPath) {
        DownloadFileOssParam param = new DownloadFileOssParam();
        param.setOssPath(ossPath);
        param.setLocalPath(localPath);
        
        ossTemplate.downloadFile(param);
        return "下载完成: " + localPath;
    }
    
    /**
     * 生成预签名下载链接
     */
    @GetMapping("/presigned-url")
    public String generatePresignedUrl(@RequestParam String ossPath, @RequestParam int expireMinutes) {
        GenPreSignedUrlParam param = new GenPreSignedUrlParam();
        param.setOssPath(ossPath);
        param.setExpiration(DateUtil.offsetMinute(new Date(), expireMinutes));
        
        GenPreSignedUrlResult result = ossTemplate.genPreSignedUrl(param);
        return result.getUrl();
    }
}
```

### 文件管理

```java
@RestController
@RequestMapping("/oss")
public class OssController {
    
    @Autowired
    private OssTemplate ossTemplate;
    
    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public String deleteFile(@RequestParam String ossPath) {
        DeleteFileOssParam param = new DeleteFileOssParam();
        param.setOssPath(ossPath);
        
        ossTemplate.deleteFile(param);
        return "删除成功";
    }
    
    /**
     * 获取文件元数据
     */
    @GetMapping("/metadata")
    public OssFileMetadata getMetadata(@RequestParam String ossPath) {
        GetFileMetadataParam param = new GetFileMetadataParam();
        param.setOssPath(ossPath);
        
        return ossTemplate.getFileMetadata(param);
    }
}
```

### 多厂商切换

```java
@Service
public class MultiCloudService {
    
    @Autowired
    private OssTemplate ossTemplate;
    
    /**
     * 上传到阿里云
     */
    public OssUploadResult uploadToAliyun(OssUploadParam param) {
        ossTemplate.switchProvider("aliyun");
        return ossTemplate.smartUpload(param);
    }
    
    /**
     * 上传到华为云
     */
    public OssUploadResult uploadToHuawei(OssUploadParam param) {
        ossTemplate.switchProvider("huawei");
        return ossTemplate.smartUpload(param);
    }
    
    /**
     * 多厂商备份上传
     */
    public Map<String, OssUploadResult> uploadToMultiCloud(OssUploadParam param) {
        Map<String, OssUploadResult> results = new HashMap<>();
        
        // 上传到阿里云
        try {
            ossTemplate.switchProvider("aliyun");
            results.put("aliyun", ossTemplate.smartUpload(param));
        } catch (Exception e) {
            log.error("阿里云上传失败", e);
        }
        
        // 上传到华为云
        try {
            ossTemplate.switchProvider("huawei");
            results.put("huawei", ossTemplate.smartUpload(param));
        } catch (Exception e) {
            log.error("华为云上传失败", e);
        }
        
        return results;
    }
}
```

### 上传进度查询

```java
@RestController
@RequestMapping("/oss/progress")
public class UploadProgressController {
    
    @Autowired
    private OssTemplate ossTemplate;
    
    /**
     * 获取上传进度
     */
    @GetMapping("/{uploadId}")
    public UploadProgress getProgress(@PathVariable String uploadId) {
        return ossTemplate.getUploadProgress(uploadId);
    }
    
    /**
     * 取消上传
     */
    @PostMapping("/cancel/{uploadId}")
    public String cancelUpload(@PathVariable String uploadId) {
        ossTemplate.cancelUpload(uploadId);
        return "上传已取消";
    }
}
```

## 配置属性详解

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `silky.oss.provider` | - | 服务商类型：`aliyun` / `huawei` |
| `silky.oss.access-key` | - | AccessKey ID |
| `silky.oss.secret-key` | - | AccessKey Secret |
| `silky.oss.bucket` | - | 默认存储桶名称 |
| `silky.oss.multipart-threshold` | 134217728 | 自动分片阈值（字节），默认 128MB |
| `silky.oss.multipart-part-size` | 33554432 | 分片大小（字节），默认 32MB |
| `silky.oss.aliyun.endpoint` | - | 阿里云 Endpoint |
| `silky.oss.hua-wei.endpoint` | - | 华为云 Endpoint |

## 常见问题

### Q1: 如何选择上传方式？

A: 组件会根据文件大小自动选择：
- 小于 `multipart-threshold`：标准上传
- 大于 `multipart-threshold`：分片上传

也可以手动指定：

```java
// 强制使用标准上传
OssUploadParam param = new OssUploadParam();
param.setFile(file);
param.setFileName("small.txt");
ossTemplate.standardUploadWithCallback(param, callback);

// 强制使用分片上传
InitiateMultipartUploadParam initParam = new InitiateMultipartUploadParam();
initParam.setFileName("large.zip");
InitiateMultipartResult result = ossTemplate.initiateMultipartUpload(initParam);
```

### Q2: 断点续传如何实现？

A: 使用检查点机制：

```java
// 1. 创建检查点
Checkpoint checkpoint = ossTemplate.createCheckpoint(param);

// 2. 保存检查点到本地（上传中断时）
// ...

// 3. 恢复上传时加载检查点
Checkpoint loadedCheckpoint = loadCheckpoint();
OssUploadResult result = ossTemplate.resumeUpload(param, loadedCheckpoint);
```

### Q3: 如何配置多线程上传？

A: 组件内置线程池配置：

```java
@Configuration
public class OssThreadPoolConfig {
    
    @Bean
    public ThreadPoolTaskExecutor ossThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("oss-upload-");
        executor.initialize();
        return executor;
    }
}
```

### Q4: 预签名 URL 的有效期如何设置？

A: 通过 `expiration` 参数设置：

```java
GenPreSignedUrlParam param = new GenPreSignedUrlParam();
param.setOssPath("files/document.pdf");

// 设置 1 小时后过期
param.setExpiration(DateUtil.offsetHour(new Date(), 1));

GenPreSignedUrlResult result = ossTemplate.genPreSignedUrl(param);
String url = result.getUrl();  // 有效期 1 小时
```

### Q5: 如何添加自定义元数据？

A: 通过 `metadata` 参数：

```java
OssUploadParam param = new OssUploadParam();
param.setFile(file);
param.setFileName("report.pdf");

Map<String, String> metadata = new HashMap<>();
metadata.put("author", "张三");
metadata.put("department", "技术部");
metadata.put("version", "1.0");
param.setMetadata(metadata);

ossTemplate.smartUpload(param);
```

## 相关链接

- [GitHub 仓库](https://github.com/yijuanmao/silky-starter)
- [Gitee 仓库](https://gitee.com/zeng_er/silky-starter)
- [阿里云 OSS 文档](https://help.aliyun.com/product/31815.html)
- [华为云 OBS 文档](https://support.huaweicloud.com/obs/index.html)

## License

Apache License 2.0
