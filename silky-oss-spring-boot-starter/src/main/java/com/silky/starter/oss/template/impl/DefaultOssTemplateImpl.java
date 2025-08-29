package com.silky.starter.oss.template.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.silky.starter.core.LimitedInputStream;
import com.silky.starter.oss.adapter.OssProviderAdapter;
import com.silky.starter.oss.adapter.registry.OssAdapterRegistry;
import com.silky.starter.oss.callback.OssUploadCallback;
import com.silky.starter.oss.core.enums.ProviderType;
import com.silky.starter.oss.core.exception.OssException;
import com.silky.starter.oss.core.util.OssFileUtil;
import com.silky.starter.oss.model.check.Checkpoint;
import com.silky.starter.oss.model.metadata.OssFileMetadata;
import com.silky.starter.oss.model.param.*;
import com.silky.starter.oss.model.part.UploadedPart;
import com.silky.starter.oss.model.progress.UploadProgress;
import com.silky.starter.oss.model.result.*;
import com.silky.starter.oss.properties.OssProperties;
import com.silky.starter.oss.service.strategy.MultiCloudPartStrategy;
import com.silky.starter.oss.service.strategy.OssUploadStrategy;
import com.silky.starter.oss.template.OssTemplate;
import com.silky.starter.oss.thread.ThreadPoolManager;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 默认的OSS统一上传模板实现类
 *
 * @author zy
 * @date 2025-08-11 16:11
 **/
public class DefaultOssTemplateImpl implements OssTemplate {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultOssTemplateImpl.class);

    private OssProviderAdapter adapter;

    private OssProperties properties;

    private final Map<String, OssProviderAdapter> adapters;

    private String currentProvider;

    private final MultiCloudPartStrategy partitionStrategy;

    private final ThreadPoolManager threadPoolManager;

    private final long multipartThreshold;

    private final long multipartPartSize;

    public DefaultOssTemplateImpl(OssAdapterRegistry registry,
                                  OssProperties properties,
                                  MultiCloudPartStrategy partitionStrategy,
                                  ThreadPoolManager threadPoolManager) {
        this.properties = properties;
        String defaultProvider = properties.getProvider();
        this.adapter = registry.getAdapter(defaultProvider);
        if (this.adapter == null) {
            throw new IllegalArgumentException("No adapter found for provider: " + defaultProvider);
        }
        this.adapters = registry.getAllAdapters();
        this.currentProvider = defaultProvider;
        this.partitionStrategy = partitionStrategy;
        this.threadPoolManager = threadPoolManager;
        this.multipartThreshold = properties.getMultipartThreshold();
        this.multipartPartSize = properties.getMultipartPartSize();
    }

    /**
     * 智能上传，这里会根据文件大小自动切换为分片上传或断点上传
     *
     * @param param 上传上下文，包含必要的上传信息
     * @return OssUploadResult
     */
    @Override
    public OssUploadResult smartUpload(OssUploadParam param) throws OssException {
        //参数校验
        param.validateForUploadParam();
        InputStream stream = Objects.isNull(param.getStream()) ? FileUtil.getInputStream(param.getFile()) : param.getStream();

        long fileSize;
        if (Objects.isNull(param.getFileSize()) || param.getFileSize() <= 0) {
            fileSize = this.getFileSize(param.getFile(), stream);
            param.setFileSize(fileSize);
        } else {
            fileSize = param.getFileSize();
        }
        boolean isMultipartUpload = fileSize >= multipartThreshold;
        if (isMultipartUpload) {
            try {
                return handleMultipartUpload(param, stream, fileSize);
            } finally {
                // 确保流关闭（FileChannel 在 try-with-resources 中已关闭，但原始流可能需要关闭）
                IoUtil.close(stream);
            }
        } else {
            // 执行标准文件上传
            return adapter.standardUpload(param);
        }
    }

    /**
     * 下载文件到本地。
     *
     * @param param 请求参数
     */
    @Override
    public void downloadFile(DownloadFileOssParam param) throws OssException {
        param.validateParam();
        adapter.download(param);
    }

    /**
     * 删除文件
     *
     * @param param 请求参数
     */
    @Override
    public void deleteFile(DeleteFileOssParam param) throws OssException {
        param.validateParam();
        adapter.delete(param);
    }

    /**
     * 获取文件元数据。
     *
     * @param param 参数
     * @return 文件元数据
     */
    @Override
    public OssFileMetadata getFileMetadata(GetFileMetadataParam param) throws OssException {
        param.validateParam();
        return adapter.getFileMetadata(param);
    }

    /**
     * 生成预签名URL
     *
     * @param param 请求参数
     * @return 预签名URL
     */
    @Override
    public GenPreSignedUrlResult genPreSignedUrl(GenPreSignedUrlParam param) throws OssException {
        param.validateParam();
        return adapter.genPreSignedUrl(param);
    }

    /**
     * 带回调的标准上传
     *
     * @param param    请求参数
     * @param callback 上传回调接口
     * @return 上传结果
     */
    @Override
    public OssUploadResult standardUploadWithCallback(OssUploadParam param, OssUploadCallback callback) throws OssException {
        param.validateForUploadParam();
        try {
            // 开始回调
            if (callback != null) {
                callback.onUploadStart(param);
            }
            //执行上传
            OssUploadResult result = this.smartUpload(param);
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
    }

    /**
     * 初始化分片上传
     *
     * @param param 参数
     * @return InitiateMultipartResult
     */
    @Override
    public InitiateMultipartResult initiateMultipartUpload(InitiateMultipartUploadParam param) throws OssException {
        return adapter.initiateMultipartUpload(param);
    }

    /**
     * 上传分片
     *
     * @param param 请求参数
     * @return UploadPartResult
     */
    @Override
    public UploadPartResult uploadPart(UploadPartParam param) throws OssException {
        param.validateParam();
        return adapter.uploadPart(param);
    }

    /**
     * 完成分片上传
     *
     * @param param 参数
     * @return 上传结果
     */
    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadParam param) throws OssException {
        param.validateParam();
        return adapter.completeMultipartUpload(param);
    }

    /**
     * 终止分片上传
     *
     * @param param 请求参数
     */
    @Override
    public void abortMultipartUpload(AbortMultipartUploadParam param) throws OssException {
        param.validateParam();
        adapter.abortMultipartUpload(param);
    }

    /**
     * 获取已上传的分片列表
     *
     * @param param 请求参数
     * @return 已上传的分片信息
     */
    @Override
    public List<UploadedPart> listUploadedParts(ListPartsParam param) throws OssException {
        param.validateParam();
        return adapter.listParts(param);
    }

    /**
     * 创建断点续传检查点
     *
     * @param param 请求参数
     * @return 检查点信息
     */
    @Override
    public Checkpoint createCheckpoint(OssUploadParam param) throws OssException {
        param.validateForUploadParam();
        return adapter.createResumableCheckpoint(param);
    }

    /**
     * 断点续传上传
     *
     * @param param      请求参数
     * @param checkpoint 检查点信息
     * @return OssUploadResult
     */
    @Override
    public OssUploadResult resumeUpload(OssUploadParam param, Checkpoint checkpoint) throws OssException {
        param.validateForUploadParam();
        // 检查是否支持断点续传
        if (!adapter.supportsResumeUpload()) {
            throw new UnsupportedOperationException("当前服务商(" + adapter.getProviderName() + ")不支持断点续传功能");
        }
        return adapter.resumeUpload(param, checkpoint);
    }

    /**
     * 可添加全局配置方法
     *
     * @param providerName 服务提供商名称,参照枚举类:{@link ProviderType}
     * @param adapter      服务提供商适配器
     */
    @Override
    public void registerProvider(String providerName, OssProviderAdapter adapter) throws OssException {

    }

    /**
     * 设置默认的上传策略
     *
     * @param strategy 上传策略
     */
    @Override
    public void setDefaultUploadStrategy(OssUploadStrategy strategy) throws OssException {

    }

    /**
     * 获取上传进度
     *
     * @param uploadId 上传任务ID
     * @return 上传进度
     */
    @Override
    public UploadProgress getUploadProgress(String uploadId) throws OssException {
        return adapter.getUploadProgress(uploadId);
    }

    /**
     * 取消上传任务
     *
     * @param uploadId 上传任务ID
     */
    @Override
    public void cancelUpload(String uploadId) throws OssException {
        adapter.cancelUpload(uploadId);
    }

    /**
     * 切换服务提供商
     *
     * @param provider 服务提供商名称, 参照枚举类: {@link ProviderType}
     */
    @Override
    public void switchProvider(String provider) throws OssException {
        OssProviderAdapter targetAdapter = adapters.get(provider);
        if (targetAdapter == null) {
            throw new OssException("Aliyun Unsupported provider: " + provider);
        }
        this.currentProvider = provider;
        this.adapter = targetAdapter;
    }

    /**
     * 创建文件通道
     */
    private FileChannel createFileChannel(InputStream input) throws IOException {
        // 创建临时文件
        Path tempFile = Files.createTempFile("oss-upload-", ".tmp");
        try (OutputStream out = Files.newOutputStream(tempFile, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        // 返回只读的FileChannel
        return FileChannel.open(tempFile, StandardOpenOption.READ);
    }

    /**
     * 处理分片上传
     *
     * @param param    上传参数
     * @param stream   输入流
     * @param fileSize 文件大小
     * @return OssUploadResult
     */
    private OssUploadResult handleMultipartUpload(OssUploadParam param, InputStream stream, long fileSize) throws OssException {
        // 计算最优分片大小
        CalculateOptimalPartSizeParam partSizeParam = new CalculateOptimalPartSizeParam(fileSize, param.getContentType(), adapter);
        if (multipartPartSize > 0) {
            partSizeParam.setBasePartSize(multipartPartSize);
        }
        long partSize = partitionStrategy.calculateOptimalPartSize(partSizeParam);

        String objectKey = OssFileUtil.buildObjectKey(param.getPath(), param.getFileName());
        // 初始化分片上传
        String uploadId = this.initiateMultipartUpload(new InitiateMultipartUploadParam(param.getPath(), param.getFileName(), param.getMetadata())).getUploadId();
        try (FileChannel channel = createFileChannel(stream)) {
            int partCount = (int) Math.ceil((double) fileSize / partSize);
            List<Future<UploadPartResult>> futures = new ArrayList<>(partCount);
            Map<Integer, String> partETags = new ConcurrentHashMap<>(partCount + 2);

            // 并发上传分片
            for (int partNumber = 1; partNumber <= partCount; partNumber++) {
                final int currentPart = partNumber;
                final long position = (currentPart - 1L) * partSize;
                final long actualSize = Math.min(partSize, fileSize - position);
                Future<UploadPartResult> future = threadPoolManager.submitTask(() -> {
                    try (InputStream partInputStream = new LimitedInputStream(Channels.newInputStream(channel), position, actualSize)) {
                        return this.uploadPart(new UploadPartParam(objectKey, uploadId, currentPart, partInputStream, actualSize));
                    } catch (Exception e) {
                        log.error("分片上传失败, 分片: {}, objectKey: {}", currentPart, objectKey, e);
                        throw e;
                    }
                });
                futures.add(future);
            }

            // 等待所有任务完成，如有异常会抛出
            for (Future<UploadPartResult> future : futures) {
                try {
                    UploadPartResult result = future.get();
                    partETags.put(result.getPartNumber(), result.getEtag());
                } catch (ExecutionException e) {
                    // 取消所有未完成任务
                    futures.forEach(f -> f.cancel(true));
                    throw new OssException("分片上传失败: " + e.getCause().getMessage(), e.getCause());
                }
            }

            // 完成上传
            CompleteMultipartUploadResult uploadResult = this.completeMultipartUpload(new CompleteMultipartUploadParam(objectKey, uploadId, partETags));
            return new OssUploadResult(uploadResult.getDownloadUrl(), "", objectKey, true, "上传成功", LocalDateTime.now());

        } catch (Exception e) {
            log.error("智能上传文件失败, objectKey: {}, 错误: {}", objectKey, e.getMessage(), e);
            try {
                this.abortMultipartUpload(new AbortMultipartUploadParam(objectKey, uploadId));
            } catch (Exception abortEx) {
                log.error("中止分片上传失败, objectKey: {}", objectKey, abortEx);
            }
            throw new OssException("智能上传文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件大小
     *
     * @param file   文件
     * @param stream 输入流
     * @return 文件大小
     */
    private long getFileSize(File file, InputStream stream) {
        long fileSize;
        try {
            if (file != null) {
                fileSize = file.length();
            } else {
                fileSize = stream.available();
            }
        } catch (IOException e) {
            throw new OssException("无法获取流的大小", e);
        }
        return fileSize;
    }

}
