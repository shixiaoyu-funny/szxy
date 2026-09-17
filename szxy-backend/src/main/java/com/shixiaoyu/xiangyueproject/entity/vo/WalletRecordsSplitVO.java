package com.shixiaoyu.xiangyueproject.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 钱包流水双分页（充值 + 消费）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "钱包流水双分页")
public class WalletRecordsSplitVO {
    @Schema(description = "充值流水分页（type=1）")
    private PageResultVO<WalletRecordVO> recharge;

    @Schema(description = "消费流水分页（type=2 支付 + type=3 退款）")
    private PageResultVO<WalletRecordVO> consume;
}
