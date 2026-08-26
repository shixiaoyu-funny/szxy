package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存省市区经纬度到 Redis 的请求体（对应 POST /pos/savePos）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "位置保存DTO")
public class PositionDTO {

    @Schema(description = "省名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String province;

    @Schema(description = "市名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @Schema(description = "区/县名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String county;

    @Schema(description = "经度（高德 GCJ-02）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String longitude;

    @Schema(description = "纬度（高德 GCJ-02）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String latitude;
}
