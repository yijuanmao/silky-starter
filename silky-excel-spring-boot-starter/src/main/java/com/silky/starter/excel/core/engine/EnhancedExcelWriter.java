package com.silky.starter.excel.core.engine;

import cn.hutool.core.io.FileUtil;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import cn.idev.excel.write.metadata.WriteSheet;
import com.silky.starter.excel.core.exception.ExcelExportException;
import lombok.Getter;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

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
    private long totalRows = 0;

    /**
     * 获取当前Sheet序号
     */
    @Getter
    private int currentSheet = 1;

    /**
     * 获取当前Sheet行数
     */
    @Getter
    private long currentSheetRows = 0;

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
    private String[] currentHeaders;

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
            this.writer = FastExcelFactory
                    .write(FileUtil.newFile(filePath))
                    .autoCloseStream(false)
                    .build();

//            this.writer = new FastExcelWriter(filePath);
            log.info("增强Excel写入器初始化成功: {}, 每Sheet最大行数: {}", filePath, maxRowsPerSheet);
        } catch (Exception e) {
            log.error("增强Excel写入器初始化失败: {}", filePath, e);
            throw new ExcelExportException("增强Excel写入器初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构造方法（默认每Sheet最大行数为20万）
     *
     * @param filePath Excel文件路径
     */
    public EnhancedExcelWriter(String filePath) {
        this(filePath, 200000); // 默认20万行 per sheet
    }

    /**
     * 写入表头
     *
     * @param data          数据列表
     * @param clazz         数据类类型
     * @param headerMapping 表头映射
     * @param sheetName     Sheet名称
     * @param <T>           数据类型
     */
    public <T> void writeHeader(List<T> data, Class<T> clazz, Map<String, String> headerMapping, String sheetName) {
        if (data == null || data.isEmpty()) {
            log.debug("数据为空，跳过表头写入");
            return;
        }

        // 检查是否需要创建新Sheet
        checkAndCreateNewSheet(sheetName);

        try {
            // 获取所有字段
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            String[] headers = new String[fields.length];

            for (int i = 0; i < fields.length; i++) {
                java.lang.reflect.Field field = fields[i];
                String headerName = getHeaderName(field, headerMapping);
                headers[i] = headerName;
            }

            // 写入表头
//            writer.writeHeader(headers);
            headerWritten = true;
            currentHeaders = headers;
            currentSheetRows++; // 表头占一行

            log.debug("Excel表头写入成功，Sheet: {}, 列数: {}", getCurrentSheetName(), headers.length);

        } catch (Exception e) {
            log.error("Excel表头写入失败", e);
            throw new ExcelExportException("Excel表头写入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入表头（使用默认Sheet名称）
     */
    public <T> void writeHeader(List<T> data, Class<T> clazz, Map<String, String> headerMapping) {
        writeHeader(data, clazz, headerMapping, getDefaultSheetName());
    }

    /**
     * 写入表头（使用默认表头生成和Sheet名称）
     */
    public <T> void writeHeader(List<T> data, Class<T> clazz) {
        writeHeader(data, clazz, null, getDefaultSheetName());
    }

    /**
     * 写入数据（支持自动分Sheet）
     *
     * @param data          数据列表
     * @param clazz         数据类类型
     * @param sheetName     Sheet名称模板（会自动添加序号）
     * @param headerMapping 表头映射
     * @param <T>           数据类型
     */
    public <T> void write(List<T> data, Class<T> clazz, String sheetName, Map<String, String> headerMapping) {
        if (data == null || data.isEmpty()) {
            log.debug("数据为空，跳过写入");
            return;
        }

        // 分批写入，考虑Sheet行数限制
        int batchSize = data.size();
        int fromIndex = 0;

        while (fromIndex < batchSize) {
            // 计算当前Sheet还能写入多少行
            long remainingRows = maxRowsPerSheet - currentSheetRows;
            if (remainingRows <= 0) {
                // 当前Sheet已满，创建新Sheet
                createNewSheet(sheetName);
                writeHeader(data, clazz, headerMapping, getCurrentSheetName());
                remainingRows = maxRowsPerSheet - currentSheetRows;
            }

            int toIndex = fromIndex + (int) Math.min(remainingRows, batchSize - fromIndex);
            List<T> batchData = data.subList(fromIndex, toIndex);

            // 写入当前批次数据
            writeBatchData(batchData, clazz);

            fromIndex = toIndex;
        }
    }

    /**
     * 写入数据（使用默认Sheet名称）
     */
    public <T> void write(List<T> data, Class<T> clazz, Map<String, String> headerMapping) {
        write(data, clazz, getDefaultSheetName(), headerMapping);
    }

    /**
     * 写入数据（使用默认表头生成和Sheet名称）
     */
    public <T> void write(List<T> data, Class<T> clazz) {
        write(data, clazz, getDefaultSheetName(), null);
    }

    /**
     * 写入批次数据
     */
    private <T> void writeBatchData(List<T> data, Class<T> clazz) {
        try {
            WriteSheet writeSheet = EasyExcel.writerSheet(getCurrentSheetName()).build();
//            writer.write(data, clazz);
            writer.write(data, writeSheet);
            int dataSize = data.size();
            totalRows += dataSize;
            currentSheetRows += dataSize;

            log.debug("Excel数据批次写入成功，Sheet: {}, 数据量: {}, 当前Sheet行数: {}, 总行数: {}",
                    getCurrentSheetName(), dataSize, currentSheetRows, totalRows);

        } catch (Exception e) {
            log.error("Excel数据批次写入失败", e);
            throw new ExcelExportException("Excel数据批次写入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查并创建新Sheet
     */
    private void checkAndCreateNewSheet(String sheetName) {
        if (currentSheetRows >= maxRowsPerSheet) {
            createNewSheet(sheetName);
        }
    }

    /**
     * 创建新Sheet
     */
    private void createNewSheet(String sheetName) {
        currentSheet++;
        currentSheetRows = 0;
        headerWritten = false;

        try {
//            writer.createSheet(getCurrentSheetName(sheetName));
            log.debug("创建新Sheet: {}", getCurrentSheetName(sheetName));

            // 如果之前有表头，在新Sheet中重新写入
            if (currentHeaders != null) {
//                writer.writeHeader(currentHeaders);
                headerWritten = true;
                currentSheetRows++; // 表头占一行
            }

        } catch (Exception e) {
            log.error("创建新Sheet失败", e);
            throw new ExcelExportException("创建新Sheet失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取当前Sheet名称
     */
    private String getCurrentSheetName() {
        return getCurrentSheetName("Sheet");
    }

    /**
     * 获取当前Sheet名称
     */
    private String getCurrentSheetName(String baseName) {
        return currentSheet == 1 ? baseName : baseName + "_" + currentSheet;
    }

    /**
     * 获取默认Sheet名称
     */
    private String getDefaultSheetName() {
        return "数据";
    }

    /**
     * 获取表头名称
     */
    private String getHeaderName(java.lang.reflect.Field field, Map<String, String> headerMapping) {
        String fieldName = field.getName();

        // 优先使用自定义映射
        if (headerMapping != null && headerMapping.containsKey(fieldName)) {
            return headerMapping.get(fieldName);
        }

        // 其次使用注解
        cn.idev.excel.annotation.ExcelProperty annotation = field.getAnnotation(cn.idev.excel.annotation.ExcelProperty.class);
        if (annotation != null && annotation.value().length > 0) {
            return annotation.value()[0];
        }

        // 最后使用字段名
        return fieldName;
    }

    /**
     * 关闭写入器
     */
    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
                log.info("增强Excel写入器关闭成功: {}, 总Sheet数: {}, 总写入行数: {}",
                        filePath, currentSheet, totalRows);
            } catch (Exception e) {
                log.error("关闭增强Excel写入器失败: {}", filePath, e);
            }
        }
    }


}
