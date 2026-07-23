package com.shixiaoyu.xiangyueproject.entity.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "村落景区VO", description = "景区的基础信息VO")
public class ScenicVO {
    /**
     * 景区ID
     */
    @Schema(description = "景区ID")
    private Long id;
    /**
     * 景区名称
     */
    @Schema(description = "景区名称")
    private String name;

    /**
     * 关联村落名称
     */
    @JsonAlias({"village_name", "villageName"})
    @Schema(description = "关联村落名称")
    private String villageName;

    /**
     * 景区介绍
     */
    @Schema(description = "景区详细介绍（文本）")
    private String intro;

    /**
     * 景区图片
     */
    @Schema(description = "景区封面图片URL（多张用逗号分隔）")
    private String image;

    /**
     * 景区价格
     */
    @Schema(description = "景区门票价格（单位：元，0表示免费）")
    private Integer price;

    /**
     * 景区类型
     */
    @Schema(description = "景区类型：1-自然景观、2-人文景观、3-娱乐体验、4-民俗体验")
    private Integer type;

    /**
     * 收藏量
     */
    @Schema(description = "收藏量")
    private Integer collections;
    /**
     * 点赞量
     */
    @Schema(description = "点赞量")
    private Integer likes;

    /**
     * 运营景区是否提供住宿
     */
    @JsonAlias({"has_accommodation", "hasAccommodation"})
    @Schema(description = "是否提供住宿：0-否、1-是")
    private Integer hasAccommodation;

    /**
     * 住宿信息
     */
    @JsonAlias({"accommodation_info", "accommodationInfo"})
    @Schema(description = "住宿详情（房型、价格、联系方式等）")
    private String accommodationInfo;
}
