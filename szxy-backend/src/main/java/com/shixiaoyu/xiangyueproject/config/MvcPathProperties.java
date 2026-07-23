package com.shixiaoyu.xiangyueproject.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "mvc.interceptor")
public class MvcPathProperties {
    private List<String> excludePath;
    private List<String> includePath;
}
