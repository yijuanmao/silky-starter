package com.silky.starter.excel.core.engine;

import cn.hutool.core.io.FileUtil;
import cn.idev.excel.ExcelReader;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.read.metadata.ReadSheet;
import com.silky.starter.excel.core.exception.ExcelExportException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.List;

/**
 * Excel读取器，封装FastExcelReader，提供大数据量分页读取功能
 *
 * @author zy
 * @date 2025-10-27 15:16
 **/
@Slf4j
public class EnhancedExcelReader<T> implements Closeable {

    /**
     * 底层FastExcelReader实例
     */
    private final ExcelReader reader;

    /**
     * Excel文件路径
     */
    @Getter
    private final String filePath;

    /**
     * 数据类类型
     */
    private final Class<T> dataClass;

    /**
     * 总行数（估算）
     */
    @Getter
    private long totalRows = 0;

    /**
     * 是否已跳过表头
     */
    private boolean headerSkipped = false;

    /**
     * 是否跳过表头
     */
    private final boolean skipHeader;

    /**
     * 构造方法
     *
     * @param filePath   Excel文件路径
     * @param dataClass  数据类类型
     * @param skipHeader 是否跳过表头
     */
    public EnhancedExcelReader(String filePath, Class<T> dataClass, boolean skipHeader) {
        this.filePath = filePath;
        this.dataClass = dataClass;
        this.skipHeader = skipHeader;
        // 跳过表头
        if (skipHeader) {
            this.headerSkipped = true;
        }
        try {
            this.reader = FastExcelFactory
                    .read(FileUtil.newFile(filePath))
                    .autoCloseStream(false)
                    .headRowNumber(skipHeader ? 1 : 0)
                    .build();
            log.info("Excel读取器初始化成功: {}", filePath);
        } catch (Exception e) {
            log.error("Excel读取器初始化失败: {}", filePath, e);
            throw new ExcelExportException("Excel读取器初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分页读取数据
     *
     * @param pageNum  页码（从1开始）
     * @param pageSize 页大小
     * @return 数据列表
     */
    public List<T> readPage(int pageNum, int pageSize) {
        try {
            // 跳过表头（如果是第一页且需要跳过表头）
            if (pageNum == 1 && skipHeader && !headerSkipped) {
//                reader.skipRows(1);
                log.debug("已跳过Excel表头");
            }
            // TODO 这里先忽略 计算要跳过的行数
  /*          int skipRows = (pageNum - 1) * pageSize;
            if (headerSkipped && pageNum > 1) {
                // 已经跳过表头，所以从第二页开始需要多跳一行
                skipRows += 1;
            }
            // 跳过行
            if (skipRows > 0) {
                reader.skipRows(skipRows);
            }*/
            ReadSheet readSheet = FastExcelFactory.readSheet(0).head(dataClass).build();
            // 读取数据
//            List<T> data = reader.read(pageSize, dataClass);
            List<T> data = (List<T>) reader.read(readSheet);
            totalRows += data.size();

            log.debug("Excel数据读取成功: 页码={}, 页大小={}, 数据量={}, 累计行数={}",
                    pageNum, pageSize, data.size(), totalRows);

            return data;

        } catch (Exception e) {
            log.error("Excel数据读取失败: 页码={}, 页大小={}", pageNum, pageSize, e);
            throw new ExcelExportException("Excel数据读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取所有数据
     * 注意：仅适用于小数据量，大数据量请使用分页读取
     */
    public void readAll() {
        try {
    /*        List<T> data = reader.readAll();
            totalRows = data.size();

            log.debug("Excel所有数据读取成功: 数据量={}", data.size());

            return data;*/
            reader.readAll();

        } catch (Exception e) {
            log.error("Excel所有数据读取失败", e);
            throw new ExcelExportException("Excel所有数据读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查是否还有更多数据
     *
     * @return 如果还有数据返回true
     */
    public boolean hasMoreData() {
        try {
//            return reader.hasMoreData();
            return true;
        } catch (Exception e) {
            log.error("检查Excel数据状态失败", e);
            return false;
        }
    }

    /**
     * 关闭读取器
     */
    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
                log.info("Excel读取器关闭成功: {}, 总读取行数: {}", filePath, totalRows);
            } catch (Exception e) {
                log.error("关闭Excel读取器失败: {}", filePath, e);
            }
        }
    }

    /**
     * 获取底层FastExcelReader实例
     * 用于高级操作，谨慎使用
     *
     * @return FastExcelReader实例
     */
    public ExcelReader getFastExcelReader() {
        return reader;
    }
}
