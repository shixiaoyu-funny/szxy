package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 游客匿名 AI 聊天请求（不落库、不持久化）
 * 上下文由前端携带 history 传入，服务端不保存会话
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "匿名AI聊天请求")
public class AnonymousChatDTO {

    @Schema(description = "本轮文本内容（可与图片同时存在；纯图片时可为空白）")
    private String content;

    @Schema(description = "本轮已上传图片的公网 URL 列表")
    private List<String> mediaUrls;

    @Schema(description = "前端内存中的历史对话（按时间升序），用于提供上下文")
    private List<HistoryMessage> history;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(title = "历史消息")
    public static class HistoryMessage {
        @Schema(description = "角色：1用户 2助手")
        private Integer role;

        @Schema(description = "消息内容")
        private String content;

        @Schema(description = "用户消息附带图片 URL 列表")
        private List<String> mediaUrls;
    }
}
