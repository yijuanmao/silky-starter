package com.silky.starter.excel.core.listener;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import cn.idev.excel.exception.ExcelDataConvertException;
import com.silky.starter.excel.core.exception.ExcelExportException;
import com.silky.starter.excel.core.model.AnalysisListenersContext;
import com.silky.starter.excel.core.model.imports.DataImporterSupplier;
import com.silky.starter.excel.core.model.imports.ImportRequest;
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

    private final int pageSize;

    private final int maxErrorCount;

    private final List<T> dataList = new ArrayList<>();

    private final List<ImportResult.ImportError> allErrors = new ArrayList<>();

    private final Map<Integer, String> headerIndexMap = new HashMap<>();

    private final List<ImportResult.ImportError> currentSheetErrors = new ArrayList<>();

    private final AtomicInteger currentSheetRowCount = new AtomicInteger(0);

    private final AtomicLong successCount = new AtomicLong(0);

    private final AtomicLong failCount = new AtomicLong(0);

    private final AtomicLong importedCount = new AtomicLong(0);

    private String currentSheetName;

    private final DataImporterSupplier<T> dataImporterSupplier;

    private final ImportRequest<T> importRequest;

    private final AnalysisListenersContext<T> context;

    public BaseAnalysisListeners(AnalysisListenersContext<T> context) {
        context.validate();
        this.context = context;
        this.maxErrorCount = context.getMaxErrorCount();
        this.pageSize = context.getPageSize();
        this.importRequest = context.getRequest();
        this.dataImporterSupplier = context.getRequest().getDataImporterSupplier();
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        headerIndexMap.clear();
        headerIndexMap.putAll(headMap);
        super.invokeHeadMap(headMap, context);
    }

    @Override
    public void invoke(T t, AnalysisContext analysisContext) {
        dataList.add(t);
        if (dataList.size() >= pageSize) {
            flushData();
        }
        currentSheetRowCount.incrementAndGet();
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        flushData();
        context.setTotalCount((int) (successCount.get() + failCount.get()));
        context.setSuccessCount((int) successCount.get());
        context.setFailCount((int) failCount.get());
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
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
     * 将当前缓存的数据批量提交给业务导入器。
     */
    private void flushData() {
        if (dataList.isEmpty()) {
            return;
        }
        List<T> processedData = new ArrayList<>(dataList);
        DataImporterSupplier.ImportBatchResult result =
                dataImporterSupplier.importData(processedData, importRequest.getParams());
        long success = result != null ? result.getSuccessCount() : processedData.size();
        long failed = result != null ? result.getFailedCount() : 0;
        successCount.addAndGet(success);
        failCount.addAndGet(failed);
        importedCount.addAndGet(success);
        if (result != null && result.getErrors() != null && !result.getErrors().isEmpty()) {
            allErrors.addAll(result.getErrors());
            currentSheetErrors.addAll(result.getErrors());
        }
        dataList.clear();
    }
}
