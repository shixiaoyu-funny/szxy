package com.shixiaoyu.xiangyueproject.entity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "用户评论实体DTO", description = "存储用户对村落/景点的评论和评分记录")
public class UserCommentDTO {

    @Schema(description = "用户ID")
    @JsonAlias({"user_id", "userId"})
    private Long userId;

    @JsonAlias({"target_id", "targetId"})
    @Schema(description = "评论目标ID")
    private Long targetId;

    @Schema(description = "评论内容")
    private String content;

    @JsonAlias({"comment_img", "commentImg"})
    @Schema(description = "评论图片URL")
    private String commentImg;

    @JsonAlias({"is_show", "isShow"})
    @Schema(description = "展示状态")
    private Integer isShow;

    @JsonAlias({"target_type", "targetType"})
    @Schema(description = "目标类型")
    private Integer targetType;

    @Schema(description = "评分")
    private Integer score;
}