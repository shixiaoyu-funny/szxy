package com.shixiaoyu.xiangyueproject.config;

import com.shixiaoyu.xiangyueproject.interceptor.FreshInterceptor;
import com.shixiaoyu.xiangyueproject.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(MvcPathProperties.class)
public class MvcConfiguration implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    private final FreshInterceptor freshInterceptor;
    private final MvcPathProperties mvcPathProperties;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("正在注册拦截器");
        log.info("排除的路径：{}", mvcPathProperties.getExcludePath());
        log.info("包含的路径：{}", mvcPathProperties.getIncludePath());
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(mvcPathProperties.getIncludePath())
                .excludePathPatterns(mvcPathProperties.getExcludePath());
        registry.addInterceptor(freshInterceptor)
                .addPathPatterns(mvcPathProperties.getIncludePath())
                .excludePathPatterns(mvcPathProperties.getExcludePath());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 静态资源映射（Swagger 相关）
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
