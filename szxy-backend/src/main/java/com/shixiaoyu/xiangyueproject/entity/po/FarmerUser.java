package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("farm_user")
@Schema(title = "农户用户实体", description = "存储农户的基础信息和审核状态")
public class FarmerUser {
    /**
     * 农户ID
     */
    @Schema(description = "农户ID（自增主键）")
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "关联用户表的主键ID")
    private Long userId;

    /**
     * 农村id
     */
    @Schema(description = "关联村落基础表的主键ID")
    private Long villageId;

    /**
     *  农户姓名
     */
    @Schema(description = "农户真实姓名")
    private String farmName;

    /**
     * 身份证号码
     */
    @Schema(description = "农户身份证号码（脱敏存储）")
    private String idCard;

    /**
     * 账号状态
     */
    @Schema(description = "账号状态：0-待审核、1-已通过、2-已拒绝、3-已禁用")
    private Integer status;

    /**
     * 审核拒绝原因
     */
    @Schema(description = "审核拒绝时的原因说明")
    private String rejectReason;

    /**
     * 农户类型
     */
    @Schema(description = "农户类型：1-民宿经营者、2-农产品销售者、3-文旅服务者")
    private Integer type;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（自动填充）")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间（自动填充）")
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
