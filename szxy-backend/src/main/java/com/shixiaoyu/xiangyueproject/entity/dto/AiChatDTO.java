package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 对话请求（文本 + 可选多模态图片 URL）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "AI聊天请求")
public class AiChatDTO {

    @Schema(description = "文本内容（可与图片同时存在；纯图片时可为空白）")
    private String content;

    @Schema(description = "已上传图片的公网 URL 列表（先调 /upload）")
    private List<String> mediaUrls;
}
