package com.shixiaoyu.xiangyueproject.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云配置（application.yml aliyun.*）
 * endpoint 为 OSS Endpoint（如 https://oss-cn-beijing.aliyuncs.com）
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun")
public class AliyunProperties {
    private String accessKeyId;
    private String accessKeySecret;
    private String regionId;
    private String endpoint;
    private String bucketName;
}
