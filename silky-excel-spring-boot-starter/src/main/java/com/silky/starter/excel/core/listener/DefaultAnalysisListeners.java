package com.silky.starter.excel.core.listener;

import com.silky.starter.excel.core.model.AnalysisListenersContext;

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
     * @param context 监听器上下文
     */
    public DefaultAnalysisListeners(AnalysisListenersContext<T> context) {
        super(context);
    }
}
