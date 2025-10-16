package com.silky.starter.rabbitmq.service;

import com.silky.starter.rabbitmq.core.model.SendResult;

/**
 * 发送回调接口
 *
 * @author zy
 * @date 2025-10-09 17:52
 **/
public interface SendCallback {

    /**
     * 发送成功回调
     *
     * @param result 发送结果
     */
    void onSuccess(SendResult result);

    /**
     * 发送失败回调
     *
     * @param result 发送结果
     */
    void onFailure(SendResult result);
}
