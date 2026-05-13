package com.silky.starter.excel.core.resolve;

import com.silky.starter.excel.core.annotation.ExcelDict;
import com.silky.starter.excel.core.annotation.ExcelEnum;
import com.silky.starter.excel.core.annotation.ExcelMask;
import com.silky.starter.excel.core.annotation.ExcelMask.MaskStrategy;
import com.silky.starter.excel.core.resolve.ExcelFieldResolver;
import com.silky.starter.excel.core.resolve.ExcelFieldResolverPipeline;
import com.silky.starter.excel.core.resolve.ResolveContext;
import com.silky.starter.excel.core.resolve.DictionaryProvider;
import com.silky.starter.excel.core.resolve.EnumFieldResolver;
import com.silky.starter.excel.core.resolve.MaskFieldResolver;
import com.silky.starter.excel.core.resolve.DictFieldResolver;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExcelFieldResolverPipeline 组合注解测试
 *
 * @author zy
 * @since 1.1.0
 */
class ExcelFieldResolverPipelineTest {

    /**
     * 测试组合注解：@ExcelEnum + @ExcelMask 同时存在
     * 每个注解独立进入解析器管道，从原始值开始处理
     * - ExcelEnum 分支：Integer 1 -> String "启用"（类型不兼容存入旁路）
     * - ExcelMask 分支：原始值 1 -> 脱敏处理
     * 因为 ExcelMask 对 Integer 值 1 的脱敏结果不是有效手机号，保持原值
     */
    @Test
    void testCombinedAnnotation_EnumAndMask() {
        List<ExcelFieldResolver> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());
        resolvers.add(new MaskFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TestCombinedDTO dto = new TestCombinedDTO();
        dto.setStatusCode(1);  // Integer 字段

        List<TestCombinedDTO> data = Collections.singletonList(dto);
        pipeline.resolve(data, TestCombinedDTO.class, context);

        // ExcelEnum 分支：Integer 字段 -> String "启用"，类型不兼容 -> 存入旁路存储
        assertEquals(1, dto.getStatusCode());  // 原值不变
        Object resolved = pipeline.getResolvedValue(dto, "statusCode");
        assertEquals("启用", resolved);
    }

    /**
     * 测试 String 字段 + @ExcelEnum：类型兼容直接 set
     */
    @Test
    void testEnumAnnotation_StringField() {
        List<ExcelFieldResolver> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TestStringEnumDTO dto = new TestStringEnumDTO();
        dto.setStatus("1");  // String 字段

        List<TestStringEnumDTO> data = Collections.singletonList(dto);
        pipeline.resolve(data, TestStringEnumDTO.class, context);

        // String 字段 + 枚举翻译 "1" -> "启用"，类型兼容直接 set
        assertEquals("启用", dto.getStatus());
    }

    /**
     * 测试 @ExcelDict 表达式模式
     */
    @Test
    void testDictExpressionMode() {
        List<ExcelFieldResolver> resolvers = new ArrayList<>();
        resolvers.add(new DictFieldResolver(null));

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TestDictDTO dto = new TestDictDTO();
        dto.setUserLevel("1");

        List<TestDictDTO> data = Collections.singletonList(dto);
        pipeline.resolve(data, TestDictDTO.class, context);

        assertEquals("VIP", dto.getUserLevel());
    }

    /**
     * 测试无注解字段不受影响
     */
    @Test
    void testNoAnnotation_NoChange() {
        List<ExcelFieldResolver> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());
        resolvers.add(new MaskFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TestNoAnnotationDTO dto = new TestNoAnnotationDTO();
        dto.setName("张三");
        dto.setAge(25);

        List<TestNoAnnotationDTO> data = Collections.singletonList(dto);
        pipeline.resolve(data, TestNoAnnotationDTO.class, context);

        assertEquals("张三", dto.getName());
        assertEquals(25, dto.getAge());
    }

    /**
     * 测试旁路存储机制（类型不兼容字段）
     */
    @Test
    void testSidestore_TypeIncompatible() {
        List<ExcelFieldResolver> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TypeIncompatibleDTO dto = new TypeIncompatibleDTO();
        dto.setStatusCode(1); // Integer 字段，翻译后变成 String "启用"

        List<TypeIncompatibleDTO> data = Collections.singletonList(dto);
        pipeline.resolve(data, TypeIncompatibleDTO.class, context);

        // 原字段值不变（类型不兼容无法 set）
        assertEquals(1, dto.getStatusCode());

        // 解析值在旁路存储中
        Object resolved = pipeline.getResolvedValue(dto, "statusCode");
        assertEquals("启用", resolved);

        // 清理旁路存储
        pipeline.clearResolvedValues(data);
        assertNull(pipeline.getResolvedValue(dto, "statusCode"));
    }

    /**
     * 测试 null 值不处理
     */
    @Test
    void testNullValue_NoChange() {
        List<ExcelFieldResolver> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TestStringEnumDTO dto = new TestStringEnumDTO();
        dto.setStatus(null);

        List<TestStringEnumDTO> data = Collections.singletonList(dto);
        pipeline.resolve(data, TestStringEnumDTO.class, context);

        assertNull(dto.getStatus());
    }

    // ========== 测试用 DTO ==========

    /** 组合注解 DTO（Integer 字段） */
    static class TestCombinedDTO {
        @ExcelEnum(enumClass = StatusEnum.class, codeField = "code", labelField = "label")
        @ExcelMask(strategy = MaskStrategy.PHONE)
        private Integer statusCode;

        public Integer getStatusCode() { return statusCode; }
        public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    }

    /** String 字段枚举 DTO */
    static class TestStringEnumDTO {
        @ExcelEnum(enumClass = StatusEnum.class, codeField = "code", labelField = "label")
        private String status;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /** 字典表达式 DTO */
    static class TestDictDTO {
        @ExcelDict(readConverterExp = "0=普通,1=VIP,2=SVIP")
        private String userLevel;

        public String getUserLevel() { return userLevel; }
        public void setUserLevel(String userLevel) { this.userLevel = userLevel; }
    }

    /** 无注解 DTO */
    static class TestNoAnnotationDTO {
        private String name;
        private int age;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    /** 类型不兼容 DTO */
    static class TypeIncompatibleDTO {
        @ExcelEnum(enumClass = StatusEnum.class, codeField = "code", labelField = "label")
        private Integer statusCode;

        public Integer getStatusCode() { return statusCode; }
        public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    }

    /** 状态枚举 */
    enum StatusEnum {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");

        private final int code;
        private final String label;

        StatusEnum(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() { return code; }
        public String getLabel() { return label; }
    }
}
