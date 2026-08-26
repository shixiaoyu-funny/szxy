package com.shixiaoyu.xiangyueproject.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 省市区经纬度查询结果（对应 GET /pos/getPos）。
 * 缓存未命中时 longitude / latitude 均为 null，由前端再调高德 API。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "位置VO")
public class PositionVO {

    @Schema(description = "省名")
    private String province;

    @Schema(description = "市名")
    private String city;

    @Schema(description = "区/县名")
    private String county;

    @Schema(description = "经度；缓存未命中时为 null")
    private String longitude;

    @Schema(description = "纬度；缓存未命中时为 null")
    private String latitude;
}
