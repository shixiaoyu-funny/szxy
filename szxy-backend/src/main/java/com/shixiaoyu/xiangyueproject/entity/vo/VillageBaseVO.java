package com.shixiaoyu.xiangyueproject.entity.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "村落基础信息实体", description = "存储乡村的基础地理、介绍等核心信息")
public class VillageBaseVO {
    /**
     * 村落ID
     */
    @Schema(description = "村落ID（自增主键）")
    private Long id;
    /**
     * 村长姓名
     */
    @Schema(description = "村长姓名")
    @JsonAlias({"manager_name", "managerName"})
    private String managerName;

    /**
     * 农村名称
     */
    @Schema(description = "村落名称（唯一）")
    private String name;

    /**
     * 省份
     */
    @Schema(description = "所属省份（如：浙江省）")
    private String province;

    /**
     * 城市
     */
    @Schema(description = "所属城市（如：杭州市）")
    private String city;

    /**
     * 区/县
     */
    @Schema(description = "所属区县（如：西湖区）")
    private String county;

    /**
     * 村落类型（特色）
     */
    @Schema(description = "村落特色类型：1-古村落、2-生态村、3-民俗村、4-文旅村")
    private Integer type;

    /**
     * 村落介绍
     */
    @Schema(description = "村落详细介绍（文本）")
    private String intro;

    /**
     * 村落图片
     */
    @Schema(description = "村落封面图片URL（多张用逗号分隔）")
    private String image;

    /**
     * 最佳游玩时间
     */
    @Schema(description = "最佳游玩时间（如：3-5月、9-11月）")
    @JsonAlias({"best_time", "bestTime"})
    private String bestTime;

    /**
     * 季节性活动
     */
    @Schema(description = "季节性特色活动（如：油菜花节、丰收节）")
    private String activity;

    /**
     * 联系电话/邮箱
     */
    @Schema(description = "村落官方联系方式（电话/邮箱，用逗号分隔）")
    private String contact;

    /**
     * 点赞量
     */
    @Schema(description = "点赞量")
    private Integer likes;

    @Schema(description = "收藏量")
    private Integer collects;
}
