package com.silky.starter.excel.core.resolve;

import com.silky.starter.excel.core.annotation.ExcelDict;
import com.silky.starter.excel.core.annotation.ExcelEnum;
import com.silky.starter.excel.core.annotation.ExcelMask;
import com.silky.starter.excel.core.annotation.ExcelMask.MaskStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ExcelFieldResolverPipeline 组合注解测试
 *
 * @author zy
 * @since 1.1.0
 */
class ExcelFieldResolverPipelineTest {

    /**
     * 测试组合注解：@ExcelEnum + @ExcelMask 同时存在
     * 同一字段的多个注解按顺序串行执行。
     * - ExcelEnum：Integer 1 -> String "启用"
     * - ExcelMask：对 "启用" 无脱敏效果
     */
    @Test
    void testCombinedAnnotation_EnumAndMask() {
        List<ExcelFieldResolver<?>> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());
        resolvers.add(new MaskFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TestCombinedDTO dto = new TestCombinedDTO();
        dto.setStatusCode(1);  // Integer 字段

        List<TestCombinedDTO> data = Collections.singletonList(dto);
        ResolvedPage resolvedPage = pipeline.resolve(data, TestCombinedDTO.class, context);

        assertEquals(1, dto.getStatusCode());  // 原值不变
        assertEquals("启用", resolvedPage.getValue(0, "statusCode"));
    }

    /**
     * 测试 String 字段 + @ExcelEnum：只生成导出展示值，不修改 DTO。
     */
    @Test
    void testEnumAnnotation_StringField() {
        List<ExcelFieldResolver<?>> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TestStringEnumDTO dto = new TestStringEnumDTO();
        dto.setStatus("1");  // String 字段

        List<TestStringEnumDTO> data = Collections.singletonList(dto);
        ResolvedPage resolvedPage = pipeline.resolve(data, TestStringEnumDTO.class, context);

        assertEquals("1", dto.getStatus());
        assertEquals("启用", resolvedPage.getValue(0, "status"));
    }

    /**
     * 测试 @ExcelDict 表达式模式
     */
    @Test
    void testDictExpressionMode() {
        List<ExcelFieldResolver<?>> resolvers = new ArrayList<>();
        resolvers.add(new DictFieldResolver(null));

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TestDictDTO dto = new TestDictDTO();
        dto.setUserLevel("1");

        List<TestDictDTO> data = Collections.singletonList(dto);
        ResolvedPage resolvedPage = pipeline.resolve(data, TestDictDTO.class, context);

        assertEquals("1", dto.getUserLevel());
        assertEquals("VIP", resolvedPage.getValue(0, "userLevel"));
    }

    /**
     * 测试无注解字段不受影响
     */
    @Test
    void testNoAnnotation_NoChange() {
        List<ExcelFieldResolver<?>> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());
        resolvers.add(new MaskFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TestNoAnnotationDTO dto = new TestNoAnnotationDTO();
        dto.setName("张三");
        dto.setAge(25);

        List<TestNoAnnotationDTO> data = Collections.singletonList(dto);
        ResolvedPage resolvedPage = pipeline.resolve(data, TestNoAnnotationDTO.class, context);

        assertEquals("张三", dto.getName());
        assertEquals(25, dto.getAge());
        assertEquals(true, resolvedPage.isEmpty());
    }

    /**
     * 测试类型不兼容字段生成页级解析值。
     */
    @Test
    void testResolvedPage_TypeIncompatible() {
        List<ExcelFieldResolver<?>> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TypeIncompatibleDTO dto = new TypeIncompatibleDTO();
        dto.setStatusCode(1); // Integer 字段，翻译后变成 String "启用"

        List<TypeIncompatibleDTO> data = Collections.singletonList(dto);
        ResolvedPage resolvedPage = pipeline.resolve(data, TypeIncompatibleDTO.class, context);

        assertEquals(1, dto.getStatusCode());
        assertEquals("启用", resolvedPage.getValue(0, "statusCode"));
    }

    /**
     * 测试 null 值不处理
     */
    @Test
    void testNullValue_NoChange() {
        List<ExcelFieldResolver<?>> resolvers = new ArrayList<>();
        resolvers.add(new EnumFieldResolver());

        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(resolvers);
        ResolveContext context = new ResolveContext();

        TestStringEnumDTO dto = new TestStringEnumDTO();
        dto.setStatus(null);

        List<TestStringEnumDTO> data = Collections.singletonList(dto);
        ResolvedPage resolvedPage = pipeline.resolve(data, TestStringEnumDTO.class, context);

        assertNull(dto.getStatus());
        assertEquals(true, resolvedPage.isEmpty());
    }

    /**
     * 测试字典 Provider 模式按页批量查询一次。
     */
    @Test
    void testDictProvider_BatchQueryOncePerPage() {
        AtomicInteger queryCount = new AtomicInteger();
        DictionaryProvider provider = (dictCode, codes) -> {
            queryCount.incrementAndGet();
            Map<String, String> result = new HashMap<>();
            result.put("1", "研发");
            result.put("2", "产品");
            return result;
        };
        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(
                Collections.singletonList(new DictFieldResolver(provider)));
        ResolveContext context = new ResolveContext();

        TestDictProviderDTO first = new TestDictProviderDTO();
        first.setDepartment("1");
        TestDictProviderDTO second = new TestDictProviderDTO();
        second.setDepartment("2");

        ResolvedPage resolvedPage = pipeline.resolve(asList(first, second), TestDictProviderDTO.class, context);

        assertEquals(1, queryCount.get());
        assertEquals("1", first.getDepartment());
        assertEquals("2", second.getDepartment());
        assertEquals("研发", resolvedPage.getValue(0, "department"));
        assertEquals("产品", resolvedPage.getValue(1, "department"));
    }

    /**
     * 测试字典 Provider 多值翻译。
     */
    @Test
    void testDictProvider_MultiValue() {
        DictionaryProvider provider = (dictCode, codes) -> {
            Map<String, String> result = new HashMap<>();
            result.put("1", "Java");
            result.put("2", "Go");
            return result;
        };
        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(
                Collections.singletonList(new DictFieldResolver(provider)));
        ResolveContext context = new ResolveContext();

        TestDictTagsDTO dto = new TestDictTagsDTO();
        dto.setTags("1,2,3");

        ResolvedPage resolvedPage = pipeline.resolve(Collections.singletonList(dto), TestDictTagsDTO.class, context);

        assertEquals("1,2,3", dto.getTags());
        assertEquals("Java,Go,3", resolvedPage.getValue(0, "tags"));
    }

    /**
     * 测试同一 dictCode 的多个字段不会互相覆盖缓存。
     */
    @Test
    void testDictProvider_SameDictCodeMultipleFields() {
        DictionaryProvider provider = (dictCode, codes) -> {
            Map<String, String> result = new HashMap<>();
            result.put("1", "研发");
            result.put("2", "产品");
            return result;
        };
        ExcelFieldResolverPipeline pipeline = new ExcelFieldResolverPipeline(
                Collections.singletonList(new DictFieldResolver(provider)));
        ResolveContext context = new ResolveContext();

        TestSameDictCodeDTO dto = new TestSameDictCodeDTO();
        dto.setPrimaryDepartment("1");
        dto.setSecondaryDepartment("2");

        ResolvedPage resolvedPage = pipeline.resolve(Collections.singletonList(dto), TestSameDictCodeDTO.class, context);

        assertEquals("研发", resolvedPage.getValue(0, "primaryDepartment"));
        assertEquals("产品", resolvedPage.getValue(0, "secondaryDepartment"));
    }

    // ========== 测试用 DTO ==========

    /**
     * 组合注解 DTO（Integer 字段）
     */
    static class TestCombinedDTO {
        @ExcelEnum(enumClass = StatusEnum.class, codeField = "code", labelField = "label")
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
     * String 字段枚举 DTO
     */
    static class TestStringEnumDTO {
        @ExcelEnum(enumClass = StatusEnum.class, codeField = "code", labelField = "label")
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 字典表达式 DTO
     */
    static class TestDictDTO {
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
     * 字典 Provider DTO
     */
    static class TestDictProviderDTO {
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
     * 多值字典 DTO
     */
    static class TestDictTagsDTO {
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
     * 相同 dictCode 多字段 DTO
     */
    static class TestSameDictCodeDTO {
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

    /**
     * 无注解 DTO
     */
    static class TestNoAnnotationDTO {
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
     * 类型不兼容 DTO
     */
    static class TypeIncompatibleDTO {
        @ExcelEnum(enumClass = StatusEnum.class, codeField = "code", labelField = "label")
        private Integer statusCode;

        public Integer getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
        }
    }

    /**
     * 状态枚举
     */
    enum StatusEnum {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");

        private final int code;
        private final String label;

        StatusEnum(int code, String label) {
            this.code = code;
            this.label = label;
        }

        public int getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }

    @SafeVarargs
    private static <T> List<T> asList(T... rows) {
        List<T> result = new ArrayList<>();
        Collections.addAll(result, rows);
        return result;
    }
}
