package com.shixiaoyu.xiangyueproject.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收到的点赞动态（消息页只读）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "收到点赞动态VO")
public class LikeReceivedVO {

    @Schema(description = "点赞人用户名")
    private String username;

    @Schema(description = "点赞人头像")
    private String avatar;

    @Schema(description = "动作文案")
    private String actionText;

    @Schema(description = "景点ID")
    private Long scenicId;

    @Schema(description = "景点名称")
    private String scenicName;

    @Schema(description = "时间")
    private LocalDateTime createTime;
}
