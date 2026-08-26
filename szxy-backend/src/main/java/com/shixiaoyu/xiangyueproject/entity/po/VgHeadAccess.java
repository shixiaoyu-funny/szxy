package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.VgHeadStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 村长准入申请
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("vghead_access")
@Schema(title = "村长准入申请")
public class VgHeadAccess {

    @TableId(type = IdType.AUTO)
    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "村落ID（冗余）")
    private Long villageId;

    @Schema(description = "申请人 farm_user.id")
    private Long fuId;

    @Schema(description = "状态：0待审 1村长已审 2管理员已审 3已拒绝")
    private VgHeadStatusEnum status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
