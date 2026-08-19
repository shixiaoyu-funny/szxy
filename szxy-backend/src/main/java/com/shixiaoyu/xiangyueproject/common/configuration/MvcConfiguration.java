package com.shixiaoyu.xiangyueproject.common.configuration;

import com.shixiaoyu.xiangyueproject.common.properties.AuthProperties;
import com.shixiaoyu.xiangyueproject.interceptor.AdminInterceptor;
import com.shixiaoyu.xiangyueproject.interceptor.FreshInterceptor;
import com.shixiaoyu.xiangyueproject.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC 配置：注册登录拦截 + 管理端拦截（admin-path）+ 流量统计拦截
 * 登录注册/文档放行路径来自 application.yml 的 auth.exclude-path，管理端路径来自 auth.admin-path
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class MvcConfiguration implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final FreshInterceptor freshInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final AuthProperties authProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePaths = authProperties.getExcludePath();
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths);
        registry.addInterceptor(freshInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths);
        // 管理端路径（当前 /report/**）：登录拦截先通过后，再强制 role=4
        List<String> adminPaths = authProperties.getAdminPath();
        if (adminPaths != null && !adminPaths.isEmpty()) {
            registry.addInterceptor(adminInterceptor)
                    .addPathPatterns(adminPaths);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
