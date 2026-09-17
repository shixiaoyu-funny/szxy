package com.shixiaoyu.xiangyueproject.entity.dto;

import com.shixiaoyu.xiangyueproject.enums.RefundHandleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 卖家处理退款
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "处理退款")
public class OrderRefundHandleDTO {
    @Schema(description = "1同意 2驳回", requiredMode = Schema.RequiredMode.REQUIRED)
    private RefundHandleEnum handle;

    @Schema(description = "驳回原因（可选）")
    private String rejectReason;
}
