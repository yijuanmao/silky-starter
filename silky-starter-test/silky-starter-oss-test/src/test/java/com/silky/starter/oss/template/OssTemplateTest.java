package com.silky.starter.oss.template;

import cn.hutool.core.io.FileUtil;
import com.silky.starter.oss.OssApplicationTest;
import com.silky.starter.oss.core.enums.OssFileTypeEnum;
import com.silky.starter.oss.model.metadata.OssFileMetadata;
import com.silky.starter.oss.model.param.DeleteFileOssParam;
import com.silky.starter.oss.model.param.DownloadFileOssParam;
import com.silky.starter.oss.model.param.GetFileMetadataParam;
import com.silky.starter.oss.model.param.OssUploadParam;
import com.silky.starter.oss.model.result.OssUploadResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.time.LocalDateTime;

/**
 * OSS模板测试类
 *
 * @author zy
 * @date 2025-08-14 15:52
 **/
public class OssTemplateTest extends OssApplicationTest {
    @Autowired
    private OssTemplate ossTemplate;

    /**
     * 智能上传测试方法
     */
    @Test
    public void testSmartUpload() {
        File file = new File("C:\\Users\\Administrator\\Desktop\\1755681995122.png");
        OssUploadParam param = new OssUploadParam();
        param.setPath("test/");
        param.setFileName("1755681995122.png");
        //文件 File和 Stream 二选一
//        param.setFile(file);
        param.setStream(FileUtil.getInputStream(file));

        //以下参数可选
        param.setExpiration(LocalDateTime.now().plusMinutes(5)); // 设置过期时间为当前时间加5分钟
        param.setContentType(OssFileTypeEnum.PNG.getContentType()); // 设置文件类型为PNG
//        param.setMetadata(); // 设置自定义元数据，如果不需要可以不设置
//        param.setFileSize(); // 设置文件大小，如果不需要可以不设置
//        param.setPublicRead(); // 设置是否公开可读，默认为true
//        param.setEnableEncryption(); // 设置是否启用加密，默认为false
        OssUploadResult result = ossTemplate.smartUpload(param);
        log.info("智能上传测试方法oss上传结果: {}", result);
    }

    /**
     * 删除文件测试
     */
    @Test
    public void testDeleteFile() {
        DeleteFileOssParam param = new DeleteFileOssParam();
        param.setObjectKey("test/1755681995122.png");
        ossTemplate.deleteFile(param);
        log.info("删除文件测试成功");
    }

    /**
     * 将文件下载到本地测试
     */
    @Test
    public void testDownloadFile() {
        DownloadFileOssParam param = new DownloadFileOssParam();
        param.setObjectKey("test/1755681995122.png");
        param.setLocalFilePath("D:\\download\\11.png");
        ossTemplate.downloadFile(param);
        log.info("将文件下载到本地测试");
    }

    /**
     * 获取文件元数据测试
     */
    @Test
    public void testGetFileMetadata() {
        GetFileMetadataParam param = new GetFileMetadataParam();
        param.setObjectKey("test/1755681995122.png");
        OssFileMetadata fileMetadata = ossTemplate.getFileMetadata(param);
        log.info("获取文件元数据测试,响应结果：" + fileMetadata);
    }
}
