package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.ScenicTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 景点 VO（含所属村落名称）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "村落景区VO")
public class ScenicVO {
    @Schema(description = "景区ID")
    private Long id;

    @Schema(description = "所属农村ID")
    private Long villageId;

    @Schema(description = "景区名称")
    private String name;

    @Schema(description = "所属村落名称")
    private String villageName;

    @Schema(description = "景区详细介绍")
    private String intro;

    @Schema(description = "景区封面图片URL")
    private String image;

    @Schema(description = "景区门票价格（元，0免费）")
    private Integer price;

    @Schema(description = "景区类型：1自然景观 2人文景观 3娱乐体验 4民俗体验")
    private ScenicTypeEnum type;

    @Schema(description = "是否提供住宿：0否 1是")
    private Integer hasAccommodation;

    @Schema(description = "住宿详情")
    private String accommodationInfo;

    @Schema(description = "点赞量")
    private Integer likes;

    @Schema(description = "收藏量")
    private Integer collections;
}
