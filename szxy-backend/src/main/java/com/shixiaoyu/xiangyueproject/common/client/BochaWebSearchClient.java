package com.shixiaoyu.xiangyueproject.common.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.shixiaoyu.xiangyueproject.common.properties.WebSearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * 博查 POST /v1/web-search 请求与响应解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BochaWebSearchClient {

    private final WebSearchProperties properties;

    /**
     * 调用博查搜索，返回供 LLM 阅读的文本摘要。
     */
    public String search(String query) {
        if (StrUtil.isBlank(query)) {
            return "搜索关键词不能为空。";
        }
        if (StrUtil.isBlank(properties.getApikey())) {
            return "博查 API Key 未配置，请在 application-local.yml 的 web-search.apikey 或管理端 system_config 中设置。";
        }

        Map<String, Object> body = new HashMap<>();
        body.put("query", query.trim());
        body.put("summary", properties.isSummary());
        body.put("freshness", properties.getFreshness());
        body.put("count", Math.min(Math.max(properties.getCount(), 1), 50));

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());

        RestClient client = RestClient.builder()
                .requestFactory(factory)
                .build();

        try {
            String responseBody = client.post()
                    .uri(properties.getBaseUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApikey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return formatResults(query.trim(), responseBody);
        } catch (RestClientException e) {
            log.warn("博查搜索请求失败 query={} msg={}", query, e.getMessage());
            return "联网搜索暂时不可用：" + e.getMessage();
        }
    }

    private String formatResults(String query, String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return "未搜索到与「" + query + "」相关的结果。";
        }

        JSONObject root = JSONUtil.parseObj(responseBody);

        JSONObject error = root.getJSONObject("error");
        if (error != null) {
            String msg = error.getStr("message", error.toString());
            return "搜索接口返回错误：" + msg;
        }

        JSONObject webPages = resolveWebPages(root);
        if (webPages == null) {
            return "未搜索到与「" + query + "」相关的结果。";
        }

        JSONArray items = webPages.getJSONArray("value");
        if (items == null || items.isEmpty()) {
            return "未搜索到与「" + query + "」相关的结果。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("关键词「").append(query).append("」的联网搜索结果：\n\n");

        int limit = Math.min(items.size(), properties.getCount());
        for (int i = 0; i < limit; i++) {
            JSONObject item = items.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String title = firstNonBlank(item.getStr("name"), item.getStr("title"), "无标题");
            String url = item.getStr("url");
            String site = item.getStr("siteName");
            String date = item.getStr("datePublished");
            String text = firstNonBlank(item.getStr("summary"), item.getStr("snippet"), "");

            sb.append(i + 1).append(". **").append(title).append("**");
            if (StrUtil.isNotBlank(site)) {
                sb.append("（").append(site).append("）");
            }
            sb.append("\n");
            if (StrUtil.isNotBlank(url)) {
                sb.append("   链接：").append(url).append("\n");
            }
            if (StrUtil.isNotBlank(date)) {
                sb.append("   时间：").append(date).append("\n");
            }
            if (StrUtil.isNotBlank(text)) {
                sb.append("   摘要：").append(StrUtil.maxLength(text, 400)).append("\n");
            }
            sb.append("\n");
        }

        Integer total = webPages.getInt("totalEstimatedMatches");
        if (total != null && total > 0) {
            sb.append("（约 ").append(total).append(" 条相关结果，以上展示前 ").append(limit).append(" 条）");
        }

        return sb.toString().trim();
    }

    /** 兼容 data.webPages 与顶层 webPages 两种响应结构 */
    private JSONObject resolveWebPages(JSONObject root) {
        JSONObject data = root.getJSONObject("data");
        if (data != null && data.getJSONObject("webPages") != null) {
            return data.getJSONObject("webPages");
        }
        return root.getJSONObject("webPages");
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (StrUtil.isNotBlank(c)) {
                return c;
            }
        }
        return "";
    }
}
