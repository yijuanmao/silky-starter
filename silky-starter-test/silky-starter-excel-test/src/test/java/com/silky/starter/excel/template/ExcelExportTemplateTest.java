package com.silky.starter.excel.template;

import com.silky.starter.excel.ExcelApplicationTest;
import com.silky.starter.excel.core.model.export.ExportPageData;
import com.silky.starter.excel.core.model.export.ExportRequest;
import com.silky.starter.excel.core.model.export.ExportResult;
import com.silky.starter.excel.template.entity.UserTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
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
        request.setPageSize(10);
        request.setDataSupplier((pageNum, pageSize, params) -> {
            //这里模拟数据库分页查询
            List<UserTest> userTests = this.findByCondition(pageNum, pageSize);
            return new ExportPageData<>(userTests, true);
        });

        // 设置每个Sheet的最大行数为1，测试多Sheet导出
//        request.setMaxRowsPerSheet(1);
        ExportResult result = excelExportTemplate.exportSync(request);
        log.info("导出结果: {}", result);
    }


    /**
     * 查询数据库（带分页）
     */
    private List<UserTest> findByCondition(int pageNum, int pageSize) {
        // 调整pageNum从1开始的偏移
        int adjustedPageNum = pageNum - 1;
        if (adjustedPageNum < 0) {
            return Collections.emptyList();
        }
        List<UserTest> allData = getAllTestData();
        int fromIndex = adjustedPageNum * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allData.size());
        if (fromIndex >= allData.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(allData.subList(fromIndex, toIndex));
    }

    // 提取测试数据为静态变量，提高复用率并降低内存消耗
    private static final List<UserTest> TEST_DATA = createTestData();

    private static List<UserTest> createTestData() {
        List<UserTest> list = new ArrayList<>(10);
        list.add(new UserTest("张三", "13800138000"));
        list.add(new UserTest("李四", "13800138001"));
        list.add(new UserTest("王五", "13800138002"));
        list.add(new UserTest("王️六", "13800138003"));
        list.add(new UserTest("赵七", "13800138004"));
        list.add(new UserTest("孙八", "13800138005"));
        list.add(new UserTest("周九", "13800138006"));
        list.add(new UserTest("吴八", "13800138007"));
        list.add(new UserTest("郑十", "13800138008"));
        return list;
    }

    private List<UserTest> getAllTestData() {
        return TEST_DATA;
    }
}
