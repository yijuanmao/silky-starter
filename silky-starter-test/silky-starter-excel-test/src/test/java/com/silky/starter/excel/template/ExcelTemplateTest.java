package com.silky.starter.excel.template;

import com.silky.starter.excel.ExcelApplicationTest;
import com.silky.starter.excel.core.annotation.ExcelDict;
import com.silky.starter.excel.core.annotation.ExcelEnum;
import com.silky.starter.excel.core.annotation.ExcelMask;
import com.silky.starter.excel.core.annotation.ExcelMask.MaskStrategy;
import com.silky.starter.excel.core.engine.EnhancedWriterWrapper;
import com.silky.starter.excel.core.model.export.ExportPageData;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.export.ExportSheet;
import com.silky.starter.excel.core.model.imports.DataImporterSupplier;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.core.resolve.*;
import com.silky.starter.excel.core.storage.StorageObject;
import com.silky.starter.excel.entity.ExportRecord;
import com.silky.starter.excel.entity.ImportRecord;
import com.silky.starter.excel.enums.ExportStatus;
import com.silky.starter.excel.enums.ImportStatus;
import com.silky.starter.excel.template.entity.UserStatus;
import com.silky.starter.excel.template.entity.UserTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExcelTemplate 示例测试类。
 * 目标：按当前组件最新用法演示导入导出，并覆盖核心行为校验。
 */
public class ExcelTemplateTest extends ExcelApplicationTest {

    /**
     * 测试数据源。
     */
    private static final List<UserTest> TEST_DATA = createTestData();

    @Autowired
    private ExcelTemplate excelTemplate;

    /**
     * 测试同步导出。
     */
    @Test
    public void testExportSync() {
        ExportRequest<UserTest> request = ExportRequest.of(
                "D:\\test-export.xlsx",
                UserTest.class,
                this::pageData
        );
        request.setPageSize(50);
        request.setEnableProgress(true);

        ExportResult result = excelTemplate.exportSync(request);
        log.info("同步导出结果: {}", result.getSummary());

        assertTrue(result.isSuccess());
        assertNotNull(result.getTaskId());
        assertNotNull(result.getFileUrl());
        assertTrue(result.getTotalCount() > 0);
        assertTrue(result.getSuccessCount() > 0);
    }

    /**
     * 测试异步导出。
     */
    @Test
    public void testExportAsync() throws InterruptedException {
        ExportRequest<UserTest> request = ExportRequest.of(
                "test-async.xlsx",
                UserTest.class,
                this::pageData
        );
        request.setPageSize(100);
        request.setEnableProgress(true);

        ExportResult result = excelTemplate.exportAsync(request);
        assertTrue(result.isSuccess());
        assertNotNull(result.getTaskId());

        ExportRecord record = waitExportRecord(result.getTaskId(), 4000);
        assertNotNull(record);
        assertNotNull(record.getStatus());
        assertTrue(record.getStatus() == ExportStatus.PENDING
                || record.getStatus() == ExportStatus.PROCESSING
                || record.getStatus() == ExportStatus.COMPLETED);
    }

    /**
     * 测试多Sheet导出。
     */
    @Test
    public void testMultiSheetExport() {
        ExportSheet<UserTest> sheet1 = ExportSheet.of(
                "用户基本信息",
                UserTest.class,
                (pageNum, pageSize, params) -> pageDataFromSource(sheet1Data(), pageNum, pageSize)
        );

        ExportSheet<UserTest> sheet2 = ExportSheet.of(
                "用户详细信息",
                UserTest.class,
                (pageNum, pageSize, params) -> pageDataFromSource(sheet2Data(), pageNum, pageSize)
        );

        ExportRequest<?> request = ExportRequest.sheets("test-multi-sheet.xlsx", asList(sheet1, sheet2));
        request.setPageSize(10);

        ExportResult result = excelTemplate.exportSync(request);
        log.info("多Sheet导出结果: {}", result.getSummary());

        assertTrue(result.isSuccess());
        assertNotNull(result.getFileUrl());
        assertEquals(4L, result.getTotalCount());
        assertTrue(result.getSuccessCount() >= 4);
    }

    /**
     * 测试自动分Sheet导出。
     */
    @Test
    public void testAutoSheetSplit() {
        ExportRequest<UserTest> request = ExportRequest.of(
                "test-auto-split.xlsx",
                UserTest.class,
                this::pageData
        );
        request.setPageSize(10);
        request.setMaxRowsPerSheet(5L);

        ExportResult result = excelTemplate.exportSync(request);
        log.info("自动分Sheet导出结果: {}", result.getSummary());

        assertTrue(result.isSuccess());
        assertTrue(result.getSheetCount() > 1);
    }

    /**
     * 测试最大行数限制。
     */
    @Test
    public void testMaxRowsPerSheetValidation() {
        ExportRequest<UserTest> invalidZero = ExportRequest.of(
                "invalid-zero.xlsx",
                UserTest.class,
                this::pageData
        );
        invalidZero.setMaxRowsPerSheet(0L);
        ExportResult zeroResult = excelTemplate.exportSync(invalidZero);
        assertFalse(zeroResult.isSuccess());
        assertTrue(zeroResult.getMessage().contains("每个 Sheet 最大行数必须在 1 到 1048576 之间"));

        ExportRequest<UserTest> invalidTooLarge = ExportRequest.of(
                "invalid-large.xlsx",
                UserTest.class,
                this::pageData
        );
        invalidTooLarge.setMaxRowsPerSheet(2_000_000L);
        ExportResult tooLargeResult = excelTemplate.exportSync(invalidTooLarge);
        assertFalse(tooLargeResult.isSuccess());
        assertTrue(tooLargeResult.getMessage().contains("每个 Sheet 最大行数必须在 1 到 1048576 之间"));
    }

    /**
     * 测试同步导入。
     */
    @Test
    public void testImportSync() {
        String importFile = exportSampleImportFile("sync");
        ImportRequest<UserTest> request = ImportRequest.of(importFile, UserTest.class,
                (data, params) -> new DataImporterSupplier.ImportBatchResult(data.size(), Collections.emptyList()));
        request.setPageSize(2);
        request.setHeadRowNumber(1);

        ImportResult result = excelTemplate.importSync(request);
        log.info("同步导入结果: {}", result.getSummary());

        assertTrue(result.isSuccess());
        assertNotNull(result.getTaskId());
        assertEquals(3L, result.getTotalCount());
        assertEquals(3L, result.getSuccessCount());
        assertEquals(0L, result.getFailedCount());
    }

    /**
     * 测试异步导入。
     */
    @Test
    public void testImportAsync() throws InterruptedException {
        String importFile = exportSampleImportFile("async");
        ImportRequest<UserTest> request = ImportRequest.of(importFile, UserTest.class,
                (data, params) -> new DataImporterSupplier.ImportBatchResult(data.size(), Collections.emptyList()));
        request.setPageSize(2);
        request.setHeadRowNumber(1);

        ImportResult result = excelTemplate.importAsync(request);
        assertTrue(result.isSuccess());
        assertNotNull(result.getTaskId());

        ImportRecord record = waitImportRecord(result.getTaskId(), 4000);
        assertNotNull(record);
        assertNotNull(record.getStatus());
        assertTrue(record.getStatus() == ImportStatus.PENDING
                || record.getStatus() == ImportStatus.PROCESSING
                || record.getStatus() == ImportStatus.COMPLETED);
    }

    /**
     * 测试存储对象。
     */
    @Test
    public void testStorageObject() {
        StorageObject obj = StorageObject.of("test_key.xlsx", 1024);
        assertEquals("test_key.xlsx", obj.getKey());
        assertEquals(1024L, obj.getSize());

        StorageObject obj2 = StorageObject.builder()
                .key("key2.xlsx")
                .url("/tmp/key2.xlsx")
                .size(2048)
                .etag("abc123")
                .build();

        assertEquals("key2.xlsx", obj2.getKey());
        assertEquals("/tmp/key2.xlsx", obj2.getUrl());
        assertEquals(2048L, obj2.getSize());
        assertEquals("abc123", obj2.getEtag());
    }

    /**
     * 测试导出结果添加错误。
     */
    @Test
    public void testExportResultAddError() {
        ExportResult result = ExportResult.success("task-1");
        result.addError(1, "fieldName", "测试错误");

        assertTrue(result.hasErrors());
        assertEquals(1, result.getErrorCount());
        assertEquals("测试错误", result.getErrors().get(0).getErrorMessage());
    }

    /**
     * 测试脱敏字段解析器。
     */
    @Test
    public void testMaskFieldResolver() throws Exception {
        MaskFieldResolver resolver = new MaskFieldResolver();
        ResolveContext context = new ResolveContext();

        UserTest row = new UserTest();

        Field phoneField = UserTest.class.getDeclaredField("phone");
        phoneField.setAccessible(true);
        Object phoneResult = resolver.resolve(
                new com.silky.starter.excel.core.resolve.FieldValueContext(row, 0, phoneField, context),
                phoneField.getAnnotation(ExcelMask.class),
                "13812345678");
        assertEquals("138****5678", phoneResult);

        Field nameField = UserTest.class.getDeclaredField("name");
        nameField.setAccessible(true);
        Object nameResult = resolver.resolve(
                new com.silky.starter.excel.core.resolve.FieldValueContext(row, 0, nameField, context),
                nameField.getAnnotation(ExcelMask.class),
                "张三丰");
        assertEquals("张**", nameResult);

        Field emailField = UserTest.class.getDeclaredField("email");
        emailField.setAccessible(true);
        Object emailResult = resolver.resolve(
                new com.silky.starter.excel.core.resolve.FieldValueContext(row, 0, emailField, context),
                emailField.getAnnotation(ExcelMask.class),
                "zhangsan@example.com");
        assertNotEquals("zhangsan@example.com", emailResult);
        assertTrue(String.valueOf(emailResult).endsWith("@example.com"));
    }

    /**
     * 测试枚举字段解析器。
     */
    @Test
    public void testEnumFieldResolver() throws Exception {
        EnumFieldResolver resolver = new EnumFieldResolver();
        ResolveContext context = new ResolveContext();
        UserTest row = new UserTest();

        Field statusField = UserTest.class.getDeclaredField("status");
        statusField.setAccessible(true);
        Object result = resolver.resolve(
                new com.silky.starter.excel.core.resolve.FieldValueContext(row, 0, statusField, context),
                statusField.getAnnotation(ExcelEnum.class),
                1);
        assertEquals("启用", result);
    }

    /**
     * 测试注解导出（枚举翻译 + 字典翻译 + 脱敏）整体流程。
     */
    @Test
    public void testExportWithAnnotations() {
        List<UserTest> data = new ArrayList<>();
        data.add(new UserTest("张三丰", "13812345678", "zhangsan@example.com", 1, 1, "110101199001011234", "6222021234567890123"));
        data.add(new UserTest("李四", "13987654321", "lisi@example.com", 0, 2, "320102199501011234", "6222021234567890456"));

        ExportRequest<UserTest> request = ExportRequest.of(
                "test-annotation-export.xlsx",
                UserTest.class,
                (pageNum, pageSize, params) -> new ExportPageData<>(data, false)
        );

        ExportResult result = excelTemplate.exportSync(request);
        log.info("注解导出结果: {}", result.getSummary());
        assertTrue(result.isSuccess());
        assertEquals(2L, result.getTotalCount());
    }

    /**
     * 测试 EnhancedWriterWrapper 的基础行为。
     */
    @Test
    public void testEnhancedWriterWrapper_Basic() throws Exception {
        String file = System.getProperty("java.io.tmpdir") + "/writer-basic-" + System.currentTimeMillis() + ".xlsx";
        EnhancedWriterWrapper writer = new EnhancedWriterWrapper(file, 100);
        try {
            assertEquals("数据_0", writer.getCurrentSheetName());
            assertEquals(0, writer.getCurrentSheetIndex());
            assertEquals(0L, writer.getTotalRows().get());

            writer.write(Collections.emptyList(), Object.class, "测试Sheet");

            assertEquals(0, writer.getCurrentSheetIndex());
            assertEquals(0L, writer.getTotalRows().get());
        } finally {
            writer.close();
            new File(file).delete();
        }
    }

    /**
     * 测试组合注解解析：枚举 + 脱敏。
     */
    @Test
    public void testResolverPipeline_EnumAndMask() {
        List<ExcelFieldResolver<?>> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());
        resolvers.add(new MaskFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        PipelineCombinedDTO dto = new PipelineCombinedDTO();
        dto.setStatusCode(1);

        ResolvedPage page = pipeline.resolve(Collections.singletonList(dto), PipelineCombinedDTO.class, context);
        assertEquals(1, dto.getStatusCode());
        assertEquals("启用", page.getValue(0, "statusCode"));
    }

    /**
     * 测试枚举翻译只影响导出展示值，不修改原始 DTO。
     */
    @Test
    public void testResolverPipeline_EnumNoMutate() {
        List<ExcelFieldResolver<?>> resolvers = Collections.singletonList(new EnumFieldResolver());
        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);

        PipelineStringEnumDTO dto = new PipelineStringEnumDTO();
        dto.setStatus("1");

        ResolvedPage page = pipeline.resolve(Collections.singletonList(dto), PipelineStringEnumDTO.class, new ResolveContext());
        assertEquals("1", dto.getStatus());
        assertEquals("启用", page.getValue(0, "status"));
    }

    /**
     * 测试字典表达式模式翻译。
     */
    @Test
    public void testResolverPipeline_DictExpression() {
        List<ExcelFieldResolver<?>> resolvers = Collections.singletonList(new DictFieldResolver(null));
        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);

        PipelineDictDTO dto = new PipelineDictDTO();
        dto.setUserLevel("1");

        ResolvedPage page = pipeline.resolve(Collections.singletonList(dto), PipelineDictDTO.class, new ResolveContext());
        assertEquals("1", dto.getUserLevel());
        assertEquals("VIP", page.getValue(0, "userLevel"));
    }

    /**
     * 测试无注解字段保持原值且无解析结果。
     */
    @Test
    public void testResolverPipeline_NoAnnotation() {
        List<ExcelFieldResolver<?>> resolvers = asList(new EnumFieldResolver(), new MaskFieldResolver());
        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);

        PipelineNoAnnotationDTO dto = new PipelineNoAnnotationDTO();
        dto.setName("张三");
        dto.setAge(25);

        ResolvedPage page = pipeline.resolve(Collections.singletonList(dto), PipelineNoAnnotationDTO.class, new ResolveContext());
        assertEquals("张三", dto.getName());
        assertEquals(25, dto.getAge());
        assertTrue(page.isEmpty());
    }

    /**
     * 测试字段值为 null 时跳过解析。
     */
    @Test
    public void testResolverPipeline_NullValue() {
        List<ExcelFieldResolver<?>> resolvers = Collections.singletonList(new EnumFieldResolver());
        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);

        PipelineStringEnumDTO dto = new PipelineStringEnumDTO();
        dto.setStatus(null);

        ResolvedPage page = pipeline.resolve(Collections.singletonList(dto), PipelineStringEnumDTO.class, new ResolveContext());
        assertNull(dto.getStatus());
        assertTrue(page.isEmpty());
    }

    /**
     * 测试字典 Provider 在单页内只批量查询一次。
     */
    @Test
    public void testResolverPipeline_DictProviderBatchQueryOnce() {
        AtomicInteger queryCount = new AtomicInteger(0);
        DictionaryProvider provider = (dictCode, codes) -> {
            queryCount.incrementAndGet();
            Map<String, String> map = new HashMap<>();
            map.put("1", "研发");
            map.put("2", "产品");
            return map;
        };

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(
                Collections.singletonList(new DictFieldResolver(provider)));

        PipelineDictProviderDTO first = new PipelineDictProviderDTO();
        first.setDepartment("1");
        PipelineDictProviderDTO second = new PipelineDictProviderDTO();
        second.setDepartment("2");

        ResolvedPage page = pipeline.resolve(asList(first, second), PipelineDictProviderDTO.class, new ResolveContext());

        assertEquals(1, queryCount.get());
        assertEquals("研发", page.getValue(0, "department"));
        assertEquals("产品", page.getValue(1, "department"));
    }

    /**
     * 测试字典 Provider 的多值翻译场景。
     */
    @Test
    public void testResolverPipeline_DictProviderMultiValue() {
        DictionaryProvider provider = (dictCode, codes) -> {
            Map<String, String> map = new HashMap<>();
            map.put("1", "Java");
            map.put("2", "Go");
            return map;
        };

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(
                Collections.singletonList(new DictFieldResolver(provider)));

        PipelineTagsDTO dto = new PipelineTagsDTO();
        dto.setTags("1,2,3");

        ResolvedPage page = pipeline.resolve(Collections.singletonList(dto), PipelineTagsDTO.class, new ResolveContext());
        assertEquals("Java,Go,3", page.getValue(0, "tags"));
    }

    /**
     * 测试同一字典编码的多个字段不会互相覆盖。
     */
    @Test
    public void testResolverPipeline_SameDictCodeMultipleFields() {
        DictionaryProvider provider = (dictCode, codes) -> {
            Map<String, String> map = new HashMap<>();
            map.put("1", "研发");
            map.put("2", "产品");
            return map;
        };

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(
                Collections.singletonList(new DictFieldResolver(provider)));

        PipelineSameDictCodeDTO dto = new PipelineSameDictCodeDTO();
        dto.setPrimaryDepartment("1");
        dto.setSecondaryDepartment("2");

        ResolvedPage page = pipeline.resolve(Collections.singletonList(dto), PipelineSameDictCodeDTO.class, new ResolveContext());
        assertEquals("研发", page.getValue(0, "primaryDepartment"));
        assertEquals("产品", page.getValue(0, "secondaryDepartment"));
    }

    /**
     * 分页数据入口，供导出请求调用。
     */
    private ExportPageData<UserTest> pageData(int pageNum, int pageSize, Map<String, Object> params) {
        return pageDataFromSource(TEST_DATA, pageNum, pageSize);
    }

    /**
     * 从指定数据源按页切片，构造导出分页结果。
     */
    private ExportPageData<UserTest> pageDataFromSource(List<UserTest> source, int pageNum, int pageSize) {
        if (pageNum <= 0 || pageSize <= 0) {
            return new ExportPageData<>(Collections.emptyList(), false);
        }
        int fromIndex = (pageNum - 1) * pageSize;
        if (fromIndex >= source.size()) {
            return new ExportPageData<>(Collections.emptyList(), false);
        }
        int toIndex = Math.min(fromIndex + pageSize, source.size());
        List<UserTest> pageData = new ArrayList<>(source.subList(fromIndex, toIndex));
        boolean hasNext = toIndex < source.size();
        return new ExportPageData<>(pageData, hasNext);
    }

    /**
     * 构造多 Sheet 场景的第一个 Sheet 数据。
     */
    private List<UserTest> sheet1Data() {
        return asList(
                new UserTest("用户1", "13800000001", "u1@silky.com", 1, 1, "110101199001011111", "6222021234567890001"),
                new UserTest("用户2", "13800000002", "u2@silky.com", 0, 2, "110101199001011112", "6222021234567890002")
        );
    }

    /**
     * 构造多 Sheet 场景的第二个 Sheet 数据。
     */
    private List<UserTest> sheet2Data() {
        return asList(
                new UserTest("用户3", "13900000003", "u3@silky.com", 1, 1, "110101199001011113", "6222021234567890003"),
                new UserTest("用户4", "13900000004", "u4@silky.com", 0, 2, "110101199001011114", "6222021234567890004")
        );
    }

    /**
     * 先生成一个临时 Excel 文件，供导入测试复用。
     */
    private String exportSampleImportFile(String suffix) {
        String fileName = System.getProperty("java.io.tmpdir")
                + "/silky-import-source-" + suffix + "-" + System.currentTimeMillis() + ".xlsx";
        List<UserTest> users = asList(
                new UserTest("导入A", "13811110001", "a@silky.com", UserStatus.ACTIVE.getCode(), 1,
                        "320106199001010001", "6222021234567800011"),
                new UserTest("导入B", "13811110002", "b@silky.com", UserStatus.DISABLED.getCode(), 2,
                        "320106199001010002", "6222021234567800012"),
                new UserTest("导入C", "13811110003", "c@silky.com", UserStatus.LOCKED.getCode(), 1,
                        "320106199001010003", "6222021234567800013")
        );

        EnhancedWriterWrapper writer = new EnhancedWriterWrapper(fileName, 1000);
        try {
            writer.write(users, UserTest.class, "导入数据");
        } finally {
            writer.close();
        }
        return fileName;
    }

    /**
     * 轮询导出记录，直到任务结束或超时。
     */
    private ExportRecord waitExportRecord(String taskId, long timeoutMillis) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            ExportRecord record = excelTemplate.getExportRecord(taskId);
            if (record != null && record.getStatus() != null
                    && (record.getStatus() == ExportStatus.COMPLETED || record.getStatus() == ExportStatus.FAILED)) {
                return record;
            }
            Thread.sleep(100);
        }
        return excelTemplate.getExportRecord(taskId);
    }

    /**
     * 轮询导入记录，直到任务结束或超时。
     */
    private ImportRecord waitImportRecord(String taskId, long timeoutMillis) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            ImportRecord record = excelTemplate.getImportRecord(taskId);
            if (record != null && record.getStatus() != null
                    && (record.getStatus() == ImportStatus.COMPLETED || record.getStatus() == ImportStatus.FAILED)) {
                return record;
            }
            Thread.sleep(100);
        }
        return excelTemplate.getImportRecord(taskId);
    }

    /**
     * 构造基础测试数据。
     */
    private static List<UserTest> createTestData() {
        List<UserTest> list = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            String name = "用户" + i;
            String phone = "1380013" + String.format("%04d", i);
            String email = "user" + i + "@silky.com";
            int status = i % 2 == 0 ? UserStatus.ACTIVE.getCode() : UserStatus.DISABLED.getCode();
            int gender = i % 2 == 0 ? 1 : 2;
            String idCard = "320106" + String.format("%012d", i) + "01";
            String bankCard = "622202" + String.format("%013d", i);
            list.add(new UserTest(name, phone, email, status, gender, idCard, bankCard));
        }
        return list;
    }

    /**
     * 便捷构建列表。
     */
    @SafeVarargs
    private static <T> List<T> asList(T... rows) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, rows);
        return list;
    }

    /**
     * 组合注解测试 DTO：同一字段包含枚举翻译和脱敏。
     */
    static class PipelineCombinedDTO {
        @ExcelEnum(enumClass = UserStatus.class, codeField = "code", labelField = "label")
        @ExcelMask(strategy = MaskStrategy.PHONE)
        private Integer statusCode;

        public Integer getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }
    }

    /**
     * 字符串枚举测试 DTO。
     */
    static class PipelineStringEnumDTO {
        @ExcelEnum(enumClass = UserStatus.class, codeField = "code", labelField = "label")
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 字典表达式测试 DTO。
     */
    static class PipelineDictDTO {
        @ExcelDict(readConverterExp = "0=普通,1=VIP,2=SVIP")
        private String userLevel;

        public String getUserLevel() {
            return userLevel;
        }

        public void setUserLevel(String userLevel) {
            this.userLevel = userLevel;
        }
    }

    /**
     * 无注解测试 DTO。
     */
    static class PipelineNoAnnotationDTO {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    /**
     * 字典 Provider 测试 DTO。
     */
    static class PipelineDictProviderDTO {
        @ExcelDict(dictCode = "department")
        private String department;

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }
    }

    /**
     * 多值字典测试 DTO。
     */
    static class PipelineTagsDTO {
        @ExcelDict(dictCode = "tags", separator = ",")
        private String tags;

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }
    }

    /**
     * 相同 dictCode 多字段测试 DTO。
     */
    static class PipelineSameDictCodeDTO {
        @ExcelDict(dictCode = "department")
        private String primaryDepartment;

        @ExcelDict(dictCode = "department")
        private String secondaryDepartment;

        public String getPrimaryDepartment() {
            return primaryDepartment;
        }

        public void setPrimaryDepartment(String primaryDepartment) {
            this.primaryDepartment = primaryDepartment;
        }

        public String getSecondaryDepartment() {
            return secondaryDepartment;
        }

        public void setSecondaryDepartment(String secondaryDepartment) {
            this.secondaryDepartment = secondaryDepartment;
        }
    }
}
