package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.CommentShowEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 景点评论
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_comment")
@Schema(title = "用户评论实体", description = "存储用户对景点的评论和评分记录")
public class UserComment {
    @TableId(type = IdType.AUTO)
    @Schema(description = "评论ID（自增主键）")
    private Long id;

    @Schema(description = "评论人 user.id")
    private Long userId;

    @Schema(description = "景点ID")
    private Long scenicId;

    @Schema(description = "父评论ID，NULL为一级评论")
    private Long parentId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评分（1-5星）")
    private Integer score;

    @Schema(description = "评论图片URL（多张用逗号分隔）")
    private String commentImg;

    @Schema(description = "是否展示：0隐藏 1展示")
    private CommentShowEnum isShow;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "评论创建时间（自动填充）")
    private LocalDateTime createTime;
}
