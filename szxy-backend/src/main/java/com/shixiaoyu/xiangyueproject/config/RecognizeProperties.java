package com.shixiaoyu.xiangyueproject.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "baiduyun.recognize")
public class RecognizeProperties {
    private String apiKey;
    private String secretKey;
}
