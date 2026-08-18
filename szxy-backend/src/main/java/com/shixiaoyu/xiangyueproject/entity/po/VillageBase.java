package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.VillageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 农村基础信息
 * manage_id：村长 user.id（与 user.role=3 在事务内同步）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("village_base")
@Schema(title = "村落基础信息实体", description = "存储乡村的基础地理、介绍等核心信息")
public class VillageBase {
    @Schema(description = "村落ID（自增主键）")
    private Long id;

    @Schema(description = "村长ID（关联 user.id）")
    private Long manageId;

    @Schema(description = "村落名称（唯一）")
    private String name;

    @Schema(description = "所属省份")
    private String province;

    @Schema(description = "所属城市")
    private String city;

    @Schema(description = "所属区县")
    private String county;

    @Schema(description = "村落经度")
    private BigDecimal longitude;

    @Schema(description = "村落纬度")
    private BigDecimal latitude;

    @Schema(description = "村落特色类型：1古村落 2生态村 3民俗村 4文旅村")
    private VillageTypeEnum type;

    @Schema(description = "村落详细介绍")
    private String intro;

    @Schema(description = "村落封面图片URL（多张用逗号分隔）")
    private String image;

    @Schema(description = "最佳游玩时间")
    private String bestTime;

    @Schema(description = "季节性特色活动")
    private String activity;

    @Schema(description = "村落官方联系方式")
    private String contact;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间（自动填充）")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "修改时间（自动填充）")
    private LocalDateTime updateTime;
}
