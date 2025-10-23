package com.silky.starter.redis.test.ratelimit.response;

import lombok.Data;

/**
 * API响应类
 *
 * @author zy
 * @date 2025-10-23 16:15
 **/
@Data
public class ApiResponse<T> {

    /**
     * 状态码
     */
    public static final String CODE_TAG = "code";

    /**
     * 返回内容
     */
    public static final String MSG_TAG = "msg";

    /**
     * 数据对象
     */
    public static final String DATA_TAG = "data";

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 返回内容
     */
    private String msg;

    /**
     * 数据对象
     */
    private T data;

    /**
     * 返回结果
     */
    private boolean success;

    /**
     * 初始化一个新创建的 ApiResponse 对象，使其表示一个空消息。
     */
    public ApiResponse() {
    }

    public ApiResponse(T data) {
        this.data = data;
    }

    public ApiResponse(Integer code, String msg, boolean success) {
        this.code = code;
        this.msg = msg;
        this.success = success;
    }

    public ApiResponse(Integer code, String msg, T data, boolean success) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.success = success;
    }

    /**
     * 操作失败
     *
     * @return
     */
    public static <T> ApiResponse<T> fail(Integer code, String msg) {
        return new ApiResponse<T>(code, msg, null, false);
    }

    /**
     * 返回成功消息
     *
     * @return 成功消息
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<T>(200, "操作成功", null, true);
    }

    /**
     * 返回成功数据
     *
     * @return 成功消息
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(200, "操作成功", data, true);
    }

    /**
     * 成功请求（有消息）
     *
     * @param msg
     * @return
     */
    public static <T> ApiResponse<T> successMsg(String msg) {
        return new ApiResponse<T>(200, msg, null, true);
    }

    /**
     * 成功请求（有消息）
     *
     * @param
     * @return
     */
    public static <T> ApiResponse<T> successMsg(T data, String msg) {
        return new ApiResponse<T>(200, msg, data, true);
    }


    /**
     * 操作失败
     *
     * @return
     */
    public static <T> ApiResponse<T> fail() {
        return new <T>ApiResponse<T>(500, "操作失败", false);
    }

    /**
     * 操作失败
     *
     * @return
     */
    public static <T> ApiResponse<T> fail(T data) {
        return new ApiResponse<T>(500, "操作失败", data, false);
    }

}
