package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户上报当前 IP 定位（省市区）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "位置上报DTO")
public class PosReportDTO {

    @Schema(description = "省名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String province;

    @Schema(description = "市名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @Schema(description = "区/县名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String county;
}
