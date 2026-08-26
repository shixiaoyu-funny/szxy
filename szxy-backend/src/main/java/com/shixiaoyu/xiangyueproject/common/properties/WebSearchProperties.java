package com.shixiaoyu.xiangyueproject.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 博查 Web Search API（application.yml ai-websearch.*）
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-websearch")
public class WebSearchProperties {

    /** Bearer Token */
    private String apikey;

    /** 完整请求地址 */
    private String baseUrl = "https://api.bochaai.com/v1/web-search";

    /** 返回条数 1-50 */
    private int count = 10;

    /** 是否返回 AI 摘要 */
    private boolean summary = true;

    /** 时间范围：noLimit / oneDay / oneWeek / oneMonth / oneYear */
    private String freshness = "noLimit";

    private Duration connectTimeout = Duration.ofSeconds(10);

    private Duration readTimeout = Duration.ofSeconds(15);
}
