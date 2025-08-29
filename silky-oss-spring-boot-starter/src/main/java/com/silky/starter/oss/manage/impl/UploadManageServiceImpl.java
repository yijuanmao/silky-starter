package com.silky.starter.oss.manage.impl;

import com.silky.starter.oss.manage.UploadManageOperations;
import com.silky.starter.oss.model.progress.UploadProgress;

/**
 * 上传管理服务实现类，提供上传相关的管理操作。
 *
 * @author zy
 * @date 2025-08-12 10:08
 **/
public class UploadManageServiceImpl implements UploadManageOperations {

    /**
     * 获取上传进度
     *
     * @param uploadId 上传任务ID
     * @return 上传进度
     */
    @Override
    public UploadProgress getUploadProgress(String uploadId) {
        return null;
    }

    /**
     * 取消上传任务
     *
     * @param uploadId 上传任务ID
     */
    @Override
    public void cancelUpload(String uploadId) {

    }

    /**
     * 清理已完成的上传任务
     *
     * @param olderThanDays 保留天数
     */
    @Override
    public void cleanupCompletedUploads(int olderThanDays) {

    }
}
