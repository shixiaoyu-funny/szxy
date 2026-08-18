package com.shixiaoyu.xiangyueproject.entity.dto;

import com.shixiaoyu.xiangyueproject.enums.CommentShowEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论 DTO（userId 由服务端从登录态填充）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "用户评论DTO")
public class UserCommentDTO {
    @NotNull(message = "景点ID不能为空")
    @Schema(description = "景点ID")
    private Long scenicId;

    @Schema(description = "评论内容")
    private String content;

    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    @Schema(description = "评分（1-5星）")
    private Integer score;

    @Schema(description = "评论图片URL")
    private String commentImg;

    @Schema(description = "是否展示：0隐藏 1展示")
    private CommentShowEnum isShow;
}
