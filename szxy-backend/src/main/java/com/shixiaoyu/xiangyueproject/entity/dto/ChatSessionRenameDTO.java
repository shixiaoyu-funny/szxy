package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改 AI 会话名称（侧栏 simpleDesc）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "修改会话名称")
public class ChatSessionRenameDTO {

    @Schema(description = "新会话名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String simpleDesc;
}
