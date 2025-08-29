package com.silky.starter.oss.core.util.aliyun;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import com.silky.starter.oss.core.constant.OssConstants;
import com.silky.starter.oss.core.enums.OssFileTypeEnum;
import com.silky.starter.oss.core.exception.OssException;
import com.silky.starter.oss.core.util.OssFileUtil;
import com.silky.starter.oss.model.config.OssProviderConfig;
import com.silky.starter.oss.model.metadata.OssFileMetadata;
import com.silky.starter.oss.model.param.*;
import com.silky.starter.oss.model.result.GenPreSignedUrlResult;
import com.silky.starter.oss.model.result.OssUploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 阿里oss上传服务
 *
 * @author zy
 * @date 2025-04-18 17:57
 **/
public class AliOssUtils {

    private final static Logger log = LoggerFactory.getLogger(AliOssUtils.class);

    private static final String FILE_URL_FORMAT = OssConstants.HTTPS + "%s.%s/%s";

    /**
     * 简单上传文件
     *
     * @param ossClient oss连接
     * @param config    配置
     * @param param     上下文
     * @return OssUploadResult
     */
    public static OssUploadResult uploadFileOss(OSS ossClient, OssProviderConfig config, OssUploadParam param) {
        InputStream is = param.getStream();
        if (Objects.isNull(is)) {
            is = FileUtil.getInputStream(param.getFile());
        }
        try (InputStream safeIs = is) {
            String fileName = param.getFileName();
            long fileSize = Objects.isNull(param.getFileSize()) ? safeIs.available() : param.getFileSize();
//            String fileName = IdUtil.fastSimpleUUID() + OssConstants.DOT + FileNameUtil.getSuffix(param.getFile());
//            String suffix = FileTypeUtil.getType(safeIs); // 使用内容检测防止后缀伪造
//            String fileName = IdUtil.fastSimpleUUID() + OssConstants.DOT + FileTypeUtil.getType(safeIs, true);
            String ossPath = OssFileUtil.buildObjectKey(param.getPath(), param.getFileName());
            LocalDateTime expiration = param.getExpiration();
            String bucketName = config.getBucketName();
            String endpoint = config.getEndpoint();
            // 创建上传Object的Metadata
            ObjectMetadata metadata = setObjectMetadata(fileName, param, fileSize);
            PutObjectResult putResult = ossClient.putObject(config.getBucketName(), ossPath, safeIs, metadata);

            // 生成文件URL
            String fileUri;
//            String fileUri = generateFileUrl(bucketName, ossPath, endpoint);
            if (Objects.isNull(expiration)) {
                fileUri = generateFileUrl(bucketName, ossPath, endpoint);
            } else {
                fileUri = genPreSignedUrl(ossClient, config, new GenPreSignedUrlParam(ossPath, expiration)).getSignedUrl();
            }
            // 构建结果
            OssUploadResult result = new OssUploadResult(fileUri, fileName, ossPath, true, "上传成功");
            result.setUploadId(putResult.getRequestId());
            result.setMetadata(convertToOssFileMetadata(bucketName, ossPath, metadata));
            result.setFileSize(fileSize);
            result.setCompletionTime(LocalDateTime.now());
            return result;
        } catch (Exception e) {
            log.error("阿里云oss上传文件失败,请求参数:{}", param, e);
            throw new OssException(e.getMessage(), e);
        }
    }

    /**
     * 获取上传文件对象
     * 备注：最重要的是获取上传文件的输出流InputStream
     *
     * @param ossClient  oss连接
     * @param bucketName 存储空间
     * @param key        Bucket下的文件的路径名+文件名 如："upload/2023/01/11/cake.jpg"
     * @return OSSObject
     */
    public static OSSObject getObject(OSS ossClient, String bucketName, String key) {
        OSSObject object = null;
        try {
            object = ossClient.getObject(bucketName, key);
            //文件大小
            long fileSize = object.getObjectMetadata().getContentLength();
            //文件相对路径
            String ossPath = object.getKey();
            //文件输入流
            InputStream is = object.getObjectContent();
            log.info("success to getObject,fileSize:" + fileSize + "\nossPath:" + ossPath + "\ninputStream:" + is);
        } catch (OSSException | ClientException e) {
            log.error("error to getObject,bucketName:" + bucketName + "\nkey:" + key, e);
        }
        return object;
    }

    /**
     * 生成预签名
     *
     * @param ossClient oss连接
     * @param config    配置
     * @param param     请求参数
     * @return GenPreSignedUrlResult
     */
    public static GenPreSignedUrlResult genPreSignedUrl(OSS ossClient, OssProviderConfig config, GenPreSignedUrlParam param) {
        LocalDateTime expiration = param.getExpiration();
        String bucketName = config.getBucketName();
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, param.getObjectKey());
        Date expirationDate;
        if (Objects.isNull(expiration)) {
            //设置URl过期时间为99年：3600L*1000*24*365*99
            expirationDate = new Date(new Date().getTime() + 3600L * 1000 * 24 * 365 * 99);
        } else {
            expirationDate = DateUtil.parse(LocalDateTimeUtil.format(expiration, DatePattern.NORM_DATETIME_PATTERN));
        }
        generatePresignedUrlRequest.setExpiration(expirationDate);
        URL url = ossClient.generatePresignedUrl(generatePresignedUrlRequest);
        String fileUrl = url.toString(); // 获取文件
        if (bucketIsPublicRead(ossClient, bucketName)) {
            //bucket为公共读的情况
            fileUrl = fileUrl.substring(0, fileUrl.indexOf("?"));
        }
        return new GenPreSignedUrlResult(fileUrl);
    }

    /**
     * 下载文件到本地
     *
     * @param ossClient oss连接
     * @param config    配置
     * @param param     参数
     */
    public static void downloadFileOss(OSS ossClient, OssProviderConfig config, DownloadFileOssParam param) {
        GetObjectRequest objectRequest = new GetObjectRequest(config.getBucketName(), param.getObjectKey());
        ossClient.getObject(objectRequest, new File(param.getLocalFilePath()));
    }

    /**
     * 删除oss上的文件
     *
     * @param ossClient oss连接
     * @param config    配置
     * @param param     参数
     */
    public static void deleteFileOss(OSS ossClient, OssProviderConfig config, DeleteFileOssParam param) {
        ossClient.deleteObject(config.getBucketName(), param.getObjectKey());
    }

    /**
     * 发起分片上传
     *
     * @param ossClient oss连接
     * @param config    配置
     * @param param     分片上传参数，包含必要的上传信息
     */
    public static InitiateMultipartUploadResult initiateMultipartUpload(OSS ossClient, OssProviderConfig config, InitiateMultipartUploadParam param) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(config.getBucketName(), OssFileUtil.buildObjectKey(param.getPath(), param.getFileName()));
        if (CollUtil.isNotEmpty(param.getMetadata())) {
            // 设置自定义元数据
            ObjectMetadata metadata = new ObjectMetadata();
            param.getMetadata().forEach(metadata::setHeader);
            request.setObjectMetadata(metadata);
        }
        return ossClient.initiateMultipartUpload(request);
    }

    /**
     * 上传分片
     *
     * @param ossClient oss连接
     * @param config    配置
     * @param param     请求参数
     * @return UploadPartResult
     */
    public static UploadPartResult uploadPart(OSS ossClient, OssProviderConfig config, UploadPartParam param) {
        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(config.getBucketName());
        request.setKey(param.getObjectKey());
        request.setUploadId(param.getUploadId());
        request.setPartNumber(param.getPartNumber());
        request.setInputStream(param.getInputStream());
        request.setPartSize(param.getPartSize());
        return ossClient.uploadPart(request);
    }

    /**
     * 完成分片上传
     *
     * @param ossClient oss连接
     * @param config    配置
     * @param param     请求参数
     * @return CompleteMultipartUploadResult
     */
    public static CompleteMultipartUploadResult completeMultipartUpload(OSS ossClient, OssProviderConfig config, CompleteMultipartUploadParam param) {
        // 转换ETag格式
        List<PartETag> eTags = param.getPartETags().entrySet().stream()
                .map(entry -> new PartETag(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return ossClient.completeMultipartUpload(
                new CompleteMultipartUploadRequest(
                        config.getBucketName(),
                        param.getObjectKey(),
                        param.getUploadId(),
                        eTags)
        );
    }

    /**
     * 获取文件元信息
     *
     * @param ossClient oss连接
     * @param config    配置
     * @param param     请求参数
     * @return ObjectMetadata
     */
    public static OssFileMetadata getObjectMetadata(OSS ossClient, OssProviderConfig config, GetFileMetadataParam param) {
        ObjectMetadata metadata = ossClient.getObjectMetadata(config.getBucketName(), param.getObjectKey());
        return convertToOssFileMetadata(config.getBucketName(), param.getObjectKey(), metadata);
    }

    /**
     * 取消分片上传
     *
     * @param ossClient oss连接
     * @param config    配置
     * @param param     请求参数
     */
    public static void abortMultipartUpload(OSS ossClient, OssProviderConfig config, AbortMultipartUploadParam param) {
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(
                config.getBucketName(),
                param.getObjectKey(),
                param.getUploadId()
        );
        ossClient.abortMultipartUpload(request);
    }

    /**
     * 列出已上传的分片
     *
     * @param ossClient oss连接
     * @param config    配置
     * @param param     请求参数
     */
    public static PartListing listParts(OSS ossClient, OssProviderConfig config, ListPartsParam param) {
        ListPartsRequest request = new ListPartsRequest(
                config.getBucketName(),
                param.getObjectKey(),
                param.getUploadId()
        );
        return ossClient.listParts(request);
    }

    /**
     * 判断一个bucket可不可以公共读
     *
     * @param ossClient  oss客户端
     * @param bucketName 存储空间
     * @return true:是
     */
    private static boolean bucketIsPublicRead(OSS ossClient, String bucketName) {
        // 判断Bucket是否允许公共读访问
        AccessControlList acl = ossClient.getBucketAcl(bucketName);
        return acl.getCannedACL().equals(CannedAccessControlList.PublicRead)
                || acl.getCannedACL().equals(CannedAccessControlList.PublicReadWrite);
    }

    /**
     * 生成文件访问URL
     *
     * @param bucketName 存储空间名称
     * @param objectKey  存储对象的键（路径+文件名）
     * @param endpoint   存储服务的访问域名
     * @return 生成的文件访问URL
     */
    public static String generateFileUrl(String bucketName, String objectKey, String endpoint) {
        // 生成永久URL（实际生产环境应使用签名URL）
        return String.format(FILE_URL_FORMAT,
                bucketName,
                endpoint,
                objectKey);
    }

    /**
     * 转换元数据对象
     */
    private static OssFileMetadata convertToOssFileMetadata(String bucketName, String objectKey, ObjectMetadata meta) {
        OssFileMetadata metadata = new OssFileMetadata();
        metadata.setBucketName(bucketName);
        metadata.setObjectKey(objectKey);
        metadata.setContentType(meta.getContentType());
        metadata.setContentLength(meta.getContentLength());
        metadata.setLastUpdateTime(DateUtil.toLocalDateTime(meta.getLastModified()));
        metadata.setEtag(meta.getETag());
        metadata.setStorageClass(meta.getObjectStorageClass().toString());
        metadata.setUserMetadata(meta.getUserMetadata());
        metadata.setContentEncoding(meta.getContentEncoding());
        metadata.setContentDisposition(meta.getContentDisposition());
        metadata.setCacheControl(meta.getCacheControl());
        metadata.setVersionId(meta.getVersionId());
        metadata.setServerSideEncryption(meta.getServerSideEncryption());
        return metadata;
    }


    /**
     * 设置对象元数据
     *
     * @param fileName 上传文件的名称
     * @param param    参数
     * @param fileSize 上传文件的大小
     * @return ObjectMetadata
     */
    private static ObjectMetadata setObjectMetadata(String fileName, OssUploadParam param, Long fileSize) {
        String contentType = param.getContentType();
        Map<String, String> useMetadata = param.getMetadata();
        ObjectMetadata metadata = new ObjectMetadata();
        if (CollUtil.isNotEmpty(useMetadata)) {
            useMetadata.forEach(metadata::setHeader);
        }
        if (Objects.nonNull(fileSize)) {
            // 上传的文件的长度
            metadata.setContentLength(fileSize);
            metadata.setContentDisposition("filename/filesize=" + fileName + "/" + fileSize + "Byte");
        }
        // 指定该Object被下载时的网页的缓存行为
        metadata.setCacheControl("no-cache");
        // 指定该Object下设置Header
        metadata.setHeader("Pragma", "no-cache");
        // 指定该Object被下载时的内容编码格式
        metadata.setContentEncoding("utf-8");
        metadata.setContentType(Objects.isNull(contentType) ? OssFileTypeEnum.getContentType(fileName) : contentType);
        if (param.isPublicRead()) {
            metadata.setObjectAcl(CannedAccessControlList.PublicRead);
        } else {
            metadata.setObjectAcl(CannedAccessControlList.Private);
        }
 /*       if (Objects.nonNull(param.getExpiration())) {
            DateTime parse = DateUtil.parse(LocalDateTimeUtil.format(param.getExpiration(), DatePattern.NORM_DATETIME_PATTERN), DatePattern.NORM_DATETIME_PATTERN);
            metadata.setExpirationTime(parse);
        }*/
        return metadata;
    }
}
