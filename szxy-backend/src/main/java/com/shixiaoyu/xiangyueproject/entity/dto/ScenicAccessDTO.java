package com.shixiaoyu.xiangyueproject.entity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 景区资质申请。全局 Jackson 为 SNAKE_CASE 时，前端常用 camelCase，需 JsonAlias 兼容。
 * villageName 字段语义为村落主键 ID（与 ScenicServiceImpl 按 id 查询一致）。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "景区资质申请信息", description = "景区资质申请信息")
public class ScenicAccessDTO {
    /**
     * 关联村庄（实际为村落 ID，字段历史命名未改）
     */
    @Schema(description = "关联村落 ID（village_base.id）")
    @JsonAlias({"village_name", "villageName"})
    private Long villageName;

    /**
     * 申请人id
     */
    @Schema(description = "景区申请人ID（关联用户表主键）")
    @JsonAlias({"user_id", "userId"})
    private Long userId;

    /**
     * 景区名称
     */
    @Schema(description = "景区名称")
    private String name;

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
     * 运营景区是否提供住宿
     */
    @Schema(description = "是否提供住宿：0-否、1-是")
    @JsonAlias({"has_accommodation", "hasAccommodation"})
    private Integer hasAccommodation;

    /**
     * 住宿信息
     */
    @Schema(description = "住宿详情（房型、价格、联系方式等）")
    @JsonAlias({"accommodation_info", "accommodationInfo"})
    private String accommodationInfo;
}
