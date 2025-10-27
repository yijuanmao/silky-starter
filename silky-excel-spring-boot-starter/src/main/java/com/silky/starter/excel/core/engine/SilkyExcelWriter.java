package com.silky.starter.excel.core.engine;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.annotation.ExcelProperty;
import com.silky.starter.excel.core.exception.ExcelExportException;
import lombok.Getter;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Excel写入器 封装FastExcelWriter，提供更易用的API和异常处理
 *
 * @author zy
 * @date 2025-10-24 16:01
 **/
@Getter
public class SilkyExcelWriter implements Closeable {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SilkyExcelWriter.class);

    private final ExcelWriter writer;

    /**
     * 获取文件路径
     */
    private final String filePath;

    /**
     * 获取总写入行数
     */
    private long totalRows = 0;

    /**
     * 检查表头是否已写入
     */
    private boolean headerWritten = false;

    /**
     * 构造方法
     *
     * @param filePath Excel文件路径
     */
    public SilkyExcelWriter(String filePath) {
        this.filePath = filePath;

        try {
            this.writer = FastExcelFactory.write(filePath)
                    .build();
//            this.writer = new ExcelWriter(filePath);
            log.info("Excel写入器初始化成功: {}", filePath);
        } catch (Exception e) {
            log.error("Excel写入器初始化失败: {}", filePath, e);
            throw new ExcelExportException("Excel写入器初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入表头
     * 根据数据类的注解自动生成表头，支持自定义表头映射
     *
     * @param data          数据列表（用于推断数据类型）
     * @param clazz         数据类类型
     * @param headerMapping 自定义表头映射（可选）
     * @param <T>           数据类型
     * @throws ExcelExportException 如果表头写入失败
     */
    public <T> void writeHeader(List<T> data, Class<T> clazz, Map<String, String> headerMapping) {
        if (data == null || data.isEmpty()) {
            log.debug("数据为空，跳过表头写入");
            return;
        }

        if (headerWritten) {
            log.debug("表头已写入，跳过重复写入");
            return;
        }

        try {
            // 获取所有字段
            Field[] fields = clazz.getDeclaredFields();
            String[] headers = new String[fields.length];

            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                String headerName = getHeaderName(field, headerMapping);
                headers[i] = headerName;
            }

            // 写入表头
            writer.writeHeader(headers);
            headerWritten = true;

            log.debug("Excel表头写入成功，列数: {}", headers.length);

        } catch (Exception e) {
            log.error("Excel表头写入失败", e);
            throw new ExcelExportException("Excel表头写入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入表头（使用默认表头生成）
     *
     * @param data  数据列表
     * @param clazz 数据类类型
     * @param <T>   数据类型
     */
    public <T> void writeHeader(List<T> data, Class<T> clazz) {
        writeHeader(data, clazz, null);
    }

    /**
     * 写入数据
     * 将数据列表写入Excel文件
     *
     * @param data  数据列表
     * @param clazz 数据类类型
     * @param <T>   数据类型
     * @throws ExcelExportException 如果数据写入失败
     */
    public <T> void write(List<T> data, Class<T> clazz) {
        if (data == null || data.isEmpty()) {
            log.debug("数据为空，跳过写入");
            return;
        }

        try {
            // 如果还没有写入表头，先写入表头
            if (!headerWritten) {
                writeHeader(data, clazz);
            }

            // 写入数据
            writer.write(data, clazz);
            totalRows += data.size();

            log.debug("Excel数据写入成功，数据量: {}, 累计行数: {}", data.size(), totalRows);

        } catch (Exception e) {
            log.error("Excel数据写入失败", e);
            throw new ExcelExportException("Excel数据写入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 写入数据（带自定义表头）
     *
     * @param data    数据列表
     * @param clazz   数据类类型
     * @param headers 自定义表头
     * @param <T>     数据类型
     */
    public <T> void write(List<T> data, Class<T> clazz, String[] headers) {
        if (data == null || data.isEmpty()) {
            log.debug("数据为空，跳过写入");
            return;
        }

        try {
            // 如果还没有写入表头，先写入自定义表头
            if (!headerWritten && headers != null && headers.length > 0) {
                writer.writeHeader(headers);
                headerWritten = true;
                log.debug("自定义Excel表头写入成功，列数: {}", headers.length);
            }

            // 写入数据
            writer.write(data, clazz);
            totalRows += data.size();

            log.debug("Excel数据写入成功，数据量: {}, 累计行数: {}", data.size(), totalRows);

        } catch (Exception e) {
            log.error("Excel数据写入失败", e);
            throw new ExcelExportException("Excel数据写入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 关闭写入器
     * 释放所有资源，必须调用以确保文件正确关闭
     */
    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.close();
                log.info("Excel写入器关闭成功: {}, 总写入行数: {}", filePath, totalRows);
            } catch (Exception e) {
                log.error("关闭Excel写入器失败: {}", filePath, e);
                // 不抛出异常，避免影响主流程
            }
        }
    }

    /**
     * 获取表头名称
     * 优先使用自定义映射，其次使用注解，最后使用字段名
     *
     * @param field         字段
     * @param headerMapping 表头映射
     * @return 表头名称
     */
    private String getHeaderName(Field field, Map<String, String> headerMapping) {
        String fieldName = field.getName();

        // 优先使用自定义映射
        if (headerMapping != null && headerMapping.containsKey(fieldName)) {
            return headerMapping.get(fieldName);
        }

        // 其次使用注解
        ExcelProperty annotation = field.getAnnotation(ExcelProperty.class);
        if (annotation != null && annotation.value().length > 0) {
            return annotation.value()[0];
        }
        // 最后使用字段名
        return fieldName;
    }

}
