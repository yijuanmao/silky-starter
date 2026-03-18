# silky-excel-spring-boot-starter

<p align="center">
  <img src="https://img.shields.io/badge/Silky%20Starter-Excel-brightgreen" alt="Silky Starter Excel"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7+-blue" alt="Spring Boot 2.7+"/>
  <img src="https://img.shields.io/badge/JDK-1.8+-orange" alt="JDK 1.8+"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-green" alt="License"/>
</p>

> 高性能、易用的 Excel 导入导出组件，支持百万级数据异步处理、多存储策略、数据压缩等功能。

## 特性

- **高性能导出**：支持百万级数据导出，自动分 Sheet，支持异步处理
- **智能导入**：支持大文件分片读取、数据校验、错误处理
- **多存储策略**：支持本地存储、Redis、MongoDB、OSS 等多种存储方式
- **数据压缩**：支持 ZIP 压缩，减少存储空间占用
- **进度追踪**：实时导出/导入进度监控
- **线程安全**：内置线程池管理，支持并发处理

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.yijuanmao</groupId>
    <artifactId>silky-excel-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 基础配置

```yaml
silky:
  excel:
    enabled: true
    # 异步配置
    async:
      enabled: true
      async-type: THREAD_POOL
      thread-pool:
        core-pool-size: 5
        max-pool-size: 20
        queue-capacity: 100
        keep-alive-seconds: 60
    # 导出配置
    export:
      max-rows-per-sheet: 200000
      batch-size: 1000
      temp-file-path: ./temp/exports
      timeout-minutes: 30
      enable-progress: true
    # 导入配置
    import:
      page-size: 10000
      max-error-count: 100
      temp-file-path: ./temp/imports
      timeout-minutes: 60
      enable-transaction: true
      skip-header: false
    # 存储配置
    storage:
      storage-type: LOCAL
      local:
        base-path: /tmp/silky-excel
        auto-clean: true
        clean-interval: 3600
        retention-days: 7
    # 压缩配置
    compression:
      enabled: false
      type: ZIP
      compression-level: 6
      split-large-files: false
      split-size: 104857600
```

### 3. 数据实体类

```java
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserData {
    
    @ExcelProperty("用户ID")
    private Long userId;
    
    @ExcelProperty("用户名")
    private String username;
    
    @ExcelProperty("邮箱")
    private String email;
    
    @ExcelProperty("手机号")
    private String phone;
    
    @ExcelProperty("创建时间")
    private String createTime;
}
```

## 核心API详解

### ExcelTemplate 接口

`ExcelTemplate` 是组件的核心接口，提供了完整的 Excel 导入导出功能。

#### 导出相关方法

```java
public interface ExcelTemplate {
    
    /**
     * 异步导出数据（使用默认异步方式）
     * 适用于大数据量导出，不会阻塞主线程
     * 
     * @param request 导出请求参数
     * @return 导出结果
     */
    <T> ExportResult exportAsync(ExportRequest<T> request);
    
    /**
     * 同步导出数据（适合小数据量）
     * 适用于数据量较小（< 10万条）的场景
     * 
     * @param request 导出请求参数
     * @return 导出结果
     */
    <T> ExportResult exportSync(ExportRequest<T> request);
    
    /**
     * 导出数据（指定异步类型）
     * 
     * @param request   导出请求参数
     * @param asyncType 异步类型：SYNC(同步) / THREAD_POOL(线程池异步)
     * @return 导出结果
     */
    <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType);
    
    /**
     * 导出数据（支持自定义任务配置）
     * 可以通过 taskConfigurer 自定义任务属性
     * 
     * @param request        导出请求参数
     * @param asyncType      异步类型
     * @param taskConfigurer 任务配置器
     * @return 导出结果
     */
    <T> ExportResult export(ExportRequest<T> request, AsyncType asyncType, Consumer<ExportTask<T>> taskConfigurer);
    
    /**
     * 异步导出（返回CompletableFuture）
     * 支持异步编程模型，可与其他异步操作组合
     * 
     * @param request 导出请求参数
     * @return CompletableFuture 包装的结果
     */
    <T> CompletableFuture<ExportResult> exportFuture(ExportRequest<T> request);
}
```

#### 导入相关方法

```java
public interface ExcelTemplate {
    
    /**
     * 同步导入数据（适合小数据量）
     * 
     * @param request 导入请求参数
     * @return 导入结果
     */
    <T> ImportResult importSync(ImportRequest<T> request);
    
    /**
     * 异步导入数据
     * 适用于大文件导入场景
     * 
     * @param request 导入请求参数
     * @return 导入结果
     */
    <T> ImportResult importAsync(ImportRequest<T> request);
    
    /**
     * 导入数据（指定异步类型）
     * 
     * @param request   导入请求参数
     * @param asyncType 异步类型
     * @return 导入结果
     */
    <T> ImportResult imports(ImportRequest<T> request, AsyncType asyncType);
    
    /**
     * 导入数据（支持自定义任务配置）
     * 
     * @param request        导入请求参数
     * @param asyncType      异步类型
     * @param taskConfigurer 任务配置器
     * @return 导入结果
     */
    <T> ImportResult imports(ImportRequest<T> request, AsyncType asyncType, Consumer<ImportTask<T>> taskConfigurer);
    
    /**
     * 异步导入（返回CompletableFuture）
     * 
     * @param request 导入请求参数
     * @return CompletableFuture 包装的结果
     */
    <T> CompletableFuture<ImportResult> importFuture(ImportRequest<T> request);
}
```

### ExportRequest 导出请求参数

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest<T> {
    
    /**
     * 数据类类型（必填）
     * 用于 Excel 表头生成和数据映射
     */
    private Class<T> dataClass;
    
    /**
     * 导出文件名（必填）
     * 包含文件扩展名，如："用户列表.xlsx"
     */
    private String fileName;
    
    /**
     * 数据供应器（必填）
     * 负责分页获取要导出的数据
     */
    private ExportDataSupplier<T> dataSupplier;
    
    // ========== 可选参数 ==========
    
    /**
     * 业务类型
     * 用于区分不同的导出业务，便于统计和管理
     * 默认值："default_export"
     */
    @Builder.Default
    private String businessType = "default_export";
    
    /**
     * 存储类型
     * 可选值：LOCAL, REDIS, MONGO, OSS
     * 默认值：LOCAL
     */
    @Builder.Default
    private StorageType storageType = StorageType.LOCAL;
    
    /**
     * 分页大小
     * 每次从数据供应器获取的数据条数
     * 默认值：2000
     */
    @Builder.Default
    private int pageSize = 2000;
    
    /**
     * 创建用户
     * 发起导出任务的用户标识
     * 默认值："system"
     */
    @Builder.Default
    private String createUser = "system";
    
    /**
     * 是否启用进度跟踪
     * 启用后会实时更新导出进度
     * 默认值：true
     */
    @Builder.Default
    private boolean enableProgress = true;
    
    /**
     * 超时时间（分钟）
     * 导出任务的最大执行时间
     */
    private Long timeout;
    
    /**
     * 每个 Sheet 的最大行数
     * 超过该行数会自动创建新 Sheet
     * 默认值：200000
     */
    private Long maxRowsPerSheet;
    
    /**
     * 数据处理器列表
     * 对数据进行转换、过滤、脱敏等处理
     */
    private List<DataProcessor<T>> processors;
    
    /**
     * 是否启用压缩
     */
    private boolean compressionEnabled;
    
    /**
     * 压缩类型：ZIP
     */
    private CompressionType compressionType;
}
```

### ImportRequest 导入请求参数

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRequest<T> {
    
    /**
     * 数据类类型（必填）
     * 用于 Excel 列到 Java 对象的映射
     */
    private Class<T> dataClass;
    
    /**
     * 导入文件名（必填）
     * 包含文件扩展名，如："用户数据.xlsx"
     */
    private String fileName;
    
    /**
     * 文件访问 URL 或存储 Key（必填）
     * 指定要导入的 Excel 文件位置
     */
    private String fileUrl;
    
    /**
     * 数据导入器（必填）
     * 负责将处理后的数据持久化到目标系统
     */
    private DataImporterSupplier<T> dataImporterSupplier;
    
    // ========== 可选参数 ==========
    
    /**
     * 业务类型
     * 默认值："default_import"
     */
    @Builder.Default
    private String businessType = "default_import";
    
    /**
     * 分页大小
     * 每次处理的数据条数
     * 默认值：1000
     */
    private Integer pageSize;
    
    /**
     * 是否启用事务
     * 导入失败时回滚已处理数据
     * 默认值：false
     */
    @Builder.Default
    private boolean enableTransaction = false;
    
    /**
     * 是否跳过表头
     * 跳过 Excel 的第一行（表头）
     * 默认值：true
     */
    @Builder.Default
    private boolean skipHeader = true;
    
    /**
     * 最大错误数量
     * 超过此数量时停止导入
     */
    private Integer maxErrorCount;
    
    /**
     * 最大读取数量
     * 限制导入的最大数据行数
     */
    private Integer maxReadCount;
    
    /**
     * 数据处理器列表
     * 对导入的数据进行校验、转换、过滤等处理
     */
    private List<DataProcessor<T>> processors;
}
```

## 使用示例

### 基础导出示例

```java
import com.silky.starter.excel.template.ExcelTemplate;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.export.ExportDataSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ExportController {
    
    @Autowired
    private ExcelTemplate excelTemplate;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/export/users")
    public ExportResult exportUsers() {
        // 构建数据供应器
        ExportDataSupplier<UserData> dataSupplier = (pageNum, pageSize, params) -> {
            // 从数据库分页查询数据
            return userService.getUserPage(pageNum, pageSize);
        };
        
        // 构建导出请求
        ExportRequest<UserData> request = ExportRequest.<UserData>builder()
                .dataClass(UserData.class)
                .fileName("用户列表.xlsx")
                .dataSupplier(dataSupplier)
                .businessType("user_export")
                .pageSize(1000)
                .createUser("admin")
                .build();
        
        // 执行导出（异步方式）
        ExportResult result = excelTemplate.exportAsync(request);
        
        return result;
    }
}
```

### 大数据量导出示例

```java
@GetMapping("/export/users-large")
public ExportResult exportUsersLarge() {
    ExportDataSupplier<UserData> dataSupplier = (pageNum, pageSize, params) -> {
        // 大数据量分页查询
        return userService.getUserPage(pageNum, pageSize);
    };
    
    ExportRequest<UserData> request = ExportRequest.<UserData>builder()
            .dataClass(UserData.class)
            .fileName("用户列表_大数据量.xlsx")
            .dataSupplier(dataSupplier)
            .businessType("user_export_large")
            .pageSize(5000)  // 增大分页大小
            .maxRowsPerSheet(100000L)  // 每个 Sheet 10 万行
            .compressionEnabled(true)  // 启用压缩
            .compressionType(CompressionType.ZIP)
            .build();
    
    // 使用 CompletableFuture 处理异步结果
    CompletableFuture<ExportResult> future = excelTemplate.exportFuture(request);
    
    // 可以添加完成回调
    future.whenComplete((result, exception) -> {
        if (exception != null) {
            log.error("导出失败", exception);
        } else {
            log.info("导出完成: {}", result.getSummary());
        }
    });
    
    // 返回异步提交结果
    return ExportResult.asyncSuccess("任务已提交");
}
```

### 带数据处理器的导出

```java
@GetMapping("/export/users-processed")
public ExportResult exportUsersWithProcessor() {
    
    // 创建数据处理器（如数据脱敏）
    DataProcessor<UserData> maskProcessor = data -> {
        // 手机号脱敏
        if (data.getPhone() != null) {
            data.setPhone(data.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        }
        // 邮箱脱敏
        if (data.getEmail() != null) {
            data.setEmail(data.getEmail().replaceAll("(\\w{2})\\w+(@\\w+)", "$1****$2"));
        }
        return data;
    };
    
    ExportDataSupplier<UserData> dataSupplier = (pageNum, pageSize, params) -> {
        return userService.getUserPage(pageNum, pageSize);
    };
    
    ExportRequest<UserData> request = ExportRequest.<UserData>builder()
            .dataClass(UserData.class)
            .fileName("用户列表_脱敏.xlsx")
            .dataSupplier(dataSupplier)
            .businessType("user_export_masked")
            .processors(Arrays.asList(maskProcessor))
            .build();
    
    return excelTemplate.exportAsync(request);
}
```

### 基础导入示例

```java
@PostMapping("/import/users")
public ImportResult importUsers(@RequestParam("file") MultipartFile file) throws IOException {
    
    // 保存上传的文件到临时目录
    String fileName = file.getOriginalFilename();
    String filePath = "/tmp/imports/" + fileName;
    file.transferTo(new File(filePath));
    
    // 构建数据导入器
    DataImporterSupplier<UserData> importer = dataList -> {
        // 批量保存到数据库
        userService.saveBatch(dataList);
        return dataList.size();
    };
    
    ImportRequest<UserData> request = ImportRequest.<UserData>builder()
            .dataClass(UserData.class)
            .fileName(fileName)
            .fileUrl(filePath)
            .dataImporterSupplier(importer)
            .businessType("user_import")
            .pageSize(1000)
            .skipHeader(true)
            .enableTransaction(true)
            .build();
    
    // 执行导入
    ImportResult result = excelTemplate.importAsync(request);
    
    return result;
}
```

### 带数据校验的导入

```java
@PostMapping("/import/users-validated")
public ImportResult importUsersWithValidation(@RequestParam("file") MultipartFile file) throws IOException {
    
    String fileName = file.getOriginalFilename();
    String filePath = "/tmp/imports/" + fileName;
    file.transferTo(new File(filePath));
    
    // 创建数据校验处理器
    DataProcessor<UserData> validator = data -> {
        // 校验用户名
        if (StrUtil.isBlank(data.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        // 校验邮箱格式
        if (!Validator.isEmail(data.getEmail())) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        // 校验手机号格式
        if (!Validator.isMobile(data.getPhone())) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        return data;
    };
    
    DataImporterSupplier<UserData> importer = dataList -> {
        userService.saveBatch(dataList);
        return dataList.size();
    };
    
    ImportRequest<UserData> request = ImportRequest.<UserData>builder()
            .dataClass(UserData.class)
            .fileName(fileName)
            .fileUrl(filePath)
            .dataImporterSupplier(importer)
            .businessType("user_import_validated")
            .processors(Arrays.asList(validator))
            .maxErrorCount(100)  // 最多允许 100 条错误
            .enableTransaction(true)
            .build();
    
    ImportResult result = excelTemplate.importAsync(request);
    
    // 输出导入结果摘要
    log.info(result.getSummary());
    
    return result;
}
```

### 导出结果处理

```java
@GetMapping("/export/status/{taskId}")
public ExportResult getExportStatus(@PathVariable String taskId) {
    // 获取导出引擎状态
    ExportEngine.EngineStatus status = excelTemplate.getExportEngineStatus();
    
    log.info("引擎运行时间: {}ms", status.getUptime());
    log.info("总处理任务: {}", status.getTotalProcessedTasks());
    log.info("成功任务: {}", status.getSuccessTasks());
    log.info("失败任务: {}", status.getFailedTasks());
    
    // 根据 taskId 查询具体任务状态（需自行实现记录查询）
    // ...
    
    return ExportResult.success(taskId);
}
```

## 高级特性

### 1. 多存储策略

组件支持多种存储方式，可根据需求选择：

```java
// 使用本地存储
ExportRequest<UserData> request = ExportRequest.<UserData>builder()
        .storageType(StorageType.LOCAL)
        // ...
        .build();

// 使用 OSS 存储
ExportRequest<UserData> request = ExportRequest.<UserData>builder()
        .storageType(StorageType.OSS)
        .fileMetadata(Map.of("bucket", "my-bucket", "path", "exports/"))
        // ...
        .build();
```

### 2. 自定义线程池

```java
@Configuration
public class ExcelConfig {
    
    @Bean("customExcelTaskExecutor")
    public ThreadPoolTaskExecutor customExcelTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("custom-excel-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 3. 导出进度监听

```java
@Component
public class ExportProgressListener {
    
    @EventListener
    public void onExportProgress(ExportProgressEvent event) {
        log.info("导出任务 [{}] 进度: {}/{} ({:.1f}%)", 
                event.getTaskId(),
                event.getProcessedCount(),
                event.getTotalCount(),
                event.getProgress() * 100);
        
        // 可以推送 WebSocket 消息给前端
        // websocketService.sendProgress(event);
    }
}
```

## 配置属性详解

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `silky.excel.enabled` | true | 是否启用 Excel 组件 |
| `silky.excel.async.enabled` | true | 是否启用异步处理 |
| `silky.excel.async.async-type` | THREAD_POOL | 默认异步类型 |
| `silky.excel.async.thread-pool.core-pool-size` | 5 | 核心线程数 |
| `silky.excel.async.thread-pool.max-pool-size` | 20 | 最大线程数 |
| `silky.excel.async.thread-pool.queue-capacity` | 100 | 队列容量 |
| `silky.excel.export.max-rows-per-sheet` | 200000 | 每个 Sheet 最大行数 |
| `silky.excel.export.batch-size` | 1000 | 批处理大小 |
| `silky.excel.export.timeout-minutes` | 30 | 导出超时时间（分钟） |
| `silky.excel.import.page-size` | 10000 | 导入分页大小 |
| `silky.excel.import.max-error-count` | 100 | 最大错误数量 |
| `silky.excel.import.enable-transaction` | true | 是否启用事务 |
| `silky.excel.storage.storage-type` | LOCAL | 存储类型 |
| `silky.excel.storage.local.base-path` | /tmp/silky-excel | 本地存储路径 |
| `silky.excel.storage.local.auto-clean` | true | 是否自动清理 |
| `silky.excel.compression.enabled` | false | 是否启用压缩 |
| `silky.excel.compression.type` | ZIP | 压缩类型 |
| `silky.excel.compression.compression-level` | 6 | 压缩级别 0-9 |

## 常见问题

### Q1: 导出大数据量时内存溢出？

A: 使用分页查询和异步导出，调整 `pageSize` 和 `maxRowsPerSheet` 参数：

```java
ExportRequest<UserData> request = ExportRequest.<UserData>builder()
        .pageSize(5000)  // 适当增大分页大小
        .maxRowsPerSheet(100000L)  // 限制每个 Sheet 行数
        // ...
        .build();
```

### Q2: 如何自定义 Excel 样式？

A: 使用 FastExcel 的注解自定义样式：

```java
@Data
public class UserData {
    
    @ExcelProperty(value = "用户ID", index = 0)
    @ColumnWidth(15)
    private Long userId;
    
    @ExcelProperty(value = "用户名", index = 1)
    @ColumnWidth(20)
    private String username;
    
    @ExcelProperty(value = "状态", index = 2)
    @Dict(dictCode = "user_status")  // 自定义字典转换
    private Integer status;
}
```

### Q3: 导入时如何处理重复数据？

A: 在数据处理器中实现去重逻辑：

```java
DataProcessor<UserData> deduplicateProcessor = data -> {
    // 查询数据库是否已存在
    if (userService.existsByUsername(data.getUsername())) {
        throw new DuplicateKeyException("用户名已存在: " + data.getUsername());
    }
    return data;
};
```

## 相关链接

- [GitHub 仓库](https://github.com/yijuanmao/silky-starter)
- [Gitee 仓库](https://gitee.com/zeng_er/silky-starter)
- [FastExcel 文档](https://fastexcel.idev.cn/)

## License

Apache License 2.0
