package com.shixiaoyu.xiangyueproject.entity.dto;

import com.shixiaoyu.xiangyueproject.enums.VillageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 农村基础信息 DTO（管理端新增/修改）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "村落基础信息DTO")
public class VillageBaseDTO {
    @Schema(description = "村落名称")
    private String name;

    @Schema(description = "所属省份")
    private String province;

    @Schema(description = "所属城市")
    private String city;

    @Schema(description = "所属区县")
    private String county;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "村落特色类型：1古村落 2生态村 3民俗村 4文旅村")
    private VillageTypeEnum type;

    @Schema(description = "村落详细介绍")
    private String intro;

    @Schema(description = "封面图片URL")
    private String image;

    @Schema(description = "最佳游玩时间")
    private String bestTime;

    @Schema(description = "季节性活动")
    private String activity;

    @Schema(description = "联系方式")
    private String contact;
}
