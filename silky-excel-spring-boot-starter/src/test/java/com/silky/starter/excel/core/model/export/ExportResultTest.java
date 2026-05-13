package com.silky.starter.excel.core.model.export;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExportResult 单元测试
 *
 * @author zy
 * @since 1.1.0
 */
class ExportResultTest {

    @Test
    void testAddError_ShouldNotThrowOnEmptyList() {
        // 使用 success() 创建的 ExportResult，errors 为 Collections.emptyList()
        ExportResult result = ExportResult.success("test-task");
        assertNotNull(result.getErrors());

        // 修复前：这里会抛出 UnsupportedOperationException
        // 修复后：正常添加
        assertDoesNotThrow(() -> result.addError(1, "fieldName", "测试错误"));
        assertEquals(1, result.getErrorCount());
        assertTrue(result.hasErrors());
    }

    @Test
    void testAddError_MultipleErrors() {
        ExportResult result = ExportResult.success("test-task");
        result.addError(1, "name", "名称不能为空");
        result.addError(2, "age", "年龄格式错误");
        result.addError(ExportResult.ExportError.of(3, "email", "邮箱格式错误"));

        assertEquals(3, result.getErrorCount());
        assertEquals("名称不能为空", result.getErrors().get(0).getErrorMessage());
    }

    @Test
    void testAddError_WithNullErrors() {
        ExportResult result = new ExportResult();
        assertNull(result.getErrors());

        assertDoesNotThrow(() -> result.addError(1, "field", "error"));
        assertEquals(1, result.getErrorCount());
    }

    @Test
    void testSuccessResult() {
        ExportResult result = ExportResult.success("task-1", 100L, 98L);
        assertTrue(result.isSuccess());
        assertEquals(100L, result.getTotalCount());
        assertEquals(98L, result.getSuccessCount());
        assertFalse(result.hasErrors());
    }

    @Test
    void testFailResult() {
        ExportResult result = ExportResult.fail("task-1", "导出失败");
        assertFalse(result.isSuccess());
        assertEquals("导出失败", result.getMessage());
    }

    @Test
    void testSuccessRate() {
        ExportResult result = ExportResult.success("task-1", 100L, 80L);
        assertEquals(0.8, result.getSuccessRate());
    }

    @Test
    void testPartialSuccess() {
        ExportResult result = ExportResult.partialSuccess("task-1", 100L, 90L, 10L);
        assertFalse(result.isSuccess());
        assertEquals(100L, result.getTotalCount());
        assertEquals(90L, result.getSuccessCount());
        assertEquals(10L, result.getFailedCount());
    }
}
