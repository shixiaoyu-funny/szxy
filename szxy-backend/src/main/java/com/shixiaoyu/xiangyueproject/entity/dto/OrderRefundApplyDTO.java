package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 买家申请退款
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "申请退款")
public class OrderRefundApplyDTO {
    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}
