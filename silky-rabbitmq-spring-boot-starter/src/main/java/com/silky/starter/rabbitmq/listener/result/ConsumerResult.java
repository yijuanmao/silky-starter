package com.silky.starter.rabbitmq.listener.result;

import lombok.Getter;

/**
 * 消费者结果
 *
 * @author zy
 * @date 2025-10-18 11:11
 **/
@Getter
public class ConsumerResult {

    /**
     * 是否处理成功
     */
    private final boolean success;

    /**
     * 错误信息
     */
    private final String errorMessage;

    /**
     * 是否需要重试
     */
    private final boolean shouldRetry;

    /**
     * 业务处理结果
     */
    private final String businessResult;

    private ConsumerResult(boolean success, String errorMessage, boolean shouldRetry, String businessResult) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.shouldRetry = shouldRetry;
        this.businessResult = businessResult;
    }

    /**
     * 成功结果
     *
     * @return ConsumerResult
     */
    public static ConsumerResult success() {
        return success(null);
    }


    /**
     * 成功结果，包含业务处理结果
     *
     * @param businessResult 业务处理结果
     * @return ConsumerResult
     */
    public static ConsumerResult success(String businessResult) {
        return new ConsumerResult(true, null, false, businessResult);
    }

    /**
     * 失败结果
     *
     * @param errorMessage 错误信息
     * @return ConsumerResult
     */
    public static ConsumerResult failure(String errorMessage) {
        return failure(errorMessage, true);
    }

    /**
     * 失败结果
     *
     * @param errorMessage 错误信息
     * @param shouldRetry  是否需要重试
     * @return ConsumerResult
     */
    public static ConsumerResult failure(String errorMessage, boolean shouldRetry) {
        return failure(errorMessage, shouldRetry);
    }
}
