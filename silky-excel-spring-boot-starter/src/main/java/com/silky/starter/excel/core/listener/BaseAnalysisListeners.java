package com.silky.starter.excel.core.listener;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import cn.idev.excel.exception.ExcelDataConvertException;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.imports.ImportResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Excel导入基础监听器
 *
 * @author zy
 * @date 2025-11-13 14:49
 **/
@Slf4j
@Getter
public abstract class BaseAnalysisListeners<T> extends AnalysisEventListener<T> {

    private final int maxReadCount;

    private final int maxErrorCount;

    private final List<T> dataList = new ArrayList<>();

    private final List<ImportResult.ImportError> allErrors = new ArrayList<>();

    private final Map<Integer, String> headerIndexMap = new HashMap<>();

    private final List<ImportResult.ImportError> currentSheetErrors = new ArrayList<>();

    private final AtomicInteger currentSheetRowCount = new AtomicInteger(0);

    private final AtomicLong successCount = new AtomicLong(0);

    private final AtomicLong failCount = new AtomicLong(0);

    private volatile boolean paused = false;

    private volatile boolean sheetFinished = false;

    private volatile boolean allFinished;

    private String currentSheetName;

    public BaseAnalysisListeners(Integer maxErrorCount, Integer maxReadCount) {
        this.maxErrorCount = maxErrorCount;
        this.maxReadCount = maxReadCount;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        headerIndexMap.clear();
        headerIndexMap.putAll(headMap);
        super.invokeHeadMap(headMap, context);
    }

    @Override
    public void invoke(T t, AnalysisContext analysisContext) {
        // 当达到批次大小时暂停读取
        if (dataList.size() >= maxReadCount) {
            this.paused = true;
            throw new PauseReadException("达到批次大小，暂停读取");
        }
        dataList.add(t);
        successCount.incrementAndGet();
        currentSheetRowCount.incrementAndGet();
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        this.sheetFinished = true;
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        // 如果是暂停异常，直接返回
        if (exception instanceof PauseReadException) {
//            throw new PauseReadException(exception.getMessage());
            return;
        }
        failCount.incrementAndGet();

        int rowIndex = -1;
        int columnIndex = -1;
        String header = "";

        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException excelException = (ExcelDataConvertException) exception;
            rowIndex = excelException.getRowIndex();
            columnIndex = excelException.getColumnIndex();

            if (headerIndexMap.containsKey(columnIndex)) {
                header = headerIndexMap.get(columnIndex);
            }

            // 构建带sheet信息的错误信息
            String errorMessage = String.format("Sheet[%s] 第%d行，第%d列(%s)数据格式错误: %s",
                    currentSheetName, rowIndex + 1, columnIndex + 1, header, excelException.getMessage());

            ImportResult.ImportError error = ImportResult.ImportError.of(rowIndex, header, errorMessage, currentSheetName);

            allErrors.add(error);
            currentSheetErrors.add(error);
        } else {
            log.error("处理数据时发生异常", exception);
            ImportResult.ImportError error = ImportResult.ImportError.of(rowIndex, header,
                    "未知错误：" + exception.getMessage(), currentSheetName);

            allErrors.add(error);
            currentSheetErrors.add(error);
        }

        if (allErrors.size() > maxErrorCount) {
            throw new ExcelExportException("导入错误数量超过最大限制: " + maxErrorCount);
        }
    }

    /**
     * 获取当前批次数据并清空，准备下一批次读取
     */
    public List<T> getAndClearBatchData() {
        List<T> batchData = new ArrayList<>(dataList);
        dataList.clear();
        paused = false;
        return batchData;
    }

    /**
     * 重置批次状态（同一sheet内）
     */
    public void resetBatchState() {
        this.paused = false;
    }

    /**
     * 准备读取新sheet
     */
    public void resetForNewSheet() {
        this.dataList.clear();
        this.currentSheetErrors.clear();
        this.currentSheetRowCount.set(0);
        this.headerIndexMap.clear();
        this.paused = false;
        this.sheetFinished = false;
        this.currentSheetName = null;
    }

    /**
     * 设置当前sheet名称
     */
    public void setCurrentSheetName(String sheetName) {
        this.currentSheetName = sheetName;
    }

    /**
     * 检查当前sheet是否还有更多数据
     */
    public boolean hasMoreDataInCurrentSheet() {
        return !sheetFinished || !dataList.isEmpty();
    }

    /**
     * 检查是否所有数据都已完成
     */
    public boolean isAllFinished() {
        return allFinished;
    }

    /**
     * 获取当前sheet的错误信息
     */
    public List<ImportResult.ImportError> getCurrentSheetErrors() {
        return new ArrayList<>(currentSheetErrors);
    }

    /**
     * 获取当前sheet的行数
     */
    public int getCurrentSheetRowCount() {
        return currentSheetRowCount.get();
    }

    /**
     * 暂停读取异常
     */
    public static class PauseReadException extends RuntimeException {
        public PauseReadException(String message) {
            super(message);
        }
    }
}
