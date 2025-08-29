package com.silky.starter.oss.template;

import com.silky.starter.oss.adapter.OssProviderAdapter;
import com.silky.starter.oss.callback.OssUploadCallback;
import com.silky.starter.oss.core.exception.OssException;
import com.silky.starter.oss.model.check.Checkpoint;
import com.silky.starter.oss.model.metadata.OssFileMetadata;
import com.silky.starter.oss.model.param.*;
import com.silky.starter.oss.model.part.UploadedPart;
import com.silky.starter.oss.model.progress.UploadProgress;
import com.silky.starter.oss.model.result.*;
import com.silky.starter.oss.service.strategy.OssUploadStrategy;

import java.util.List;

/**
 * oss统一上传模板接口，集成了标准上传、断点续传和分片上传的操作。
 *
 * @author zy
 * @date 2025-08-11 15:56
 **/
public interface OssTemplate {

    /**
     * 智能上传，这里会根据文件大小自动切换为分片上传或断点上传
     *
     * @param param 上传上下文，包含必要的上传信息
     * @return OssUploadResult
     */
    OssUploadResult smartUpload(OssUploadParam param) throws OssException;

    /**
     * 下载文件到本地。
     *
     * @param param 请求参数
     */
    void downloadFile(DownloadFileOssParam param) throws OssException;

    /**
     * 删除文件
     *
     * @param param 请求参数
     */
    void deleteFile(DeleteFileOssParam param) throws OssException;

    /**
     * 获取文件元数据。
     *
     * @param param 参数
     * @return 文件元数据
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
     * 带回调的标准上传
     *
     * @param param    请求参数
     * @param callback 上传回调接口
     * @return 上传结果
     */
    OssUploadResult standardUploadWithCallback(OssUploadParam param, OssUploadCallback callback) throws OssException;

    /**
     * 初始化分片上传
     *
     * @param param 参数
     * @return InitiateMultipartResult
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
     * @param param 参数
     * @return 上传结果
     */
    CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadParam param) throws OssException;

    /**
     * 终止分片上传
     *
     * @param param 请求参数
     */
    void abortMultipartUpload(AbortMultipartUploadParam param) throws OssException;

    /**
     * 获取已上传的分片列表
     *
     * @param param 请求参数
     * @return 已上传的分片信息
     */
    List<UploadedPart> listUploadedParts(ListPartsParam param) throws OssException;

    /**
     * 创建断点续传检查点
     *
     * @param param 请求参数
     * @return 检查点信息
     */
    Checkpoint createCheckpoint(OssUploadParam param) throws OssException;

    /**
     * 断点续传上传
     *
     * @param param      请求参数
     * @param checkpoint 检查点信息
     * @return OssUploadResult
     */
    OssUploadResult resumeUpload(OssUploadParam param, Checkpoint checkpoint) throws OssException;

    /**
     * 获取上传进度
     *
     * @param uploadId 上传任务ID
     * @return 上传进度
     */
    UploadProgress getUploadProgress(String uploadId) throws OssException;

    /**
     * 取消上传任务
     *
     * @param uploadId 上传任务ID
     */
    void cancelUpload(String uploadId) throws OssException;

    /**
     * 切换服务提供商
     *
     * @param provider 服务提供商名称, 参照枚举类: {@link com.silky.starter.oss.core.enums.ProviderType}
     */
    void switchProvider(String provider) throws OssException;

    /**
     * 可添加全局配置方法
     *
     * @param providerName 服务提供商名称,参照枚举类:{@link com.silky.starter.oss.core.enums.ProviderType}
     * @param adapter      服务提供商适配器
     */
    void registerProvider(String providerName, OssProviderAdapter adapter) throws OssException;

    /**
     * 设置默认的上传策略
     *
     * @param strategy 上传策略
     */
    void setDefaultUploadStrategy(OssUploadStrategy strategy) throws OssException;
}
