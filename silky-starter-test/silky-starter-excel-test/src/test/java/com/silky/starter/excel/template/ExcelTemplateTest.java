package com.silky.starter.excel.template;

import cn.hutool.core.collection.ListUtil;
import com.silky.starter.excel.ExcelApplicationTest;
import com.silky.starter.excel.core.annotation.ExcelEnum;
import com.silky.starter.excel.core.annotation.ExcelMask;
import com.silky.starter.excel.core.model.export.ExportPageData;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.core.model.export.ExportSheet;
import com.silky.starter.excel.core.model.imports.DataImporterSupplier;
import com.silky.starter.excel.core.model.imports.ImportRequest;
import com.silky.starter.excel.core.model.imports.ImportResult;
import com.silky.starter.excel.core.resolve.EnumFieldResolver;
import com.silky.starter.excel.core.resolve.MaskFieldResolver;
import com.silky.starter.excel.core.resolve.ResolveContext;
import com.silky.starter.excel.core.storage.StorageObject;
import com.silky.starter.excel.enums.AsyncType;
import com.silky.starter.excel.enums.StorageType;
import com.silky.starter.excel.template.entity.UserTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExcelTemplate 示例测试类。
 * 说明：该模块用于示例演示，因此测试更偏向“接口用法示例 + 基础行为校验”。
 *
 * @author zy
 */
public class ExcelTemplateTest extends ExcelApplicationTest {

    private static final List<UserTest> TEST_DATA = createTestData();

    @Autowired
    private ExcelTemplate excelTemplate;

    /**
     * 验证同步导出基础流程。
     */
    @Test
    public void testExportSync() {
        ExportResult result = excelTemplate.export(ExportRequest.<UserTest>builder()
                .dataClass(UserTest.class)
                .fileName("test-export.xlsx")
                .dataSupplier((pageNum, pageSize, params) -> new ExportPageData<>(TEST_DATA, false))
                .build(), AsyncType.SYNC);

        log.info("同步导出结果: {}", result.getSummary());
        assertTrue(result.isSuccess());
        assertNotNull(result.getFileUrl());
        assertTrue(result.getTotalCount() > 0);
    }

    /**
     * 验证分页同步导出流程。
     */
    @Test
    public void testPageExportSync() {
        ExportRequest<UserTest> request = new ExportRequest<>();
        request.setDataClass(UserTest.class);
        request.setFileName("test-page.xls");
        request.setPageSize(10);
        request.setDataSupplier((pageNum, pageSize, params) -> {
            List<UserTest> users = findByCondition(pageNum, pageSize);
            return new ExportPageData<>(users, pageNum * pageSize < TEST_DATA.size());
        });

        ExportResult result = excelTemplate.exportSync(request);
        log.info("分页导出结果: {}", result.getSummary());
        assertTrue(result.isSuccess());
        assertNotNull(result.getTaskId());
    }

    /**
     * 验证异步导出任务提交流程。
     */
    @Test
    public void testExportAsync() {
        ExportRequest<UserTest> request = new ExportRequest<>();
        request.setDataClass(UserTest.class);
        request.setFileName("test-async.xls");
        request.setPageSize(10);
        request.setMaxRowsPerSheet(10000L);
        request.setDataSupplier((pageNum, pageSize, params) -> {
            List<UserTest> users = findByCondition(pageNum, pageSize);
            return new ExportPageData<>(users, pageNum * pageSize < TEST_DATA.size());
        });

        ExportResult result = excelTemplate.exportAsync(request);
        log.info("异步导出结果: {}", result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getTaskId());
    }

    /**
     * 验证同步导入接口可正常返回结果（示例模式下不强依赖固定文件）。
     */
    @Test
    public void testImportSync() {
        ImportRequest<UserTest> request = buildImportRequest();
        ImportResult result = excelTemplate.importSync(request);

        log.info("同步导入结果: {}", result.getSummary());
        assertNotNull(result);
        assertNotNull(result.getTaskId());
    }

    /**
     * 验证异步导入任务提交流程。
     */
    @Test
    public void testImportAsync() {
        ImportRequest<UserTest> request = buildImportRequest();
        ImportResult result = excelTemplate.importAsync(request);

        log.info("异步导入结果: {}", result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getTaskId());
    }

    /**
     * 验证字段脱敏解析器，覆盖手机号、姓名、邮箱。
     * 修复点：resolve 参数必须是 (field, annotation, value, context)。
     */
    @Test
    public void testMaskFieldResolver() {
        MaskFieldResolver resolver = new MaskFieldResolver();
        ResolveContext context = new ResolveContext();

        Field phoneField = getField("phone");
        ExcelMask phoneMask = phoneField.getAnnotation(ExcelMask.class);
        Object phoneResult = resolver.resolve(phoneField, phoneMask, "13812345678", context);
        assertEquals("138****5678", phoneResult);

        Field nameField = getField("name");
        ExcelMask nameMask = nameField.getAnnotation(ExcelMask.class);
        Object nameResult = resolver.resolve(nameField, nameMask, "张三丰", context);
        assertEquals("张**", nameResult);

        Field emailField = getField("email");
        ExcelMask emailMask = emailField.getAnnotation(ExcelMask.class);
        Object emailResult = resolver.resolve(emailField, emailMask, "zhangsan@example.com", context);
        assertTrue(String.valueOf(emailResult).endsWith("@example.com"));
        assertNotEquals("zhangsan@example.com", emailResult);
    }

    /**
     * 验证枚举翻译解析器。
     */
    @Test
    public void testEnumFieldResolver() {
        EnumFieldResolver resolver = new EnumFieldResolver();
        Field statusField = getField("status");
        ExcelEnum annotation = statusField.getAnnotation(ExcelEnum.class);

        Object result = resolver.resolve(statusField, annotation, 1, new ResolveContext());
        assertEquals("启用", result);
    }

    /**
     * 验证注解驱动导出（枚举翻译 + 脱敏）流程。
     */
    @Test
    public void testExportWithAnnotations() {
        List<UserTest> data = new ArrayList<>();

        UserTest user1 = new UserTest("张三丰", "13812345678");
        user1.setEmail("zhangsan@example.com");
        user1.setStatus(1);
        user1.setIdCard("110101199001011234");
        user1.setBankCard("6222021234567890123");
        data.add(user1);

        UserTest user2 = new UserTest("李四", "13987654321");
        user2.setEmail("lisi@example.com");
        user2.setStatus(0);
        user2.setIdCard("320102199501011234");
        user2.setBankCard("6222021234567890456");
        data.add(user2);

        ExportResult result = excelTemplate.export(ExportRequest.<UserTest>builder()
                .dataClass(UserTest.class)
                .fileName("test-annotation-export.xlsx")
                .dataSupplier((pageNum, pageSize, params) -> new ExportPageData<>(data, false))
                .build(), AsyncType.SYNC);

        log.info("注解导出结果: {}", result.getSummary());
        assertTrue(result.isSuccess());
        assertEquals(2L, result.getTotalCount());
    }

    /**
     * 验证 StorageObject 构建与字段赋值。
     */
    @Test
    public void testStorageObject() {
        StorageObject obj = StorageObject.of("test_key.xlsx", 1024);
        assertEquals("test_key.xlsx", obj.getKey());
        assertEquals(1024, obj.getSize());

        StorageObject obj2 = StorageObject.builder()
                .key("key2.xlsx")
                .url("/tmp/key2.xlsx")
                .size(2048)
                .etag("abc123")
                .build();

        assertEquals("key2.xlsx", obj2.getKey());
        assertEquals("/tmp/key2.xlsx", obj2.getUrl());
        assertEquals(2048, obj2.getSize());
        assertEquals("abc123", obj2.getEtag());
    }

    /**
     * 验证多 Sheet 导出。
     * 修复点：当前模板层校验要求 dataClass/dataSupplier 非空，因此补充占位字段。
     */
    @Test
    public void testMultiSheetExport() {
        List<UserTest> sheet1Data = new ArrayList<>();
        sheet1Data.add(new UserTest("用户1", "13800000001"));

        List<UserTest> sheet2Data = new ArrayList<>();
        sheet2Data.add(new UserTest("用户2", "13900000002"));

        ExportRequest<UserTest> request = ExportRequest.<UserTest>builder()
                .dataClass(UserTest.class)
                .dataSupplier((pageNum, pageSize, params) -> new ExportPageData<>(Collections.emptyList(), false))
                .fileName("test-multi-sheet.xlsx")
                .sheets(ListUtil.of(
                        ExportSheet.of("用户基本信息", UserTest.class,
                                (pageNum, pageSize, params) -> new ExportPageData<>(sheet1Data, false)),
                        ExportSheet.of("用户详细信息", UserTest.class,
                                (pageNum, pageSize, params) -> new ExportPageData<>(sheet2Data, false))
                ))
                .build();

        ExportResult result = excelTemplate.export(request, AsyncType.SYNC);
        log.info("多Sheet导出结果: {}", result.getSummary());
        assertTrue(result.isSuccess());
        assertEquals(2, result.getSheetCount().intValue());
    }

    /**
     * 验证 maxRowsPerSheet 参数边界。
     */
    @Test
    public void testMaxRowsPerSheetValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                excelTemplate.export(ExportRequest.<UserTest>builder()
                        .dataClass(UserTest.class)
                        .fileName("test-invalid.xlsx")
                        .maxRowsPerSheet(0L)
                        .dataSupplier((pageNum, pageSize, params) -> new ExportPageData<>(TEST_DATA, false))
                        .build(), AsyncType.SYNC)
        );

        assertThrows(IllegalArgumentException.class, () ->
                excelTemplate.export(ExportRequest.<UserTest>builder()
                        .dataClass(UserTest.class)
                        .fileName("test-invalid2.xlsx")
                        .maxRowsPerSheet(2_000_000L)
                        .dataSupplier((pageNum, pageSize, params) -> new ExportPageData<>(TEST_DATA, false))
                        .build(), AsyncType.SYNC)
        );
    }

    /**
     * 验证自动分 Sheet 逻辑。
     */
    @Test
    public void testAutoSheetSplit() {
        ExportResult result = excelTemplate.export(ExportRequest.<UserTest>builder()
                .dataClass(UserTest.class)
                .fileName("test-auto-split.xlsx")
                .maxRowsPerSheet(5L)
                .pageSize(10)
                .dataSupplier((pageNum, pageSize, params) -> {
                    List<UserTest> page = findByCondition(pageNum, 5);
                    return new ExportPageData<>(page, pageNum * 5 < TEST_DATA.size());
                })
                .build(), AsyncType.SYNC);

        log.info("自动分Sheet导出结果: {}", result.getSummary());
        assertTrue(result.isSuccess());
        assertTrue(result.getSheetCount() > 1);
    }

    /**
     * 验证带任务配置器的导入调用。
     */
    @Test
    public void testImportsWithTaskConfig() {
        ImportRequest<UserTest> request = buildImportRequest();

        ImportResult result = excelTemplate.imports(request, AsyncType.SYNC, task ->
                log.info("自定义导入任务配置: taskId={}", task.getTaskId())
        );

        log.info("通用导入结果: {}", result.getSummary());
        assertNotNull(result);
        assertNotNull(result.getTaskId());
    }

    /**
     * 验证导出引擎状态查询接口。
     */
    @Test
    public void testExportEngineStatus() {
        assertNotNull(excelTemplate.getExportEngineStatus());
    }

    /**
     * 验证导入引擎状态查询接口。
     */
    @Test
    public void testImportEngineStatus() {
        assertNotNull(excelTemplate.getImportEngineStatus());
    }

    /**
     * 按页码与页大小从内存测试数据中切片。
     */
    private List<UserTest> findByCondition(int pageNum, int pageSize) {
        int adjustedPageNum = pageNum - 1;
        if (adjustedPageNum < 0) {
            return Collections.emptyList();
        }

        int fromIndex = adjustedPageNum * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, TEST_DATA.size());
        if (fromIndex >= TEST_DATA.size()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(TEST_DATA.subList(fromIndex, toIndex));
    }

    /**
     * 构建导入请求。
     */
    private ImportRequest<UserTest> buildImportRequest() {
        ImportRequest<UserTest> request = new ImportRequest<>();
        request.setDataClass(UserTest.class);
        request.setFileName("test.xls");
        request.setFileUrl(System.getProperty("java.io.tmpdir") + "/silky-excel/test-import.xlsx");
        request.setDataImporterSupplier((data, params) -> {
            log.info("导入数据长度: {}", data.size());
            return new DataImporterSupplier.ImportBatchResult(data.size(), Collections.emptyList());
        });
        request.setStorageType(StorageType.LOCAL);
        request.setPageSize(20000);
        return request;
    }

    /**
     * 通过字段名获取 UserTest 字段，避免依赖声明顺序。
     */
    private Field getField(String fieldName) {
        try {
            return UserTest.class.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("字段不存在: " + fieldName, e);
        }
    }

    /**
     * 创建测试用用户数据。
     */
    private static List<UserTest> createTestData() {
        int size = 500;
        List<UserTest> list = new ArrayList<>(size);
        for (int i = 1; i <= size; i++) {
            list.add(new UserTest("用户" + i, "1380013" + String.format("%04d", i)));
        }
        return list;
    }
}
