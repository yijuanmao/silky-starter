package com.silky.starter.oss.core.util.huawei;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.IdUtil;
import com.obs.services.ObsClient;
import com.obs.services.model.*;
import com.silky.starter.oss.core.constant.OssConstants;
import com.silky.starter.oss.core.enums.OssFileTypeEnum;
import com.silky.starter.oss.core.util.OssFileUtil;
import com.silky.starter.oss.model.config.OssProviderConfig;
import com.silky.starter.oss.model.metadata.OssFileMetadata;
import com.silky.starter.oss.model.param.*;
import com.silky.starter.oss.model.result.GenPreSignedUrlResult;
import com.silky.starter.oss.model.result.OssUploadResult;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 华为OBS工具类
 *
 * @author zy
 * @date 2025-08-15 10:01
 **/
public class HuaWeiObsUtils {

    /**
     * 简单上传文件
     *
     * @param obsClient obs连接
     * @param config    配置
     * @param param     参数
     * @return OssUploadResult
     */
    public static OssUploadResult uploadFile(ObsClient obsClient, OssProviderConfig config, OssUploadParam param) {
        InputStream is = Objects.nonNull(param.getStream()) ? param.getStream() : FileUtil.getInputStream(param.getFile());

        //新文件名：带后缀
        String fileName = IdUtil.fastSimpleUUID() + OssConstants.DOT + FileTypeUtil.getType(is);
        String contentType = (Objects.isNull(param.getContentType()) ? OssFileTypeEnum.getContentType(fileName) : param.getContentType());
        //元数据
        ObjectMetadata metadata = setMetadata(param, contentType, param.getFileSize());

        String objectKey = OssFileUtil.buildObjectKey(param.getPath(), fileName);

        PutObjectRequest request = new PutObjectRequest(config.getBucketName(), objectKey, is);
        request.setMetadata(metadata);
        //默认公共读
        request.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
        if (Objects.nonNull(param.getExpiration())) {
            long days = LocalDateTimeUtil.between(LocalDateTime.now(), param.getExpiration()).toDays();
            request.setExpires(Convert.toInt(days));
        }
        PutObjectResult objectResult = obsClient.putObject(request);
        return new OssUploadResult(objectResult.getObjectUrl(), fileName, objectResult.getObjectKey(), true, "上传成功");
    }

    /**
     * 下载文件到本地
     *
     * @param obsClient obs连接
     * @param config    配置
     * @param param     请求参数
     */
    public static void downloadFileOss(ObsClient obsClient, OssProviderConfig config, DownloadFileOssParam param) {
        GetObjectRequest objectRequest = new GetObjectRequest(config.getBucketName(), param.getObjectKey());
        obsClient.getObject(objectRequest);
    }

    /**
     * 删除oss上的文件
     *
     * @param obsClient obs连接
     * @param config    配置
     * @param param     请求参数
     */
    public static void deleteFileOss(ObsClient obsClient, OssProviderConfig config, DeleteFileOssParam param) {
        obsClient.deleteObject(config.getBucketName(), param.getObjectKey());
    }

    /**
     * 发起分片上传
     *
     * @param obsClient obs连接
     * @param config    配置
     * @param param     分片上传参数，包含必要的上传信息
     */
    public static InitiateMultipartUploadResult initiateMultipartUpload(ObsClient obsClient, OssProviderConfig config, InitiateMultipartUploadParam param) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(config.getBucketName(), OssFileUtil.buildObjectKey(param.getPath(), param.getFileName()));
        if (CollUtil.isNotEmpty(param.getMetadata())) {
            ObjectMetadata metadata = new ObjectMetadata();
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(param.getMetadata(), metadata, copyOptions);
        }
        return obsClient.initiateMultipartUpload(request);
    }

    /**
     * 上传分片
     *
     * @param obsClient obs连接
     * @param config    配置
     * @param param     请求参数
     * @return UploadPartResult
     */
    public static UploadPartResult uploadPart(ObsClient obsClient, OssProviderConfig config, UploadPartParam param) {
        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(config.getBucketName());
        request.setObjectKey(param.getObjectKey());
        request.setUploadId(param.getUploadId());
        request.setPartNumber(param.getPartNumber());
        request.setInput(param.getInputStream());
        request.setPartSize(param.getPartSize());
        return obsClient.uploadPart(request);
    }

    /**
     * 完成分片上传
     *
     * @param obsClient obs连接
     * @param config    配置
     * @param param     请求参数
     * @return CompleteMultipartUploadResult
     */
    public static CompleteMultipartUploadResult completeMultipartUpload(ObsClient obsClient, OssProviderConfig config, CompleteMultipartUploadParam param) {
        // 转换ETag格式
        List<PartEtag> eTags = param.getPartETags().entrySet().stream()
                .map(entry -> new PartEtag(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
        return obsClient.completeMultipartUpload(
                new CompleteMultipartUploadRequest(
                        config.getBucketName(),
                        param.getObjectKey(),
                        param.getUploadId(),
                        eTags)
        );
    }

    /**
     * 取消分片上传
     *
     * @param obsClient obs连接
     * @param config    配置
     * @param param     请求参数
     */
    public static void abortMultipartUpload(ObsClient obsClient, OssProviderConfig config, AbortMultipartUploadParam param) {
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(
                config.getBucketName(),
                param.getObjectKey(),
                param.getUploadId()
        );
        obsClient.abortMultipartUpload(request);
    }

    /**
     * 列出已上传的分片
     *
     * @param obsClient obs连接
     * @param config    配置
     * @param param     请求参数
     */
    public static ListPartsResult listParts(ObsClient obsClient, OssProviderConfig config, ListPartsParam param) {
        ListPartsRequest request = new ListPartsRequest(
                config.getBucketName(),
                param.getObjectKey(),
                param.getUploadId()
        );
        return obsClient.listParts(request);
    }

    /**
     * 获取文件元信息
     *
     * @param obsClient obs连接
     * @param config    配置
     * @param param     请求参数
     * @return ObjectMetadata
     */
    public static OssFileMetadata getObjectMetadata(ObsClient obsClient, OssProviderConfig config, GetFileMetadataParam param) {
        ObjectMetadata metadata = obsClient.getObjectMetadata(config.getBucketName(), param.getObjectKey());
        return convertToOssFileMetadata(config.getBucketName(), param.getObjectKey(), metadata);
    }

    /**
     * 生成预签名
     *
     * @param obsClient obs连接
     * @param config    配置
     * @param param     参数
     * @return GenPreSignedUrlResult
     */
    public static GenPreSignedUrlResult genPreSignedUrl(ObsClient obsClient, OssProviderConfig config, GenPreSignedUrlParam param) {
        LocalDateTime expiration = param.getExpiration();
        String bucketName = config.getBucketName();
        long expirationDate;
        if (Objects.isNull(expiration)) {
            //设置URl过期时间为99年：3600L*1000*24*365*99
            expirationDate = new Date().getTime() + 3600L * 1000 * 24 * 365 * 99;
        } else {
            expirationDate = LocalDateTimeUtil.between(LocalDateTime.now(), expiration).toMillis();
        }
        TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.POST, expirationDate);
        request.setBucketName(bucketName);
        request.setObjectKey(param.getObjectKey());

        TemporarySignatureResponse response = obsClient.createTemporarySignature(request);
        return new GenPreSignedUrlResult(response.getSignedUrl());
    }

    /**
     * 设置元数据
     *
     * @param param         上传参数
     * @param contentType   文件类型
     * @param contentLength 文件长度
     * @return ObjectMetadata
     */
    private static ObjectMetadata setMetadata(OssUploadParam param, String contentType, Long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        if (CollUtil.isNotEmpty(param.getMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(param.getMetadata(), metadata, copyOptions);
        }
        if (Objects.nonNull(contentLength)) {
            // 上传的文件的长度
            metadata.setContentLength(contentLength);
        }
        // 指定该Object被下载时的内容编码格式
        metadata.setContentEncoding("utf-8");
        return metadata;
    }

    /**
     * 转换字符串权限
     *
     * @param acl 访问控制列表字符串
     * @return AccessControlList
     */
    private static AccessControlList convertAcl(String acl) {
        AccessControlList aclObj;
        switch (acl) {
            case "private":
                aclObj = AccessControlList.REST_CANNED_PRIVATE;
                break;
            case "read_write":
                aclObj = AccessControlList.REST_CANNED_PUBLIC_READ_WRITE;
                break;
            default:
                aclObj = AccessControlList.REST_CANNED_PUBLIC_READ;
        }
        return aclObj;
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
        metadata.setEtag(meta.getEtag());
        metadata.setStorageClass(meta.getObjectStorageClass().toString());
        metadata.setContentEncoding(meta.getContentEncoding());
        metadata.setContentDisposition(meta.getContentDisposition());
        metadata.setCacheControl(meta.getCacheControl());
        return metadata;
    }
}
