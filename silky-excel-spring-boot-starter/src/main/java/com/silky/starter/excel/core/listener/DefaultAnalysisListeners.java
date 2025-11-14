package com.silky.starter.excel.core.listener;

/**
 * Excel导入默认监听器
 *
 * @author zy
 * @date 2025-11-14 10:00
 **/
public class DefaultAnalysisListeners<T> extends BaseAnalysisListeners<T> {

    /**
     * 构造方法
     *
     * @param maxErrorCount 最大错误数量
     * @param maxReadCount  最大读取数量
     */
    public DefaultAnalysisListeners(Integer maxErrorCount, Integer maxReadCount) {
        super(maxErrorCount, maxReadCount);
    }
}
