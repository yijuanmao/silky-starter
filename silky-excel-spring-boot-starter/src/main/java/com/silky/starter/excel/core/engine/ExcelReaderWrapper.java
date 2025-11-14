package com.silky.starter.excel.core.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.idev.excel.ExcelReader;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.read.metadata.ReadSheet;
import cn.idev.excel.support.ExcelTypeEnum;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.listener.BaseAnalysisListeners;
import com.silky.starter.excel.core.listener.DefaultAnalysisListeners;
import com.silky.starter.excel.core.model.imports.ImportResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Excel读取器，封装FastExcelReader，提供大数据量分页读取功能
 *
 * @author zy
 * @date 2025-10-27 15:16
 **/
@Slf4j
public class ExcelReaderWrapper<T> implements Closeable {

    private int currentPage = 0;
    private int pageSize = 10000; // 默认每页10000条
    private final ExcelReader reader;
    @Getter
    private final String filePath;
    @Getter
    private long totalRows = 0;

    private final BaseAnalysisListeners<T> baseAnalysisListeners;

    /**
     * Sheet列表
     */
    private final List<ReadSheet> sheets;

    private final AtomicInteger currentSheetIndex = new AtomicInteger(0);

    private final boolean multiSheetMode;

    private String currentSheetName;

    // 批次控制
    private int currentBatch = 0;

    private boolean sheetFinished = false;

    private boolean allSheetsFinished = false;

    public ExcelReaderWrapper(String filePath, boolean skipHeader, int maxErrorCount, int maxReadCount) {
        this(filePath, skipHeader, new DefaultAnalysisListeners<>(maxErrorCount, maxReadCount));
    }

    public ExcelReaderWrapper(String filePath, boolean skipHeader, BaseAnalysisListeners<T> baseAnalysisListeners) {
        File file = FileUtil.newFile(filePath);
        this.filePath = filePath;
        this.baseAnalysisListeners = baseAnalysisListeners;
        try {
            this.reader = FastExcelFactory
                    .read()
                    .file(file)
                    .autoCloseStream(false)
                    .headRowNumber(skipHeader ? 1 : 0)
                    .excelType(this.getExcelType(filePath))
                    .registerReadListener(baseAnalysisListeners)
                    .build();

            // 初始化sheet列表
            this.sheets = reader.excelExecutor().sheetList();
            this.multiSheetMode = CollUtil.isNotEmpty(sheets);

            log.info("Excel 读取器初始化成功: {}, Sheet数量: {}", filePath,
                    sheets != null ? sheets.size() : 1);
        } catch (Exception e) {
            log.error("Excel 读取器初始化失败: {}", filePath, e);
            throw new ExcelExportException("Excel 读取器初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取下一批次数据 - 支持多sheet分批读取
     */
    public List<T> readNextBatch() {
        if (allSheetsFinished) {
            return Collections.emptyList();
        }
        try {
            if (sheetFinished) {
                boolean hasNextSheet = switchToNextSheet();
                if (!hasNextSheet) {
                    allSheetsFinished = true;
                    return Collections.emptyList();
                }
            }
            // 重置批次状态
            baseAnalysisListeners.resetBatchState();
            currentBatch++;
            // 读取当前sheet的下一批次数据
            ReadSheet currentSheet = sheets.get(currentSheetIndex.get());
            reader.read(currentSheet);

            // 获取当前批次数据
            List<T> batchData = baseAnalysisListeners.getAndClearBatchData();
            totalRows += batchData.size();

            // 检查当前sheet是否完成
            sheetFinished = !baseAnalysisListeners.hasMoreDataInCurrentSheet();
            log.debug("读取批次完成: Sheet[{}], 批次[{}], 数据量[{}], 累计行数[{}]",
                    currentSheetName, currentBatch, batchData.size(), totalRows);
            return batchData;
        } catch (BaseAnalysisListeners.PauseReadException e) {
            // 正常暂停，继续下一批次
            log.debug("批次读取暂停: Sheet[{}], 批次[{}]", currentSheetName, currentBatch);
            List<T> batchData = baseAnalysisListeners.getAndClearBatchData();
            totalRows += batchData.size();
            return batchData;
        } catch (Exception e) {
            log.error("读取Excel数据失败: Sheet[{}], 批次[{}]", currentSheetName, currentBatch, e);
            throw new ExcelExportException("读取Excel数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 切换到下一个sheet
     */
    private boolean switchToNextSheet() {
        int nextIndex = currentSheetIndex.incrementAndGet();
        if (nextIndex >= sheets.size()) {
            return false;
        }

        ReadSheet nextSheet = sheets.get(nextIndex);
        currentSheetName = nextSheet.getSheetName();
        sheetFinished = false;
        currentBatch = 0;

        // 重置监听器状态，准备读取新sheet
        baseAnalysisListeners.resetForNewSheet();

        log.info("切换到下一个Sheet: [{}], Index: [{}]", currentSheetName, nextIndex);
        return true;
    }

    /**
     * 是否还有更多数据（包括所有sheet）
     */
    public boolean hasMoreData() {
        return !allSheetsFinished;
    }

    /**
     * 获取当前sheet信息
     */
    public String getCurrentSheetInfo() {
        if (sheets == null || sheets.isEmpty()) {
            return "No Sheet";
        }
        int currentIndex = currentSheetIndex.get();
        if (currentIndex >= sheets.size()) {
            return "Completed";
        }
        ReadSheet sheet = sheets.get(currentIndex);
        return String.format("Sheet[%s](%d/%d)",
                sheet.getSheetName(), currentIndex + 1, sheets.size());
    }

    /**
     * 获取总sheet数量
     */
    public int getTotalSheetCount() {
        return CollUtil.isNotEmpty(sheets) ? sheets.size() : 1;
    }

    /**
     * 获取当前sheet索引
     */
    public int getCurrentSheetIndex() {
        return currentSheetIndex.get();
    }

    /**
     * 获取所有错误信息
     */
    public List<ImportResult.ImportError> getAllErrors() {
        return baseAnalysisListeners.getAllErrors();
    }

    /**
     * 获取当前sheet的错误信息
     */
    public List<ImportResult.ImportError> getCurrentSheetErrors() {
        return baseAnalysisListeners.getCurrentSheetErrors();
    }

    /**
     * 获取已读取的总行数
     *
     * @return 总行数
     */
    public long getAllCount() {
        return this.totalRows;
    }

    /**
     * 获取成功读取的行数
     *
     * @return 成功行数
     */
    public long getSuccessRowCount() {
        return baseAnalysisListeners.getSuccessCount().get();
    }

    /**
     * 获取失败读取的行数
     *
     * @return 成功行数
     */
    public long getFailRowCount() {
        return baseAnalysisListeners.getFailCount().get();
    }

    /**
     * 关闭读取器
     */
    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
                log.info("Excel读取器关闭成功: {}, 总Sheet数: {}, 总读取行数: {}",
                        filePath, getTotalSheetCount(), totalRows);
            } catch (Exception e) {
                log.error("关闭Excel读取器失败: {}", filePath, e);
            }
        }
    }

    /**
     * 获取Excel文件类型
     */
    private ExcelTypeEnum getExcelType(String filePath) {
        String lowerPath = FileUtil.getName(filePath).toLowerCase();
        if (lowerPath.endsWith(".xls")) {
            return ExcelTypeEnum.XLS;
        } else if (lowerPath.endsWith(".xlsx")) {
            return ExcelTypeEnum.XLSX;
        } else if (lowerPath.endsWith(".csv")) {
            return ExcelTypeEnum.CSV;
        } else {
            throw new ExcelExportException("不支持的Excel文件类型: " + filePath);
        }
    }

    /**
     * 获取底层FastExcelReader实例
     *
     * @return ExcelReader实例
     */
    public ExcelReader getFastExcelReader() {
        return this.reader;
    }
}
