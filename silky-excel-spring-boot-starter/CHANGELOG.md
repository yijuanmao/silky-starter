
## v1.0.7 (2026-05-27)

### 💎 功能优化
- **ExcelTemplate API 收口**：导入导出入口统一为 `exportSync`、`exportAsync`、`importSync`、`importAsync`，用户最小使用链路变为定义 DTO、编写分页 supplier、提交异步任务、通过 taskId 查询进度和文件地址。
- **导入导出流程简化**：导出、导入执行逻辑收口到 `DefaultExcelTemplate`，减少 `ExportEngine`、`ImportEngine` 带来的重复调度心智。
- **请求与配置精简**：保留大文件分页、多 Sheet、异步任务、进度、存储扩展、导入批处理等核心配置，移除压缩分片、重复异步模型和未使用配置项。
- **字段解析管道重构**：`ExcelFieldResolverPipeline` 改为页级解析模型，通过 `ResolvedPage` 承载导出展示值，不再维护任务级旁路状态。
- **字段转换不反写 DTO**：`@ExcelEnum`、`@ExcelDict`、`@ExcelMask` 只影响 Excel 单元格输出，避免导出过程修改业务对象。
- **解析器 SPI 泛型化**：`ExcelFieldResolver` 通过 `getAnnotationType()` 声明支持的注解，新增 `BatchExcelFieldResolver` 支持页级批量预处理。
- **字典 Provider 批量优化**：`@ExcelDict(dictCode = "...")` 在每页导出前批量查询字典值，多值字段按 `separator` 拆分后统一翻译。
- **枚举缓存修正**：枚举翻译缓存 key 纳入 `enumClass`、`codeField`、`labelField`，避免同一枚举不同字段映射互相污染。
- **README 更新**：补充分页异步导出、进度查询、存储扩展、导入批处理、字段转换不反写 DTO 和字典批量查询说明。

### 🐛 Bug Fixes
- 修复导入最后不足一批的数据未提交问题。
- 修复导入记录服务空实现导致任务记录无法查询的问题。
- 修复 `@ExcelDict(readConverterExp = "...")` 在没有 `DictionaryProvider` 时不生效的问题。

## v1.0.6 (2026-04-30)

### ✨ 新特性
- **注解驱动字段转换管道**：新增 `@ExcelEnum`、`@ExcelDict`、`@ExcelMask` 注解，导出时自动完成枚举翻译、字典翻译、数据脱敏
- **统一 SPI**：新增 `ExcelFieldResolver` 接口、`ExcelFieldResolverPipeline` 管道、`DictionaryProvider` 字典数据提供者
- **多 Sheet 导出**：支持业务多 Sheet（不同数据源/表头），通过 `ExportSheet` 定义
- **StorageObject**：存储策略 `storeFile` 返回 `StorageObject`（含 key、url、size、etag、expireAt）
- **存储接口增强**：新增 `storeFile(InputStream)` 方法，`downloadFile` 入参统一为 key
- **@ExcelDict 表达式模式**：支持 `readConverterExp` 直接表达式翻译，无需实现 `DictionaryProvider`（如 `"0=女,1=男"`）
- **@ExcelDict 多值分隔符**：支持 `separator` 属性，多编码值逐个翻译后拼接
- **DictionaryProvider 单条查询**：新增 `query(String dictCode, String code)` 默认方法，开发者可选实现

### 💎 功能优化
- **参数校验**：`maxRowsPerSheet` 必须大于 0 且不超过 Excel 限制 1,048,576
- **字典批量查询**：`DictionaryProvider` 强制批量查询，禁止逐行逐列查库
- **管道执行顺序**：原值 -> 枚举翻译 -> 字典翻译 -> 脱敏 -> 输出
- **LocalStorageStrategy 增强**：实现 `exists()` 和 `getFileSize()` 方法，修复 `downloadFile` 使用 basePath 拼接
- **类型安全修复**：Pipeline 对类型不兼容字段（如 Integer -> String）使用旁路存储 + CellWriteHandler 替代直接 Field.set
- **移除** `DataProcessor.converting()`（类型擦除不安全）
- **移除** `DataProcessor.statistics()`（空实现无意义）
- **移除** `ImportEngine.batchTaskCache`（未使用）
- `StorageStrategy.storeFile()` 返回值从 `String` 改为 `StorageObject`
- `ExportEngine` 构造函数新增 `ExcelFieldResolverPipeline` 参数

### 🐛 Bug Fixes
- 修复 `ExcelFieldResolverPipeline` 将 String 解析值 set 到 Integer 字段导致 `IllegalArgumentException` 的问题
- 新增 `ResolveCellWriteHandler`，在单元格写入后用旁路存储的解析值覆盖原始值


## v1.0.5 (2026-04-28)
### 💎 功能优化
* hutool工具类 =》 5.8.44版本
