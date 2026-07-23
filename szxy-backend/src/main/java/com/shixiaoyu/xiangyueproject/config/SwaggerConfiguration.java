package com.shixiaoyu.xiangyueproject.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfiguration implements WebMvcConfigurer {
    /**
     * 配置OpenAPI接口文档
     * @return
     */
    @Bean
    public OpenAPI logisticsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("数智乡约——AI驱动乡村振兴服务平台API接口文档")
                        .description("基于SpringBoot3.5.11的AI驱动乡村振兴服务平台")
                        .version("1.0.0"));
    }
}
