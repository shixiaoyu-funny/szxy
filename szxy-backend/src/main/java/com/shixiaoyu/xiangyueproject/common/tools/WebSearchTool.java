package com.shixiaoyu.xiangyueproject.common.tools;

import com.shixiaoyu.xiangyueproject.common.client.BochaWebSearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import static com.shixiaoyu.xiangyueproject.common.config.AIConfiguration.logToolInvoked;

/**
 * 禾小智联网搜索工具（博查 Web Search API）。
 */
@Component
@RequiredArgsConstructor
public class WebSearchTool {

    private final BochaWebSearchClient bochaWebSearchClient;

    @Tool(
            name = "web_search",
            description = """
                    联网搜索全网实时信息。
                    调用时机：新闻、政策、近期活动、天气、外部事实等时效性问题；
                    或本地知识库无法确定答案、没有绝对把握时必须调用。
                    输入简洁中文搜索关键词，返回标题、链接与摘要，回答时请引用来源链接。
                    """
    )
    public String webSearch(
            @ToolParam(description = "搜索关键词，例如：2026杭州西湖丰收节、某某村近期活动") String query) {
        logToolInvoked("web_search");
        return bochaWebSearchClient.search(query);
    }
}
