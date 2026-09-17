package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.VillageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 村落 VO（含村长展示名与点赞/收藏量）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "村落基础信息VO")
public class VillageBaseVO {
    @Schema(description = "村落ID")
    private Long id;

    @Schema(description = "村长展示名（user.username）")
    private String managerName;

    @Schema(description = "村落名称")
    private String name;

    @Schema(description = "所属省份")
    private String province;

    @Schema(description = "所属城市")
    private String city;

    @Schema(description = "所属区县")
    private String county;

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

    @Schema(description = "村长用户ID")
    private Long manageId;

    @Schema(description = "总点赞量（下属景点之和）")
    private Integer likes;

    @Schema(description = "总收藏量（下属景点之和）")
    private Integer collects;
}
