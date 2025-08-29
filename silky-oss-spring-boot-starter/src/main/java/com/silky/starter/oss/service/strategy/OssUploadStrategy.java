package com.silky.starter.oss.service.strategy;

import com.silky.starter.oss.core.exception.OssException;
import com.silky.starter.oss.model.param.OssUploadParam;
import com.silky.starter.oss.model.result.OssUploadResult;
import com.silky.starter.oss.template.OssTemplate;

/**
 * OSS上传策略接口
 *
 * @author zy
 * @date 2025-08-11 16:09
 **/
public interface OssUploadStrategy {

    /**
     * 判断当前上传策略是否支持指定的上传上下文
     *
     * @param context 上传上下文
     * @return true 如果支持，false 如果不支持
     */
    boolean supports(OssUploadParam context);

    /**
     * 执行上传操作
     *
     * @param template 统一OSS模板，提供上传操作的具体实现
     * @param context  上传上下文，包含必要的上传信息，如文件路径、存储桶等
     * @return OssUploadResult
     */
    OssUploadResult upload(OssTemplate template, OssUploadParam context) throws OssException;
}
