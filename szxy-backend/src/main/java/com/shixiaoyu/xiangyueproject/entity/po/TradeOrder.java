package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.FulfillmentTypeEnum;
import com.shixiaoyu.xiangyueproject.enums.OrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单主表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("trade_order")
@Schema(title = "交易订单")
public class TradeOrder {
    @TableId(type = IdType.AUTO)
    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "对外订单号（唯一）")
    private String orderNo;

    @Schema(description = "买家 user.id")
    private Long userId;

    @Schema(description = "订单状态")
    private OrderStatusEnum status;

    @Schema(description = "整单履约类型：1虚拟 2实物")
    private FulfillmentTypeEnum fulfillmentType;

    @Schema(description = "收货地址 user_address.id（虚拟单可空）")
    private Long addressId;

    @Schema(description = "下单时地址快照（JSON）")
    private String addressSnapshot;

    @Schema(description = "订单总额（元）")
    private BigDecimal totalAmount;

    @Schema(description = "实付金额（元）")
    private BigDecimal payAmount;

    @Schema(description = "买家备注")
    private String remark;

    @Schema(description = "申请退款前的状态（驳回时恢复用）")
    private OrderStatusEnum statusBeforeRefund;

    @Schema(description = "买家退款申请原因")
    private String refundReason;

    @Schema(description = "申请退款时间")
    private LocalDateTime refundApplyTime;

    @Schema(description = "支付成功时间")
    private LocalDateTime payTime;

    @Schema(description = "农户发货时间")
    private LocalDateTime shipTime;

    @Schema(description = "送达时间")
    private LocalDateTime deliverTime;

    @Schema(description = "完结时间（已使用/已签收）")
    private LocalDateTime finishTime;

    @Schema(description = "用户软删：0否 1是")
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "下单/创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
