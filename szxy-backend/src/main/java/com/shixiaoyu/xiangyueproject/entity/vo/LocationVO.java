package com.shixiaoyu.xiangyueproject.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "位置信息VO", description = "存储用户位置信息")
public class LocationVO {
    /**
     * 省份
     */
    @Schema(description = "省份（如：浙江省）")
    private String province;

    /**
     * 城市
     */
    @Schema(description = "城市（如：杭州市）")
    private String city;
}
