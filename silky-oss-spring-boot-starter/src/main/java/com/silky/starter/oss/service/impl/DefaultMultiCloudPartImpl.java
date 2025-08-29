package com.silky.starter.oss.service.impl;

import com.silky.starter.oss.model.param.CalculateOptimalPartSizeParam;
import com.silky.starter.oss.service.strategy.MultiCloudPartStrategy;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 默认的多云分片上传策略实现类
 *
 * @author zy
 * @date 2025-08-14 11:13
 **/
public class DefaultMultiCloudPartImpl implements MultiCloudPartStrategy {

    // 网络速度采样窗口
    private final Deque<Long> networkSpeedSamples = new ArrayDeque<>();

    /**
     * 计算最佳分片大小
     *
     * @param param 请求参数
     * @return 最佳分片大小
     */
    @Override
    public long calculateOptimalPartSize(CalculateOptimalPartSizeParam param) {
        //参数校验
        param.validateCalculateOptimalPart();
        // 1. 根据文件大小和类型计算基础分片
        long partSize = this.calculateBasePartSize(param.getFileSize(), param.getFileType(), param.getBasePartSize());
        // 2. 根据网络状况调整
//        partSize = adjustByNetwork(partSize);
        //考虑平台限制
        partSize = applyPlatformLimits(partSize, param.getPartCountLimit(), param.getFileSize(), param.getMinPartSize());
        return partSize;
    }

    /**
     * 计算基础分片大小
     *
     * @param fileSize     文件大小
     * @param fileType     文件类型
     * @param basePartSize 基础分片大小
     * @return 基础分片大小
     */
    private long calculateBasePartSize(long fileSize, String fileType, long basePartSize) {
        // 基础分片大小
        long partSize = basePartSize;

        // 根据文件大小调整
        if (fileSize > 100 * 1024 * 1024) {
            partSize = 20 * 1024 * 1024; // 20MB
        }
        if (fileSize > 1024 * 1024 * 1024) {
            partSize = 50 * 1024 * 1024; // 50MB
        }
        if (fileSize > 5L * 1024 * 1024 * 1024) {
            partSize = 100 * 1024 * 1024; // 100MB
        }
        // 根据文件类型调整，后期扩展
/*        switch (fileType) {
            case "video":
                partSize = Math.max(partSize, 20 * 1024 * 1024);
                break;
            case "database":
                partSize = Math.min(partSize, 5 * 1024 * 1024);
                break;
        }*/
        return partSize;
    }

    /**
     * 根据网络状况调整分片大小
     *
     * @param partSize 初始分片大小
     * @return 调整后的分片大小
     */
    private long adjustByNetwork(long partSize) {
        // 获取平均网络速度 (KB/s)
        double avgSpeed = networkSpeedSamples.stream()
                .mapToLong(Long::longValue)
                .average()
                // 默认1MB/s
                .orElse(1024);
        // 基准网络速度 (10MB/s)
        final double baseSpeed = 10 * 1024;
        // 计算网络因子 (0.5-2.0)
        double factor = Math.max(0.5, Math.min(2.0, avgSpeed / baseSpeed));
        return (long) (partSize * factor);
    }

    /**
     * 应用平台分片大小限制
     *
     * @param partSize              分片大小
     * @param platformPartSizeLimit 平台分片大小限制
     * @param fileSize              文件大小
     * @return 应用限制后的分片大小
     */
    private long applyPlatformLimits(long partSize, int platformPartSizeLimit, long fileSize, long baseMinPartSize) {
        // 应用平台分片大小限制
        partSize = Math.min(partSize, platformPartSizeLimit);
        // 确保分片数量不超过平台限制
        long minPartSize = (long) Math.ceil((double) fileSize / platformPartSizeLimit);
        // 取两者最大值
        partSize = Math.max(partSize, minPartSize);
        // 确保不小于最小分片
        return Math.max(partSize, baseMinPartSize);
    }
}
