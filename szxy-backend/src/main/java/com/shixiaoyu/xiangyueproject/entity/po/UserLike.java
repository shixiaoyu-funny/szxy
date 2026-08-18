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
 * 点赞记录（user_id+scenic_id 唯一）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_like")
@Schema(title = "用户点赞实体", description = "存储用户对景点的点赞记录")
public class UserLike {
    @TableId(type = IdType.AUTO)
    @Schema(description = "点赞ID（自增主键）")
    private Long id;

    @Schema(description = "用户 user.id")
    private Long userId;

    @Schema(description = "景点ID")
    private Long scenicId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "点赞时间（自动填充）")
    private LocalDateTime createTime;
}
