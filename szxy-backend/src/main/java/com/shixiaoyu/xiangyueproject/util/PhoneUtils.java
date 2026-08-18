package com.shixiaoyu.xiangyueproject.util;

import com.shixiaoyu.xiangyueproject.common.properties.AliyunProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableConfigurationProperties(AliyunProperties.class)
public class PhoneUtils {
    @Resource
    private AliyunProperties aliyunProperties;
    /**
     * <b>description</b> :
     * <p>使用凭据初始化账号Client</p>
     * @return Client
     *
     */
    public com.aliyun.dypnsapi20170525.Client createClient() throws Exception {
        com.aliyun.credentials.Client credential = new com.aliyun.credentials.Client();
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setCredential(credential);
        config.endpoint = "dypnsapi.aliyuncs.com";
        config.regionId = aliyunProperties.getRegionId();
        config.accessKeyId = aliyunProperties.getAccessKeyId();
        config.accessKeySecret = aliyunProperties.getAccessKeySecret();
        return new com.aliyun.dypnsapi20170525.Client(config);
    }
}
