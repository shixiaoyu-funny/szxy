package com.shixiaoyu.xiangyueproject.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun")
public class AliyunProperties {
    private String accessKeyId;
    private String accessKeySecret;
    private String regionId;
    private String endpointOfInfo;
    private String endpointOfOSS;
    private String bucketName;
}
