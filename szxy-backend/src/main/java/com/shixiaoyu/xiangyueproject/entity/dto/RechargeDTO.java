package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 模拟充值
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "充值")
public class RechargeDTO {
    @Schema(description = "充值金额（元）", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;
}
