package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改待支付订单（仅地址 / 备注）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "修改待支付订单")
public class OrderAlterDTO {
    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "收货地址ID（实物可改；虚拟可空）")
    private Long addressId;

    @Schema(description = "买家备注")
    private String remark;
}
