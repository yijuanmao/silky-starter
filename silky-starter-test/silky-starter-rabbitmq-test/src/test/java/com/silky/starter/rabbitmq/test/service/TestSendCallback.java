package com.silky.starter.rabbitmq.test.service;

import com.silky.starter.rabbitmq.core.model.SendResult;
import com.silky.starter.rabbitmq.service.SendCallback;
import lombok.extern.slf4j.Slf4j;

/**
 * 测试发送回调
 *
 * @author zy
 * @date 2025-10-16 17:57
 **/
@Slf4j
public class TestSendCallback implements SendCallback {


    /**
     * 发送成功回调
     *
     * @param result
     */
    @Override
    public void onSuccess(SendResult result) {

        log.info("消息发送成功回调，发送结果：{}", result);
    }

    /**
     * 发送失败回调
     *
     * @param result
     */
    @Override
    public void onFailure(SendResult result) {
        log.info("消息发送失败回调，发送结果：{}", result);
    }
}
