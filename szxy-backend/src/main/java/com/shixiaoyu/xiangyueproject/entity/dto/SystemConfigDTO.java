package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统配置新增/修改
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "系统配置DTO")
public class SystemConfigDTO {

    @NotBlank(message = "配置键不能为空")
    @Size(max = 32, message = "配置键最长32字符")
    @Schema(description = "配置键", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configKey;

    @NotBlank(message = "配置值不能为空")
    @Size(max = 256, message = "配置值最长256字符")
    @Schema(description = "配置值", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configValue;
}
