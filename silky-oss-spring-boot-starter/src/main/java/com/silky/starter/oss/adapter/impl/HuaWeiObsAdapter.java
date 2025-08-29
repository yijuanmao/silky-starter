package com.silky.starter.oss.adapter.impl;

import cn.hutool.core.date.DateUtil;
import com.obs.services.ObsClient;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListPartsResult;
import com.silky.starter.oss.adapter.OssProviderAdapter;
import com.silky.starter.oss.core.enums.ProviderType;
import com.silky.starter.oss.core.exception.OssException;
import com.silky.starter.oss.core.util.huawei.HuaWeiObsUtils;
import com.silky.starter.oss.model.check.Checkpoint;
import com.silky.starter.oss.model.config.OssProviderConfig;
import com.silky.starter.oss.model.metadata.OssFileMetadata;
import com.silky.starter.oss.model.param.*;
import com.silky.starter.oss.model.part.UploadedPart;
import com.silky.starter.oss.model.progress.UploadProgress;
import com.silky.starter.oss.model.result.*;
import com.silky.starter.oss.properties.OssProperties;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 华为OBS适配器
 *
 * @author zy
 * @date 2025-08-15 11:45
 **/
public class HuaWeiObsAdapter implements OssProviderAdapter {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HuaWeiObsAdapter.class);

    private final ObsClient oobClient;
    private final OssProperties ossProperties;
    private final OssProviderConfig config;

    public HuaWeiObsAdapter(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
        OssProperties.HuaWeiConfig huaWeiConfig = ossProperties.getHuaWei();
        if (huaWeiConfig == null) {
            throw new IllegalStateException("Huawei OBS configuration is missing");
        }
        String endpoint = huaWeiConfig.getEndpoint();
        this.oobClient = new ObsClient(ossProperties.getAccessKey(), ossProperties.getSecretKey(), endpoint);
        this.config = new OssProviderConfig(endpoint,
                ossProperties.getAccessKey(),
                ossProperties.getSecretKey(),
                ossProperties.getBucket()
        );
    }

    /**
     * 下载文件到本地
     *
     * @param param 下载文件参数
     */
    @Override
    public void download(DownloadFileOssParam param) throws OssException {
        try {
            HuaWeiObsUtils.downloadFileOss(oobClient, config, param);
        } catch (Exception e) {
            log.error("下载文件失败: {}", param, e);
            throw new OssException("下载文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除指定存储桶中的对象
     *
     * @param param 删除文件参数
     */
    @Override
    public void delete(DeleteFileOssParam param) throws OssException {
        try {
            HuaWeiObsUtils.deleteFileOss(oobClient, config, param);
        } catch (Exception e) {
            log.error("删除文件失败: {}", param, e);
            throw new OssException("删除文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件元数据。
     *
     * @param param 请求参数
     * @return OssFileMetadata
     */
    @Override
    public OssFileMetadata getFileMetadata(GetFileMetadataParam param) throws OssException {
        try {
            return HuaWeiObsUtils.getObjectMetadata(oobClient, config, param);
        } catch (Exception e) {
            log.error("获取文件元数据失败: {}", param, e);
            throw new OssException("获取文件元数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成预签名URL
     *
     * @param param 请求参数
     * @return GenPreSignedUrlResult
     */
    @Override
    public GenPreSignedUrlResult genPreSignedUrl(GenPreSignedUrlParam param) throws OssException {
        try {
            return HuaWeiObsUtils.genPreSignedUrl(oobClient, config, param);
        } catch (Exception e) {
            log.error("生成预签名URL失败: {}", param, e);
            throw new OssException("生成预签名URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 标准上传
     *
     * @param param 上传上下文，包含必要的上传信息
     * @return OssUploadResult
     */
    @Override
    public OssUploadResult standardUpload(OssUploadParam param) throws OssException {
        try {
            return HuaWeiObsUtils.uploadFile(oobClient, config, param);
        } catch (Exception e) {
            log.error("华为OBS-标准上传失败: {}", param, e);
            throw new OssException("标准上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分片上传
     *
     * @param param 分片上传参数，包含必要的上传信息
     * @return InitiateMultipartUploadResult
     */
    @Override
    public InitiateMultipartResult initiateMultipartUpload(InitiateMultipartUploadParam param) throws OssException {
        try {
            InitiateMultipartUploadResult uploadResult = HuaWeiObsUtils.initiateMultipartUpload(oobClient, config, param);
            return new InitiateMultipartResult(uploadResult.getObjectKey(), uploadResult.getUploadId());
        } catch (Exception e) {
            log.error("华为OBS-分片上传初始化失败: {}", param, e);
            throw new OssException("分片上传初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传分片
     *
     * @param param 请求参数
     * @return UploadPartResult
     */
    @Override
    public UploadPartResult uploadPart(UploadPartParam param) throws OssException {
        try {
            com.obs.services.model.UploadPartResult partResult = HuaWeiObsUtils.uploadPart(oobClient, config, param);
            return new UploadPartResult(partResult.getPartNumber(), partResult.getEtag());
        } catch (Exception e) {
            log.error("华为OBS-上传分片失败: {}", param, e);
            throw new OssException("上传分片失败: " + e.getMessage(), e);
        }
    }

    /**
     * 完成分片上传
     *
     * @param param 请求参数
     * @return CompleteMultipartUploadResult
     */
    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadParam param) throws OssException {
        try {
            com.obs.services.model.CompleteMultipartUploadResult result = HuaWeiObsUtils.completeMultipartUpload(oobClient, config, param);

            return new CompleteMultipartUploadResult(param.getUploadId(), result.getObjectUrl(), result.getEtag(), result.getObjectKey());
        } catch (Exception e) {
            log.error("华为OBS-完成分片上传失败: {}", param, e);
            throw new OssException("完成分片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 放弃分片上传
     *
     * @param param 请求参数
     */
    @Override
    public void abortMultipartUpload(AbortMultipartUploadParam param) throws OssException {
        try {
            HuaWeiObsUtils.abortMultipartUpload(oobClient, config, param);
        } catch (Exception e) {
            log.error("华为OBS-放弃分片上传失败: {}", e.getMessage(), e);
            throw new OssException("放弃分片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列出已上传的分片
     *
     * @param param 请求参数
     * @return 已上传的分片列表
     */
    @Override
    public List<UploadedPart> listParts(ListPartsParam param) throws OssException {
        try {
            ListPartsResult partsResult = HuaWeiObsUtils.listParts(oobClient, config, param);
            List<UploadedPart> uploadedParts = new ArrayList<>(partsResult.getMultipartList().size());
            partsResult.getMultipartList().forEach(partSummary -> {
                UploadedPart part = new UploadedPart(partSummary.getPartNumber(),
                        partSummary.getEtag(),
                        partSummary.getSize(),
                        DateUtil.toLocalDateTime(partSummary.getLastModified()));
                uploadedParts.add(part);
            });
            return uploadedParts;
        } catch (Exception e) {
            throw new OssException("华为OBS-列出已上传的分片失败: " + param, e);
        }
    }

    /**
     * 检查当前OSS提供商是否支持断点续传
     *
     * @return true 如果支持断点续传，false 如果不支持
     */
    @Override
    public boolean supportsResumeUpload() {
        return true;
    }

    /**
     * 创建断点续传检查点
     *
     * @param param 上传上下文，包含必要的上传信息
     * @return 创建的断点续传检查点
     */
    @Override
    public Checkpoint createResumableCheckpoint(OssUploadParam param) throws OssException {
        return null;
    }

    /**
     * 断点续传上传
     *
     * @param param      上传上下文，包含必要的上传信息
     * @param checkpoint 断点续传检查点
     * @return 上传结果
     */
    @Override
    public OssUploadResult resumeUpload(OssUploadParam param, Checkpoint checkpoint) throws OssException {
        return null;
    }

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
     * 获取提供商名称 ,参照枚举:{@link ProviderType}
     *
     * @return 提供商名称
     */
    @Override
    public String getProviderName() {
        return ProviderType.HUAWEI.getCode();
    }

    /**
     * 配置提供商
     *
     * @param config 提供商配置
     */
    @Override
    public void configure(OssProviderConfig config) {

    }
}
