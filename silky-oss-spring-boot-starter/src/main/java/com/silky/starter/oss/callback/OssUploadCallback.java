package com.silky.starter.oss.callback;

import com.silky.starter.oss.model.param.OssUploadParam;
import com.silky.starter.oss.model.result.OssUploadResult;

/**
 * OSS上传回调接口
 *
 * @author zy
 * @date 2025-08-11 15:22
 **/
public interface OssUploadCallback {

    /**
     * 上传开始时调用
     *
     * @param param 参数
     */
    void onUploadStart(OssUploadParam param);

    /**
     * 上传分片完成时调用
     *
     * @param param      参数
     * @param partNumber 分片编号
     * @param etag       分片的ETag
     */
//    void onPartComplete(OssUploadParam param, int partNumber, String etag);

    /**
     * 上传进度变化时调用
     *
     * @param param    参数
     * @param progress 上传进度，范围0.0到1.0
     */
//    void onProgressChanged(OssUploadParam param, double progress);

    /**
     * 上传完成时调用
     *
     * @param result 上传结果
     */
    void onUploadComplete(OssUploadResult result);

    /**
     * 上传失败时调用
     *
     * @param param 参数
     * @param ex    上传异常
     */
    void onUploadFail(OssUploadParam param, Exception ex);

    /**
     * 上传后清理回调（无论成功失败都会调用）
     *
     * @param param 参数
     */
    default void onUploadFinished(OssUploadParam param) {
    }
}
