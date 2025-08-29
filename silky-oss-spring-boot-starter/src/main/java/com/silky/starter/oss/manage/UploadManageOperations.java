package com.silky.starter.oss.manage;

import com.fenmi.starter.oss.model.progress.UploadProgress;

/**
 * 上传管理操作接口，定义了上传相关的管理操作。
 *
 * @author zy
 * @date 2025-08-11 16:13
 **/
public interface UploadManageOperations {

    /**
     * 获取上传进度
     *
     * @param uploadId 上传任务ID
     * @return 上传进度
     */
    UploadProgress getUploadProgress(String uploadId);

    /**
     * 取消上传任务
     *
     * @param uploadId 上传任务ID
     */
    void cancelUpload(String uploadId);

    /**
     * 清理已完成的上传任务
     *
     * @param olderThanDays 保留天数
     */
    void cleanupCompletedUploads(int olderThanDays);
}
