package com.silky.starter.excel.core.async;

/**
 * 异步任务统一接口
 *
 * @author zy
 * @date 2025-10-27 21:34
 **/
public interface AsyncTask {

    /**
     * 获取任务ID
     */
    String getTaskId();

    /**
     * 获取业务类型
     */
    String getBusinessType();

    /**
     * 验证任务
     */
    void validate();
}
