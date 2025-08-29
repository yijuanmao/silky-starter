package com.silky.starter.oss.adapter;

import com.silky.starter.oss.core.exception.OssException;
import com.silky.starter.oss.model.check.Checkpoint;
import com.silky.starter.oss.model.config.OssProviderConfig;
import com.silky.starter.oss.model.metadata.OssFileMetadata;
import com.silky.starter.oss.model.param.*;
import com.silky.starter.oss.model.part.UploadedPart;
import com.silky.starter.oss.model.progress.UploadProgress;
import com.silky.starter.oss.model.result.*;

import java.util.List;

/**
 * OSS服务提供商适配器接口
 *
 * @author zy
 * @date 2025-08-11 15:59
 **/
public interface OssProviderAdapter {

    /**
     * 下载文件到本地
     *
     * @param param 下载文件参数
     */
    void download(DownloadFileOssParam param) throws OssException;

    /**
     * 删除指定存储桶中的对象
     *
     * @param param 删除文件参数
     */
    void delete(DeleteFileOssParam param) throws OssException;

    /**
     * 获取文件元数据。
     *
     * @param param 请求参数
     * @return OssFileMetadata
     */
    OssFileMetadata getFileMetadata(GetFileMetadataParam param) throws OssException;

    /**
     * 生成预签名URL
     *
     * @param param 请求参数
     * @return GenPreSignedUrlResult
     */
    GenPreSignedUrlResult genPreSignedUrl(GenPreSignedUrlParam param) throws OssException;

    /**
     * 标准上传
     *
     * @param param 上传上下文，包含必要的上传信息
     * @return OssUploadResult
     */
    OssUploadResult standardUpload(OssUploadParam param) throws OssException;

    /**
     * 分片上传
     *
     * @param param 分片上传参数，包含必要的上传信息
     * @return InitiateMultipartUploadResult
     */
    InitiateMultipartResult initiateMultipartUpload(InitiateMultipartUploadParam param) throws OssException;

    /**
     * 上传分片
     *
     * @param param 请求参数
     * @return UploadPartResult
     */
    UploadPartResult uploadPart(UploadPartParam param) throws OssException;

    /**
     * 完成分片上传
     *
     * @param param 请求参数
     * @return CompleteMultipartUploadResult
     */
    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadParam param) throws OssException;

    /**
     * 放弃分片上传
     *
     * @param param 请求参数
     */
    void abortMultipartUpload(AbortMultipartUploadParam param) throws OssException;

    /**
     * 列出已上传的分片
     *
     * @param param 请求参数
     * @return 已上传的分片列表
     */
    List<UploadedPart> listParts(ListPartsParam param) throws OssException;

    /**
     * 检查当前OSS提供商是否支持断点续传
     *
     * @return true 如果支持断点续传，false 如果不支持
     */
    boolean supportsResumeUpload();

    /**
     * 创建断点续传检查点
     *
     * @param param 上传上下文，包含必要的上传信息
     * @return 创建的断点续传检查点
     */
    Checkpoint createResumableCheckpoint(OssUploadParam param) throws OssException;

    /**
     * 断点续传上传
     *
     * @param param      上传上下文，包含必要的上传信息
     * @param checkpoint 断点续传检查点
     * @return 上传结果
     */
    OssUploadResult resumeUpload(OssUploadParam param, Checkpoint checkpoint) throws OssException;

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
     * 获取提供商名称 ,参照枚举:{@link com.silky.starter.oss.core.enums.ProviderType}
     *
     * @return 提供商名称
     */
    String getProviderName();

    /**
     * 配置提供商
     *
     * @param config 提供商配置
     */
    void configure(OssProviderConfig config);
}
