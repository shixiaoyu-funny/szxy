package com.shixiaoyu.xiangyueproject.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
@EnableConfigurationProperties(AliyunProperties.class)
@RequiredArgsConstructor
public class UploadConfiguration {
    private final AliyunProperties aliyunProperties;
    @Bean
    public OSS OSSClient(){
        CredentialsProvider credentialsProvider=CredentialsProviderFactory.newDefaultCredentialProvider(aliyunProperties.getAccessKeyId(), aliyunProperties.getAccessKeySecret());
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        return OSSClientBuilder.create()
                .endpoint(aliyunProperties.getEndpointOfOSS())
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(aliyunProperties.getRegionId())
                .build();
    }
}
