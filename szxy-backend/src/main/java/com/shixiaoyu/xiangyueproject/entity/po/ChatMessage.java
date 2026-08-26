package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.ChatMessageRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 聊天消息（方案二：一条记录对应一轮 user 或 assistant 内容）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_message")
@Schema(title = "AI 聊天消息", description = "按 sessionId + createTime 排序加载多轮上下文")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "所属会话 chat_session.id")
    private Long sessionId;

    @Schema(description = "所属用户 user.id（冗余，便于按用户查）")
    private Long userId;

    @Schema(description = "消息角色：1用户 2AI")
    private ChatMessageRoleEnum role;

    @Schema(description = "消息正文")
    private String content;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "消息发送时间")
    private LocalDateTime createTime;
}
