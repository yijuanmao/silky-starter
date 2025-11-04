package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import com.silky.starter.excel.core.exception.ExcelExportException;
import lombok.Getter;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Excel写入器 封装FastExcelWriter，支持大数据量按Sheet分页写入，避免单个Sheet数据过多导致的性能问题
 *
 * @author zy
 * @date 2025-10-24 16:01
 **/
@Getter
public class EnhancedExcelWriter implements Closeable {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EnhancedExcelWriter.class);

    /**
     * 获取底层FastExcelWriter实例
     */
    @Getter
    private final ExcelWriter writer;

    /**
     * 文件路径
     */
    @Getter
    private final String filePath;

    /**
     * 获取总写入行数
     */
    @Getter
    private final AtomicLong totalRows = new AtomicLong(0);

    /**
     * 获取当前Sheet序号
     */
    @Getter
    private int currentSheet = 1;

    private int currentSheetIndex = 0;

    /**
     * 获取当前Sheet行数
     */
    @Getter
    private final AtomicLong currentSheetRows = new AtomicLong(0);

    /**
     * 获取每个Sheet的最大行数
     */
    @Getter
    private final long maxRowsPerSheet;

    /**
     * 表头是否已写入,用于新Sheet创建时判断是否需要重新写入表头
     */
    private boolean headerWritten = false;

    /**
     * 当前Sheet的表头
     */
    private List<String> currentHeaders;

    /**
     * 当前WriteSheet实例
     */
    private WriteSheet currentWriteSheet;


    // 常量定义
    private static final int DEFAULT_MAX_ROWS_PER_SHEET = 200000;

    private static final String DEFAULT_SHEET_NAME = "数据";


    /**
     * 构造方法
     *
     * @param filePath        Excel文件路径
     * @param maxRowsPerSheet 每个Sheet的最大行数（包含表头）
     */
    public EnhancedExcelWriter(String filePath, long maxRowsPerSheet) {
        this.filePath = filePath;
        this.maxRowsPerSheet = maxRowsPerSheet;

        try {
            this.writer = EasyExcel.write(FileUtil.getOutputStream(filePath))
                    .autoCloseStream(false)
                    .build();
            log.info("增强Excel写入器初始化成功: {}, 每Sheet最大行数: {}", filePath, maxRowsPerSheet);
        } catch (Exception e) {
            log.error("增强Excel写入器初始化失败: {}", filePath, e);
            throw new ExcelExportException("增强Excel写入器初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入数据（推荐使用） - 自动处理表头和分Sheet
     * @param clazz
     */
    public <T> void write(List<T> data, Class<T> clazz, String sheetName,
                          Map<String, String> headerMapping) {
        if (data == null || data.isEmpty()) {
            log.debug("数据为空，跳过写入");
            return;
        }

        int fromIndex = 0;
        final int totalSize = data.size();

        while (fromIndex < totalSize) {
            // 检查是否需要创建新Sheet
            if (needNewSheet()) {
                createNewSheet(sheetName, clazz, headerMapping);
            }

            // 计算当前批次大小
            long remainingCapacity = maxRowsPerSheet - currentSheetRows.get() - 1; // -1 为表头
            int batchSize = (int) Math.min(remainingCapacity, totalSize - fromIndex);

            if (batchSize <= 0) {
                // 当前Sheet已满，创建新Sheet后继续
                continue;
            }

            List<T> batchData = data.subList(fromIndex, fromIndex + batchSize);
            writeBatchData(batchData);

            fromIndex += batchSize;
        }
    }

    /**
     * 写入数据
     */
    public <T> void write(List<T> data, Class<T> clazz, Map<String, String> headerMapping) {
        write(data, clazz, DEFAULT_SHEET_NAME, headerMapping);
    }

    /**
     * 写入数据（最简版本）
     */
    public <T> void write(List<T> data, Class<T> clazz) {
        write(data, clazz, DEFAULT_SHEET_NAME, null);
    }

    /**
     * 手动写入表头（适用于需要精确控制表头的情况）
     */
    public <T> void writeHeader(Class<T> clazz, Map<String, String> headerMapping, String sheetName) {
        createNewSheet(sheetName, clazz, headerMapping);
    }

    public <T> void writeHeader(Class<T> clazz, Map<String, String> headerMapping) {
        writeHeader(clazz, headerMapping, DEFAULT_SHEET_NAME);
    }

    public <T> void writeHeader(Class<T> clazz) {
        writeHeader(clazz, null, DEFAULT_SHEET_NAME);
    }

    /**
     * 写入批次数据
     */
    private <T> void writeBatchData(List<T> data) {
        try {
            writer.write(data, currentWriteSheet);

            int batchSize = data.size();
            totalRows.addAndGet(batchSize);
            currentSheetRows.addAndGet(batchSize);

            log.debug("Excel数据批次写入成功，Sheet: {}, 数据量: {}, 当前Sheet行数: {}, 总行数: {}",
                    getCurrentSheetName(), batchSize, currentSheetRows.get(), totalRows.get());

        } catch (Exception e) {
            log.error("Excel数据批次写入失败", e);
            throw new ExcelExportException("Excel数据批次写入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查是否需要创建新Sheet
     */
    private boolean needNewSheet() {
        return currentWriteSheet == null || currentSheetRows.get() >= maxRowsPerSheet - 1;
    }

    /**
     * 创建新Sheet
     */
    private <T> void createNewSheet(String baseSheetName, Class<T> clazz,
                                    Map<String, String> headerMapping) {
        currentSheetIndex++;
        currentSheetRows.set(0);

        String sheetName = getSheetName(baseSheetName, currentSheetIndex);

        try {
            // 构建表头
            this.currentHeaders = buildHeaders(clazz, headerMapping);

            // 创建WriteSheet
            this.currentWriteSheet = EasyExcel.writerSheet(sheetName)
                    .head(buildEasyExcelHeaders(currentHeaders))
                    .build();

            log.debug("创建新Sheet: {}", sheetName);

        } catch (Exception e) {
            log.error("创建新Sheet失败: {}", sheetName, e);
            throw new ExcelExportException("创建新Sheet失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建表头列表
     */
    private <T> List<String> buildHeaders(Class<T> clazz, Map<String, String> headerMapping) {
        Field[] fields = clazz.getDeclaredFields();
        List<String> headers = new ArrayList<>(fields.length);
        for (Field field : fields) {
            String headerName = getHeaderName(field, headerMapping);
            headers.add(headerName);
        }
        return headers;
    }

    /**
     * 构建EasyExcel需要的表头格式
     */
    private List<List<String>> buildEasyExcelHeaders(List<String> headers) {
        List<List<String>> easyExcelHeaders = new ArrayList<>(headers.size());
        for (String header : headers) {
            easyExcelHeaders.add(ListUtil.toList(header));
        }
        return easyExcelHeaders;
    }

    /**
     * 获取表头名称
     */
    private String getHeaderName(Field field, Map<String, String> headerMapping) {
        String fieldName = field.getName();

        // 优先使用自定义映射
        if (headerMapping != null && headerMapping.containsKey(fieldName)) {
            return headerMapping.get(fieldName);
        }
        // 使用注解
        cn.idev.excel.annotation.ExcelProperty annotation =
                field.getAnnotation(cn.idev.excel.annotation.ExcelProperty.class);
        if (annotation != null && annotation.value().length > 0) {
            return annotation.value()[0];
        }
        // 默认使用字段名
        return fieldName;
    }

    /**
     * 获取当前Sheet名称
     */
    public String getCurrentSheetName() {
        return getSheetName(DEFAULT_SHEET_NAME, currentSheetIndex);
    }

    private String getSheetName(String baseName, int sheetIndex) {
        return sheetIndex == 1 ? baseName : baseName + "_" + sheetIndex;
    }

    /**
     * 获取当前统计信息
     */
    public String getStats() {
        return String.format("总行数: %d, 当前Sheet: %s, 当前Sheet行数: %d",
                totalRows.get(), getCurrentSheetName(), currentSheetRows.get());
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
                log.info("增强Excel写入器关闭成功: {}, 总Sheet数: {}, 总写入行数: {}",
                        filePath, currentSheetIndex, totalRows.get());
            } catch (Exception e) {
                log.error("关闭增强Excel写入器失败: {}", filePath, e);
                throw new ExcelExportException("关闭Excel写入器失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 使用try-with-resources的便捷方法
     */
//    public static void writeToFile(String filePath, List<?> data, Class<?> clazz) {
//        try (EnhancedExcelWriter writer = new EnhancedExcelWriter(filePath)) {
//            writer.write(data, clazz);
//        }
//    }
}
