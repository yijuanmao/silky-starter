package com.silky.starter.excel.core.model;

import com.silky.starter.excel.enums.CompressionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 批次任务上下文
 *
 * @author zy
 * @date 2025-11-07 15:59
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatchTask<T> {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * 批次数据
     */
    private List<T> batchData;

    /**
     * 当前批次索引
     */
    private int batchIndex;

    /**
     * 总批次数
     */
    private int totalBatches;

    /**
     * 已处理记录数
     */
    private AtomicLong processedCount;

    /**
     * 成功记录数
     */
    private AtomicLong successCount;

    /**
     * 失败记录数
     */
    private AtomicLong failedCount;

    /**
     * 任务开始时间
     */
    private long startTime;

    /**
     * 是否压缩
     */
    private boolean compressed;

    /**
     * 压缩类型
     */
    private CompressionType compressionType;

    /**
     * 压缩开关
     */
    private boolean compressionEnabled = false;

    /**
     * 压缩级别
     */
    private int compressionLevel = 6;

    /**
     * 分割大文件开关
     */
    private boolean splitLargeFiles = false;

    /**
     * 分割大小
     */
    private long splitSize = 100 * 1024 * 1024;

    public double getProgress() {
        return totalBatches > 0 ? (double) batchIndex / totalBatches * 100 : 0;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}
