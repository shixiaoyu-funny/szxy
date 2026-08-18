package com.shixiaoyu.xiangyueproject.util;

import cn.hutool.core.lang.UUID;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import com.shixiaoyu.xiangyueproject.common.properties.AliyunProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * OSS 上传工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadUtils {
    private final OSS ossClient;
    private final AliyunProperties aliyunProperties;

    public String upload(byte[] content, String originalFilename) {
        String fileName = UUID.randomUUID().toString(true);
        String objectName = fileName + originalFilename.substring(originalFilename.lastIndexOf("."));
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    aliyunProperties.getBucketName(),
                    objectName,
                    new ByteArrayInputStream(content));
            ossClient.putObject(putObjectRequest);
            log.info("OSS上传成功：{}", objectName);
        } catch (OSSException oe) {
            log.error("OSS请求异常：{}", oe.getErrorMessage());
            throw new RuntimeException("图片上传失败：" + oe.getErrorMessage());
        } catch (Exception ce) {
            log.error("OSS客户端异常：{}", ce.getMessage());
            throw new RuntimeException("图片上传失败：网络异常");
        }
        // https://{bucket}.{endpoint-host}/{objectName}
        String host = aliyunProperties.getEndpoint()
                .replace("https://", "").replace("http://", "");
        return "https://" + aliyunProperties.getBucketName() + "." + host + "/" + objectName;
    }
}
