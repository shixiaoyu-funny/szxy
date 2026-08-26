package com.shixiaoyu.xiangyueproject.entity.dto;

import com.shixiaoyu.xiangyueproject.enums.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游客申请/改提成为农户
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "农户申请DTO")
public class FarmerAccessDTO {

    @Schema(description = "身份证号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idCard;

    @Schema(description = "所属农村ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long villageId;

    @Schema(description = "经营类型：1民宿 2农产品 3文旅", requiredMode = Schema.RequiredMode.REQUIRED)
    private BusinessTypeEnum businessType;
}
