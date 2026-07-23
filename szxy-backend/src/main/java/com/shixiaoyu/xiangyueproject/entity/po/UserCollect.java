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
@Schema(title = "用户收藏实体", description = "存储用户对村落/景点的收藏记录")
public class UserCollect {
    /**
     * 收藏ID（主键自增）
     */
    @Schema(description = "收藏ID（自增主键）")
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
     * 收藏时间
     */
    @Schema(description = "收藏时间（自动填充）")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
