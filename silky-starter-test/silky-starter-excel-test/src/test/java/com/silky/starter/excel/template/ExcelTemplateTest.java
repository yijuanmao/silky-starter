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
public class ExcelTemplateTest extends ExcelApplicationTest {

    // 提取测试数据为静态变量，提高复用率并降低内存消耗
    private static final List<UserTest> TEST_DATA = createTestData();

    @Autowired
    private ExcelTemplate excelTemplate;

    /**
     * 测试同步导出
     */
    @Test
    public void testExportSync() {
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
        ExportResult result = excelTemplate.exportSync(request);
        log.info("导出结果: {}", result);
    }

    /**
     * 测试异步导出
     */
    @Test
    public void testExportAsync() {
        boolean hasNext = true;
        // 创建导出请求
        ExportRequest<UserTest> request = new ExportRequest<>();
        request.setDataClass(UserTest.class);
        request.setFileName("test_async.xls");
        request.setPageSize(10);
        request.setMaxRowsPerSheet(10000L);
        request.setDataSupplier((pageNum, pageSize, params) -> {
            //这里模拟数据库分页查询
            List<UserTest> userTests = this.findByCondition(pageNum, pageSize);
            //hasNext 用于是否有下一页数据,如果查询findByCondition方法使用分页插件，就可以从分页插件中获取是否有下一页；·
            return new ExportPageData<>(userTests, hasNext);
        });
        ExportResult result = excelTemplate.exportAsync(request);
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

    /**
     * 创建测试数据
     *
     * @return 测试数据列表
     */
    private static List<UserTest> createTestData() {
        int size = 10;
        // 预设容量避免频繁扩容，提高性能
        List<UserTest> list = new ArrayList<>(size);

        for (int i = 1; i <= size; i++) {
            // 使用规律性数据便于识别和测试
            list.add(new UserTest("用户" + i, "1380013" + String.format("%04d", i)));
        }
        return list;
    }

    private List<UserTest> getAllTestData() {
        return TEST_DATA;
    }
}
