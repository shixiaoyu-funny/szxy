package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 聊天会话（方案二：会话元数据，消息明细见 {@link ChatMessage}）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_session")
@Schema(title = "AI 聊天会话", description = "用户与禾小智的会话窗口，simpleDesc 为列表展示用概要")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "所属用户 user.id")
    private Long userId;

    @Schema(description = "会话概要（侧栏标题，如首句摘要或 AI 生成标题）")
    private String simpleDesc="新对话";

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "会话创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "会话更新时间（有新消息时刷新）")
    private LocalDateTime updateTime;
}
