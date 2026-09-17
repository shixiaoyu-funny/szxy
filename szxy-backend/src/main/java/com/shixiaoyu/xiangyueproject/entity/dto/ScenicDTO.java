package com.shixiaoyu.xiangyueproject.entity.dto;

import com.shixiaoyu.xiangyueproject.enums.ScenicTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 景点 DTO（农户/村长在所属村直接新增）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "景点DTO")
public class ScenicDTO {
    @NotNull(message = "所属农村ID不能为空")
    @Schema(description = "所属农村ID（须为当前农户/村长所属村）")
    private Long villageId;

    @Schema(description = "景点名称")
    private String name;

    @Schema(description = "景点介绍")
    private String intro;

    @Schema(description = "图片URL（多张逗号分隔）")
    private String image;

    @Schema(description = "景点类型：1自然景观 2人文景观 3娱乐体验 4民俗体验")
    private ScenicTypeEnum type;

    @Schema(description = "是否提供住宿：0否 1是")
    private Integer hasAccommodation;

    @Schema(description = "住宿详情")
    private String accommodationInfo;

    @Schema(description = "门票单价（元）；新增景点时同步创建门票商品，不传默认 0（免费票）")
    private BigDecimal ticketPrice;

    @Schema(description = "住宿核销单价（元）；hasAccommodation=1 时可选，不传则不创建住宿商品")
    private BigDecimal stayPrice;
}
