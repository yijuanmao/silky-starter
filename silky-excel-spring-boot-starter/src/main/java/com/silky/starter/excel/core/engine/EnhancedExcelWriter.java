package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollectionUtil;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.metadata.WriteSheet;
import com.silky.starter.excel.core.exception.ExcelExportException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
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

    private static final Logger log = LoggerFactory.getLogger(EnhancedExcelWriter.class);

    private final ExcelWriter writer;

    private final String filePath;

    /**
     * 每个Sheet最大行数
     */
    private final long maxRowsPerSheet;

    /**
     * 总写入行数
     */
    private final AtomicLong totalRows = new AtomicLong(0);

    /**
     * 当前Sheet总行数
     */
    private final AtomicLong currentSheetRows = new AtomicLong(0);

    /**
     * 当前Sheet索引
     */
    private int currentSheetIndex = 0;

    /**
     * 当前WriteSheet
     */
    private WriteSheet currentWriteSheet;

    private List<String> currentHeaders;

    private static final String DEFAULT_SHEET_NAME = "数据";

    public EnhancedExcelWriter(String filePath, long maxRowsPerSheet) {
        this.filePath = filePath;
        this.maxRowsPerSheet = maxRowsPerSheet;

        try {
            this.writer = EasyExcel.write(filePath)
                    .autoCloseStream(true)  // 设置为true让EasyExcel管理流
                    .build();

            log.info("Excel写入器初始化成功: {}, 每Sheet最大行数: {}", filePath, maxRowsPerSheet);
        } catch (Exception e) {
            log.error("Excel写入器初始化失败: {}", filePath, e);
            throw new ExcelExportException("Excel写入器初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入数据
     *
     * @param data          数据列表
     * @param clazz         数据类
     * @param sheetName     Sheet名称
     * @param headerMapping 表头映射
     */
    public <T> void write(List<T> data, Class<T> clazz, String sheetName,
                          Map<String, String> headerMapping) {
        if (CollectionUtil.isEmpty(data)) {
            log.debug("数据为空，跳过写入");
            return;
        }
        // 修复：确保有Sheet存在
        if (currentWriteSheet == null) {
            createNewSheet(sheetName, clazz);
        }

        int fromIndex = 0;
        final int totalSize = data.size();

        while (fromIndex < totalSize) {
            if (needNewSheet()) {
                createNewSheet(sheetName, clazz);
            }

            long remainingCapacity = maxRowsPerSheet - currentSheetRows.get();
            int batchSize = (int) Math.min(remainingCapacity, totalSize - fromIndex);

            if (batchSize <= 0) {
                continue;
            }

            List<T> batchData = data.subList(fromIndex, fromIndex + batchSize);
            writeBatchData(batchData);
            fromIndex += batchSize;
        }
    }

    /**
     * 写入批次数据
     */
    private <T> void writeBatchData(List<T> data) {
        try {
            if (currentWriteSheet == null) {
                throw new ExcelExportException("WriteSheet未初始化");
            }

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
     * 创建新Sheet
     *
     * @param baseSheetName 基础Sheet名称
     * @param clazz         数据类
     */
    private <T> void createNewSheet(String baseSheetName, Class<T> clazz) {
        currentSheetIndex++;
        currentSheetRows.set(0);

        String sheetName = getSheetName(baseSheetName, currentSheetIndex);

        try {
            // 修复：使用EasyExcel的head直接设置数据类，而不是手动构建表头
            this.currentWriteSheet = EasyExcel.writerSheet(sheetName)
                    .head(clazz)  // 修复：直接使用数据类，EasyExcel会自动处理注解
                    .build();

            log.info("创建新Sheet: {}", sheetName);

        } catch (Exception e) {
            log.error("创建新Sheet失败: {}", sheetName, e);
            throw new ExcelExportException("创建新Sheet失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入数据
     *
     * @param data          数据列表
     * @param clazz         数据类
     * @param headerMapping 表头映射
     */
    public <T> void write(List<T> data, Class<T> clazz, Map<String, String> headerMapping) {
        this.write(data, clazz, DEFAULT_SHEET_NAME, headerMapping);
    }

    /**
     * 写入数据
     *
     * @param data  数据列表
     * @param clazz 数据类
     */
    public <T> void write(List<T> data, Class<T> clazz) {
        write(data, clazz, DEFAULT_SHEET_NAME, null);
    }

    private boolean needNewSheet() {
        return currentWriteSheet == null || currentSheetRows.get() >= maxRowsPerSheet;
    }

    public String getCurrentSheetName() {
        return getSheetName(DEFAULT_SHEET_NAME, currentSheetIndex);
    }

    private String getSheetName(String baseName, int sheetIndex) {
        return sheetIndex == 1 ? baseName : baseName + "_" + sheetIndex;
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.finish();  // 修复：先调用finish确保数据写入完成
                writer.close();
                log.info("Excel写入器关闭成功: {}, 总Sheet数: {}, 总写入行数: {}",
                        filePath, currentSheetIndex, totalRows.get());
            } catch (Exception e) {
                log.error("关闭Excel写入器失败: {}", filePath, e);
                throw new ExcelExportException("关闭Excel写入器失败: " + e.getMessage(), e);
            }
        }
    }
}
