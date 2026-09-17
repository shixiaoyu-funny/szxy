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

    @Schema(description = "村落特色类型：1古建聚落 2非遗民俗 3山水生态 4农业观光 5近郊休闲 6康养度假 7红色研学 8滨水渔乡 9民族村寨 10综合文旅")
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

    @Schema(description = "村长用户ID（可选；须为已在该村建档的农户，设置时自动任命为村长）")
    private Long manageId;
}
