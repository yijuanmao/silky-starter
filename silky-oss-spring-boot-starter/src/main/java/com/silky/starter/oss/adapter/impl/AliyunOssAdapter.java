package com.silky.starter.oss.adapter.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.PartListing;
import com.silky.starter.oss.adapter.OssProviderAdapter;
import com.silky.starter.oss.core.enums.ProviderType;
import com.silky.starter.oss.core.exception.OssException;
import com.silky.starter.oss.core.util.aliyun.AliOssUtils;
import com.silky.starter.oss.model.check.Checkpoint;
import com.silky.starter.oss.model.config.OssProviderConfig;
import com.silky.starter.oss.model.metadata.OssFileMetadata;
import com.silky.starter.oss.model.param.*;
import com.silky.starter.oss.model.part.UploadedPart;
import com.silky.starter.oss.model.progress.UploadProgress;
import com.silky.starter.oss.model.result.*;
import com.silky.starter.oss.properties.OssProperties;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 阿里云OSS适配器
 *
 * @author zy
 * @date 2025-08-11 16:21
 **/
public class AliyunOssAdapter implements OssProviderAdapter {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AliyunOssAdapter.class);

    private final OSS ossClient;
    private final OssProperties ossProperties;
    private final OssProviderConfig config;
    private final long multipartPartSize;

    public AliyunOssAdapter(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
        OssProperties.AliyunConfig aliyunConfig = ossProperties.getAliyun();
        if (aliyunConfig == null) {
            throw new IllegalStateException("Aliyun OSS configuration is missing");
        }
        this.multipartPartSize = ossProperties.getMultipartPartSize();
        String endpoint = aliyunConfig.getEndpoint();
        this.ossClient = new OSSClientBuilder().build(
                endpoint,
                ossProperties.getAccessKey(),
                ossProperties.getSecretKey()
        );
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
            AliOssUtils.downloadFileOss(ossClient, config, param);
        } catch (Exception e) {
            log.error("error to download object from OSS,bucketName:" + config.getBucketName() + "\nfilePath:" + param.getObjectKey(), e);
            throw new OssException("Failed to download object from OSS", e);
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
            // 删除文件或目录。如果要删除目录，目录必须为空。
            AliOssUtils.deleteFileOss(ossClient, config, param);
        } catch (Exception e) {
            log.error("error to delete object from OSS,bucketName:" + config.getBucketName() + "\nfilePath:" + param.getObjectKey(), e);
            throw new OssException("Failed to delete object from OSS", e);
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
            return AliOssUtils.getObjectMetadata(ossClient, config, param);
        } catch (Exception e) {
            log.error("error to getObjectMetadata from OSS,bucketName:" + config.getBucketName() + "\nfilePath:" + param.getObjectKey(), e);
            throw new OssException("Failed to getObjectMetadata from OSS", e);
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
            return AliOssUtils.genPreSignedUrl(ossClient, config, param);
        } catch (Exception e) {
            log.error("error to getFileUrl from OSS,GenPreSignedUrlParam:" + param, e);
            throw new OssException("Failed to getFileUrl from OSS", e);
        }
    }

    /**
     * 标准上传
     *
     * @param param 上传上下文，包含必要的上传信息
     * @return String
     */
    @Override
    public OssUploadResult standardUpload(OssUploadParam param) throws OssException {
        try {
            return AliOssUtils.uploadFileOss(ossClient, config, param);
        } catch (Exception e) {
            log.error("error to standardUpload,OssUploadParam:{}", param, e);
            String errMsg = StrUtil.format("error to uploadFileOss,Path:%s\nFileName:%s\nerrMsg:%s", param.getPath(), param.getFileName(), e.getMessage());
            throw new OssException(errMsg, e);
        }
    }


    /**
     * 带回调的标准上传
     *
     * @param param    上传上下文
     * @param callback 上传回调接口
     * @return 上传结果
     */
/*    @Override
    public OssUploadResult standardUploadWithCallback(OssUploadParam param, OssUploadCallback callback) throws OssException {
        try {
            // 开始回调
            if (callback != null) {
                callback.onUploadStart(param);
            }
            //执行上传
            OssUploadResult result = this.standardUpload(param);
            // 成功回调
            if (callback != null) {
                callback.onUploadComplete(result);
            }
            return result;
        } catch (OssException e) {
            // 失败回调
            if (callback != null) {
                callback.onUploadFail(param, e);
            }
            throw e;
        } finally {
            // 清理回调
            if (callback != null) {
                callback.onUploadFinished(param);
            }
        }
    }*/

    /**
     * 分片上传
     *
     * @param param 分片上传参数，包含必要的上传信息
     * @return 上传ID
     */
    @Override
    public InitiateMultipartResult initiateMultipartUpload(InitiateMultipartUploadParam param) throws OssException {
        try {
            InitiateMultipartUploadResult result = AliOssUtils.initiateMultipartUpload(ossClient, config, param);
            return new InitiateMultipartResult(result.getKey(), result.getUploadId());
        } catch (Exception e) {
            log.error("error to initiateMultipartUpload,InitiateMultipartUploadParam:{}:", param, e);
            throw new OssException("Failed to initiate multipart upload", e);
        }
    }

    /**
     * 上传分片
     *
     * @param param 请求参数
     * @return 分片ETag
     */
    @Override
    public UploadPartResult uploadPart(UploadPartParam param) throws OssException {
        try {
            com.aliyun.oss.model.UploadPartResult result = AliOssUtils.uploadPart(ossClient, config, param);
            return new UploadPartResult(result.getPartNumber(), result.getETag());
        } catch (Exception e) {
            log.error("error to uploadPart,bucketName:" + config.getBucketName() + "\nfilePath:" + param.getObjectKey(), e);
            throw new OssException("Failed to upload part", e);
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
            com.aliyun.oss.model.CompleteMultipartUploadResult result = AliOssUtils.completeMultipartUpload(ossClient, config, param);
            String fileUrl = AliOssUtils.generateFileUrl(config.getBucketName(), param.getObjectKey(), config.getEndpoint());
            return new CompleteMultipartUploadResult(param.getUploadId(), fileUrl, result.getETag(), param.getObjectKey());
        } catch (Exception e) {
            log.error("error to completeMultipartUpload,bucketName:" + config.getBucketName() + "\nfilePath:" + param.getObjectKey(), e);
            throw new OssException("Failed to complete multipart upload", e);
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
            AliOssUtils.abortMultipartUpload(ossClient, config, param);
        } catch (Exception e) {
            log.error("error to abortMultipartUpload,bucketName:" + config.getBucketName() + "\nfilePath:" + param.getObjectKey(), e);
            throw new OssException("Failed to abort multipart upload", e);
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
            PartListing partListing = AliOssUtils.listParts(ossClient, config, param);
            List<UploadedPart> uploadedParts = new ArrayList<>(partListing.getParts().size());
            partListing.getParts().forEach(partSummary -> {
                UploadedPart part = new UploadedPart(partSummary.getPartNumber(),
                        partSummary.getETag(),
                        partSummary.getSize(),
                        LocalDateTime.now());
                uploadedParts.add(part);
            });
            return uploadedParts;
        } catch (Exception e) {
            log.error("error to listParts,bucketName:" + config.getBucketName() + "\nfilePath:" + param.getObjectKey(), e);
            throw new OssException("Failed to list parts", e);
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
        throw new UnsupportedOperationException("createResumableCheckpoint is not implemented for AliyunOssAdapter");
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
        String objectKey = null;
        try {
            String uploadId;
            // 初始化或恢复上传
            if (checkpoint == null || StrUtil.isBlank(checkpoint.getUploadId())) {
                InitiateMultipartResult initiated = this.initiateMultipartUpload(new InitiateMultipartUploadParam(param.getPath(), param.getFileName(), param.getMetadata()));
                uploadId = initiated.getUploadId();
                objectKey = initiated.getObjectKey();
                checkpoint = this.createCheckpoint(param, uploadId, objectKey);
            }
            // 上传剩余分片
            for (int partNumber : checkpoint.getRemainingParts()) {
                try (InputStream partStream = getPartStream(param, partNumber)) {
                    UploadPartResult partResult = this.uploadPart(new UploadPartParam(objectKey, checkpoint.getUploadId(), partNumber, partStream, multipartPartSize));
                    checkpoint.addCompletedPart(partNumber, partResult.getEtag());
                }
            }
            // 完成上传
            CompleteMultipartUploadResult uploadResult = this.completeMultipartUpload(new CompleteMultipartUploadParam(objectKey, checkpoint.getUploadId(), checkpoint.getPartETags()));
            return new OssUploadResult(uploadResult.getDownloadUrl(), "", uploadResult.getObjectKey(), true, "上传成功");
        } catch (Exception e) {
            log.error("error to resumeUpload,bucketName:" + config.getBucketName() + "\nfilePath:" + objectKey, e);
            throw new OssException("断点续传失败", e);
        }
    }

    /**
     * 获取上传进度
     *
     * @param uploadId 上传任务ID
     * @return 上传进度
     */
    @Override
    public UploadProgress getUploadProgress(String uploadId) {
        throw new UnsupportedOperationException("getUploadProgress is not implemented for AliyunOssAdapter");
    }

    /**
     * 取消上传任务
     *
     * @param uploadId 上传任务ID
     */
    @Override
    public void cancelUpload(String uploadId) {
        throw new UnsupportedOperationException("cancelUpload is not implemented for AliyunOssAdapter");
    }

    /**
     * 获取提供商名称
     *
     * @return 提供商名称
     */
    @Override
    public String getProviderName() {
        return ProviderType.ALIYUN.getCode();
    }

    /**
     * 配置提供商
     *
     * @param config 提供商配置
     */
    @Override
    public void configure(OssProviderConfig config) {
        // 初始化配置
//        this.currentConfig = ossProperties.getAliyun();
//        initializeClient(currentConfig);
    }

    /**
     * 创建断点续传检查点
     *
     * @param param     上传参数
     * @param uploadId  上传任务ID
     * @param objectKey 对象Key
     */
    private Checkpoint createCheckpoint(OssUploadParam param, String uploadId, String objectKey) {
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setId(UUID.randomUUID().toString());
        checkpoint.setUploadId(uploadId);
        checkpoint.setBucketName(config.getBucketName());
        checkpoint.setObjectKey(objectKey);
        checkpoint.setFilePath(param.getFile().getAbsolutePath());
        checkpoint.setFileSize(param.getFile().length());
        checkpoint.setPartSize(multipartPartSize);
        checkpoint.setProvider(ProviderType.ALIYUN.getCode());
        checkpoint.setCreatedTime(LocalDateTime.now());
        // 初始化分片信息
        int partCount = (int) Math.ceil((double) checkpoint.getFileSize() / checkpoint.getPartSize());
        for (int i = 1; i <= partCount; i++) {
            checkpoint.getRemainingParts().add(i);
        }
        return checkpoint;
    }

    /**
     * 获取分片数据流
     */
    private InputStream getPartStream(OssUploadParam param, int partNumber) throws IOException {
        RandomAccessFile file = new RandomAccessFile(param.getFile(), "r");
        long start = (partNumber - 1L) * multipartPartSize;
        long end = Math.min(start + multipartPartSize, param.getFile().length());
        long length = end - start;

        file.seek(start);
        byte[] buffer = new byte[(int) length];
        file.read(buffer);
        file.close();
        return new ByteArrayInputStream(buffer);
    }
}
