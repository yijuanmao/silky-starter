package com.silky.starter.excel.template;

import com.silky.starter.excel.ExcelApplicationTest;
import com.silky.starter.excel.core.model.export.ExportPageData;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.template.entity.UserTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel导出模板测试类
 *
 * @author zy
 * @date 2025-10-31 16:53
 **/
public class ExcelExportTemplateTest extends ExcelApplicationTest {
    @Autowired
    private ExcelExportTemplate excelExportTemplate;

    /**
     * 测试导出
     */
    @Test
    public void testExport() {
        // 创建导出请求
        ExportRequest<UserTest> request = new ExportRequest<>();
        request.setDataClass(UserTest.class);
        request.setFileName("test.xls");
        request.setPageSize(100);
        request.setDataSupplier((pageNum, pageSize, params) -> {
            List<UserTest> userTests = this.findByCondition();
            return new ExportPageData<>(userTests, false);
        });
        ExportResult result = excelExportTemplate.exportSync(request);
        log.info("导出结果: {}", result);
    }

    /**
     * 查询数据库
     */
    private List<UserTest> findByCondition() {
        List<UserTest> list = new ArrayList<>();
        list.add(new UserTest("张三", "13800138000"));
        list.add(new UserTest("李四", "13800138001"));
        list.add(new UserTest("王五", "13800138002"));
        return list;
    }
}
