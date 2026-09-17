package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.WalletRecordTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水（充值 / 支付 / 退款）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wallet_record")
@Schema(title = "钱包流水")
public class WalletRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "流水ID")
    private Long id;

    @Schema(description = "用户 user.id")
    private Long userId;

    @Schema(description = "流水类型：1充值 2支付扣款 3退款入账")
    private WalletRecordTypeEnum type;

    @Schema(description = "变动金额（元，正数；方向由 type 区分）")
    private BigDecimal amount;

    @Schema(description = "变动后账户余额（元）")
    private BigDecimal balanceAfter;

    @Schema(description = "关联订单 trade_order.id（充值可空）")
    private Long orderId;

    @Schema(description = "备注说明")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "流水时间")
    private LocalDateTime createTime;
}
