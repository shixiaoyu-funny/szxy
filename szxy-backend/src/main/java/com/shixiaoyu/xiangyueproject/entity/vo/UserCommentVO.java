package com.shixiaoyu.xiangyueproject.entity.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCommentVO {
    /**
     * 用户名
     */
    private String username;
    /**
     * 评论内容
     */
    private String content;

    /**
     * 评分（1-5星）
     */
    private Integer score;

    /**
     * 评论图片（多图逗号分隔）
     */
    @JsonAlias({"comment_img", "commentImg"})
    private String commentImg;

    /**
     * 创建时间
     */
    @JsonAlias({"create_time", "createTime"})
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 是否展示（0-隐藏/1-展示）
     */
    @JsonAlias({"is_show", "isShow"})
    private Integer isShow;
}
