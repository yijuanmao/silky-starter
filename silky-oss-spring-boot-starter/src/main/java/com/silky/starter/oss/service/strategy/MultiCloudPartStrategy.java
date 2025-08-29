package com.silky.starter.oss.service.strategy;

import com.silky.starter.oss.model.param.CalculateOptimalPartSizeParam;

/**
 * 多云分片上传策略接口
 *
 * @author zy
 * @date 2025-08-14 11:07
 **/
public interface MultiCloudPartStrategy {

    /**
     * 计算最佳分片大小
     *
     * @param param 请求参数
     * @return 最佳分片大小
     */
    long calculateOptimalPartSize(CalculateOptimalPartSizeParam param);
}
