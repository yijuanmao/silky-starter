
## v1.0.7 (2026-04-30)

### New Features
- **注解驱动字段转换管道**：新增 `@ExcelEnum`、`@ExcelDict`、`@ExcelMask` 注解，导出时自动完成枚举翻译、字典翻译、数据脱敏
- **统一 SPI**：新增 `ExcelFieldResolver` 接口、`ExcelFieldResolverPipeline` 管道、`DictionaryProvider` 字典数据提供者
- **多 Sheet 导出**：支持业务多 Sheet（不同数据源/表头），通过 `ExportSheet` 定义
- **StorageObject**：存储策略 `storeFile` 返回 `StorageObject`（含 key、url、size、etag、expireAt）
- **存储接口增强**：新增 `storeFile(InputStream)` 方法，`downloadFile` 入参统一为 key

### Improvements
- **参数校验**：`maxRowsPerSheet` 必须大于 0 且不超过 Excel 限制 1,048,576
- **字典批量查询**：`DictionaryProvider` 强制批量查询，禁止逐行逐列查库
- **管道执行顺序**：原值 -> 枚举翻译 -> 字典翻译 -> 脱敏 -> 输出
- **LocalStorageStrategy 增强**：实现 `exists()` 和 `getFileSize()` 方法，修复 `downloadFile` 使用 basePath 拼接

### Removed
- 移除 `DataProcessor.converting()`（类型擦除不安全）
- 移除 `DataProcessor.statistics()`（空实现无意义）
- 移除 `ImportEngine.batchTaskCache`（未使用）

### Breaking Changes
- `StorageStrategy.storeFile()` 返回值从 `String` 改为 `StorageObject`
- `ExportEngine` 构造函数新增 `ExcelFieldResolverPipeline` 参数

## v1.0.5 (2026-04-28)
### 💎 功能优化
* hutool工具类 =》 5.8.44版本
