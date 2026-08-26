package com.shixiaoyu.xiangyueproject.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统配置 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "系统配置VO")
public class SystemConfigVO {

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;
}
