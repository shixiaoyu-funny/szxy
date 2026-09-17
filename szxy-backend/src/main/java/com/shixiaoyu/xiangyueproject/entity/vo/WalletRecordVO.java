package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.WalletRecordTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "钱包流水")
public class WalletRecordVO {
    @Schema(description = "流水ID")
    private Long id;

    @Schema(description = "流水类型：1充值 2支付扣款 3退款入账")
    private WalletRecordTypeEnum type;

    @Schema(description = "变动金额（元）")
    private BigDecimal amount;

    @Schema(description = "变动后余额（元）")
    private BigDecimal balanceAfter;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "流水时间")
    private LocalDateTime createTime;
}
