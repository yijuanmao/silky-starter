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
import com.silky.starter.excel.core.model.AnalysisListenersContext;
import com.silky.starter.excel.core.model.imports.ImportResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.util.List;

/**
 * Excel读取器，封装FastExcelReader，提供大数据量分页读取功能
 *
 * @author zy
 * @date 2025-10-27 15:16
 **/
@Slf4j
public class ExcelReaderWrapper<T> implements Closeable {

    private final ExcelReader reader;
    @Getter
    private final String filePath;

    private final BaseAnalysisListeners<T> baseAnalysisListeners;

    /**
     * Sheet列表
     */
    private final List<ReadSheet> sheets;

    public ExcelReaderWrapper(String filePath, boolean skipHeader, AnalysisListenersContext<T> context) {
        this(filePath, skipHeader, new DefaultAnalysisListeners<>(context));
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

            log.info("Excel 读取器初始化成功: {}, Sheet数量: {}", filePath, sheets != null ? sheets.size() : 1);
        } catch (Exception e) {
            log.error("Excel 读取器初始化失败: {}", filePath, e);
            throw new ExcelExportException("Excel 读取器初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按Sheet读取数据
     */
    public void doRead() {
        for (ReadSheet sheet : this.sheets) {
            reader.read(sheet);
        }
    }

    /**
     * 读取所有数据
     */
    public void doReadAll() {
        reader.readAll();
    }

    /**
     * 获取总sheet数量
     */
    public int getTotalSheetCount() {
        return CollUtil.isNotEmpty(sheets) ? sheets.size() : 1;
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
        return this.getSuccessRowCount() + this.getFailRowCount();
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
                        filePath, getTotalSheetCount(), getAllCount());
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
