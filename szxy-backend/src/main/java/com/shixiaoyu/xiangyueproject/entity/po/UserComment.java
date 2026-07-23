package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "用户评论实体", description = "存储用户对村落/景点的评论和评分记录")
public class UserComment {
    /**
     * 评论ID（主键自增）
     */
    @Schema(description = "评论ID（自增主键）")
    private Long id;

    /**
     * 用户ID（关联user.id）
     */
    @Schema(description = "用户ID（关联基础用户表主键）")
    private Long userId;

    /**
     * 景点id
     */
    @Schema(description = "景点id")
    private Long targetId;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容（最大256字符）")
    private String content;

    /**
     * 评分（1-5星）
     */
    @Schema(description = "评分（1-5星，1星最低，5星最高）")
    private Integer score;

    /**
     * 评论图片（多图逗号分隔）
     */
    @Schema(description = "评论图片URL（多张图片用逗号分隔）")
    private String commentImg;

    /**
     * 创建时间
     */
    @Schema(description = "评论创建时间（自动填充）")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 是否展示（0-隐藏/1-展示）
     */
    @Schema(description = "评论展示状态：0-隐藏、1-正常展示")
    private Integer isShow;
}
