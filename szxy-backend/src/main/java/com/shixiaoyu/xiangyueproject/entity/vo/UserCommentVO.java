package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.CommentShowEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论 VO（含评论人用户名）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "评论VO")
public class UserCommentVO {
    @Schema(description = "评论人用户名")
    private String username;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评分（1-5星）")
    private Integer score;

    @Schema(description = "评论图片URL")
    private String commentImg;

    @Schema(description = "是否展示：0隐藏 1展示")
    private CommentShowEnum isShow;

    @Schema(description = "评论时间")
    private LocalDateTime createTime;
}
