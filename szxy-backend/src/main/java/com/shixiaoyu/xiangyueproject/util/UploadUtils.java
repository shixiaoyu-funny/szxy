package com.shixiaoyu.xiangyueproject.util;

import cn.hutool.core.lang.UUID;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.shixiaoyu.xiangyueproject.config.AliyunProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AliyunProperties.class)
public class UploadUtils {
    private final OSS ossClient;
    private final AliyunProperties aliyunProperties;
    public String upload(byte[] content,String originalFilename) throws com.aliyuncs.exceptions.ClientException {
        //生成随机文件名
        String fileName = UUID.randomUUID().toString(true);
        String objectName=fileName+originalFilename.substring(originalFilename.lastIndexOf("."));

        try {
            // 2. 上传OSS
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    aliyunProperties.getBucketName(),
                    objectName,
                    new ByteArrayInputStream(content)
            );
            ossClient.putObject(putObjectRequest);

            log.info("OSS上传成功：{}", objectName);
        } catch (OSSException oe) {
            log.error("OSS请求异常：{}", oe.getErrorMessage());
            throw new RuntimeException("图片上传失败：" + oe.getErrorMessage()); // 抛异常，上层感知
        } catch (Exception ce) {
            log.error("OSS客户端异常：{}", ce.getMessage());
            throw new RuntimeException("图片上传失败：网络异常"); // 抛异常，上层感知
        }
        return "https://"+aliyunProperties.getBucketName()+"."+aliyunProperties.getEndpointOfOSS()+"/"+objectName;
    }
}
