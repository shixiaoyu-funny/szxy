package com.shixiaoyu.xiangyueproject.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 认证/拦截器配置（application.yml auth.*）
 * exclude-path：无需登录即可访问的路径（登录注册、Swagger 文档等）
 * include-path：需要特定权限（管理端 role=4）的路径
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private List<String> excludePath;
    private List<String> includePath;
}
