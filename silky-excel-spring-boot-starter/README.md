# silky-excel-spring-boot-starter

基于 FastExcel 的 Spring Boot 导入导出 starter，主打大文件分页导出、多 Sheet、异步任务、进度查询和可扩展文件存储。

## 特性

- 大文件导出：分页查询、分批写入，避免一次性加载全部数据。
- 数据库分页导出：通过 `ExportDataSupplier` 按页拉取数据。
- 多 Sheet：每个 Sheet 可以使用不同 DTO 和不同数据源。
- 异步任务：`exportAsync` / `importAsync` 提交任务后返回 taskId。
- 进度查询：通过 `getExportRecord(taskId)` / `getImportRecord(taskId)` 查询状态。
- 存储扩展：内置本地存储，业务方实现 `StorageStrategy` 即可接入 OSS、COS、S3、MinIO 等。
- 导入支持：按批读取 Excel，并交给业务方 `DataImporterSupplier` 落库。
- 字段转换：支持 `@ExcelEnum`、`@ExcelDict`、`@ExcelMask`。

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>top.silky</groupId>
    <artifactId>silky-excel-spring-boot-starter</artifactId>
    <version>1.0.7</version>
</dependency>
```

### 2. 基础配置

```yaml
silky:
  excel:
    enabled: true
    export:
      page-size: 1000
      max-rows-per-sheet: 200000
      timeout-minutes: 30
      progress-enabled: true
    imports:
      page-size: 1000
      max-error-count: 100
      timeout-minutes: 60
      head-row-number: 1
    storage:
      type: local
      local:
        base-path: /tmp/silky-excel
    async:
      thread-pool:
        core-pool-size: 5
        max-pool-size: 20
        queue-capacity: 100
        keep-alive-seconds: 60
```

### 3. 定义 DTO

```java
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserExcelRow {

    @ExcelProperty("用户ID")
    private Long userId;

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("手机号")
    @ExcelMask(strategy = ExcelMask.MaskStrategy.PHONE)
    private String phone;
}
```

### 4. 分页导出

```java
ExportRequest<UserExcelRow> request = ExportRequest.of(
        "用户列表.xlsx",
        UserExcelRow.class,
        (pageNum, pageSize, params) -> {
            Page<UserExcelRow> page = userService.page(pageNum, pageSize);
            return new ExportPageData<>(page.getRecords(), page.hasNext());
        }
);

ExportResult result = excelTemplate.exportAsync(request);
ExportRecord record = excelTemplate.getExportRecord(result.getTaskId());
```

同步导出：

```java
ExportResult result = excelTemplate.exportSync(request);
String fileUrl = result.getFileUrl();
```

## 多 Sheet 导出

```java
ExportSheet<UserExcelRow> userSheet = ExportSheet.of(
        "用户",
        UserExcelRow.class,
        (pageNum, pageSize, params) -> {
            Page<UserExcelRow> page = userService.page(pageNum, pageSize);
            return new ExportPageData<>(page.getRecords(), page.hasNext());
        }
);

ExportSheet<OrderExcelRow> orderSheet = ExportSheet.of(
        "订单",
        OrderExcelRow.class,
        (pageNum, pageSize, params) -> {
            Page<OrderExcelRow> page = orderService.page(pageNum, pageSize);
            return new ExportPageData<>(page.getRecords(), page.hasNext());
        }
);

ExportRequest<?> request = ExportRequest.sheets("业务数据.xlsx", Arrays.asList(userSheet, orderSheet));
ExportResult result = excelTemplate.exportAsync(request);
```

当某个 Sheet 超过 `maxRowsPerSheet` 时，会自动写入 `SheetName_2`、`SheetName_3`。

## 导入

```java
ImportRequest<UserExcelRow> request = ImportRequest.of(
        "用户列表.xlsx",
        UserExcelRow.class,
        (rows, params) -> {
            userService.saveBatch(rows);
            return DataImporterSupplier.ImportBatchResult.success(rows.size());
        }
);

ImportResult result = excelTemplate.importAsync(request);
ImportRecord record = excelTemplate.getImportRecord(result.getTaskId());
```

## 存储扩展

内置 `local` 存储。接入 OSS、COS、S3、MinIO 时，实现 `StorageStrategy` 并注册为 Spring Bean：

```java
@Component
public class OssExcelStorageStrategy implements StorageStrategy {

    @Override
    public String getType() {
        return "oss";
    }

    @Override
    public StorageObject store(File file, String fileName, Map<String, Object> metadata) {
        String key = ossClient.putObject(file, fileName);
        return StorageObject.builder()
                .key(key)
                .url(ossClient.getUrl(key))
                .size(file.length())
                .build();
    }

    @Override
    public File download(String keyOrUrl) {
        return ossClient.downloadToTempFile(keyOrUrl);
    }
}
```

配置默认存储：

```yaml
silky:
  excel:
    storage:
      type: oss
```

单次请求指定存储：

```java
request.setStorageType("oss");
```

## 字段转换

枚举翻译：

```java
@ExcelProperty("状态")
@ExcelEnum(enumClass = UserStatus.class, codeField = "code", labelField = "label")
private Integer status;
```

字典表达式：

```java
@ExcelProperty("性别")
@ExcelDict(readConverterExp = "0=女,1=男")
private Integer gender;
```

字典 Provider：

```java
@Component
public class CustomDictionaryProvider implements DictionaryProvider {

    @Override
    public Map<String, String> batchQuery(String dictCode, List<String> codes) {
        return dictService.batchGetLabels(dictCode, codes);
    }
}
```

脱敏：

```java
@ExcelProperty("手机号")
@ExcelMask(strategy = ExcelMask.MaskStrategy.PHONE)
private String phone;
```

字段转换顺序：原值 -> 枚举翻译 -> 字典翻译 -> 脱敏 -> 输出。

## 核心接口

```java
public interface ExcelTemplate {

    <T> ExportResult exportSync(ExportRequest<T> request);

    <T> ExportResult exportAsync(ExportRequest<T> request);

    <T> ImportResult importSync(ImportRequest<T> request);

    <T> ImportResult importAsync(ImportRequest<T> request);

    ExportRecord getExportRecord(String taskId);

    ImportRecord getImportRecord(String taskId);
}
```

## License

Apache License 2.0
