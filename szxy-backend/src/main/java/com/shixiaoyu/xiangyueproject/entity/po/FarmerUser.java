package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 农户档案（纯业务档案，无审核状态）
 * user_id 与 user 一一对应；禁用走 user.status
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("farm_user")
@Schema(title = "农户用户实体", description = "存储农户的基础经营信息")
public class FarmerUser {
    @Schema(description = "农户档案ID（自增主键）")
    private Long id;

    @Schema(description = "关联 user.id（唯一）")
    private Long userId;

    @Schema(description = "所属农村ID")
    private Long villageId;

    @Schema(description = "身份证号码")
    private String idCard;

    @Schema(description = "经营类型：1民宿 2农产品 3文旅")
    private BusinessTypeEnum businessType;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间（自动填充）")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "修改时间（自动填充）")
    private LocalDateTime updateTime;
}
