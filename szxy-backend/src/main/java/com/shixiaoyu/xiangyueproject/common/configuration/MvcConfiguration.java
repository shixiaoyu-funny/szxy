package com.shixiaoyu.xiangyueproject.common.configuration;

import com.shixiaoyu.xiangyueproject.common.properties.AuthProperties;
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
 * MVC 配置：注册登录拦截（含管理端角色校验） + 流量统计拦截
 * 登录注册/文档放行路径与管理端路径均来自 application.yml 的 auth.* 配置
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class MvcConfiguration implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final FreshInterceptor freshInterceptor;
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
