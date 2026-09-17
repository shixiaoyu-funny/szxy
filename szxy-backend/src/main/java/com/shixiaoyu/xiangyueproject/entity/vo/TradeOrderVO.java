package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.FulfillmentTypeEnum;
import com.shixiaoyu.xiangyueproject.enums.OrderStatusEnum;
import com.shixiaoyu.xiangyueproject.enums.VerifyStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表/详情 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "交易订单")
public class TradeOrderVO {
    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "对外订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private OrderStatusEnum status;

    @Schema(description = "履约类型：1虚拟 2实物")
    private FulfillmentTypeEnum fulfillmentType;

    @Schema(description = "收货地址ID")
    private Long addressId;

    @Schema(description = "地址快照")
    private String addressSnapshot;

    @Schema(description = "订单总额")
    private BigDecimal totalAmount;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "买家备注")
    private String remark;

    @Schema(description = "退款原因")
    private String refundReason;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "发货时间")
    private LocalDateTime shipTime;

    @Schema(description = "送达时间")
    private LocalDateTime deliverTime;

    @Schema(description = "完结时间")
    private LocalDateTime finishTime;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "订单明细")
    private List<OrderItemVO> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(title = "订单明细")
    public static class OrderItemVO {
        @Schema(description = "明细ID")
        private Long id;

        @Schema(description = "商品ID")
        private Long productId;

        @Schema(description = "景点ID")
        private Long scenicId;

        @Schema(description = "卖家用户ID")
        private Long sellerUserId;

        @Schema(description = "商品名称快照")
        private String itemName;

        @Schema(description = "商品封面快照")
        private String itemImage;

        @Schema(description = "单价快照")
        private BigDecimal unitPrice;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "小计")
        private BigDecimal subtotal;

        @Schema(description = "履约类型")
        private FulfillmentTypeEnum fulfillmentType;

        @Schema(description = "核销码")
        private String verifyCode;

        @Schema(description = "核销状态")
        private VerifyStatusEnum verifyStatus;

        @Schema(description = "核销时间")
        private LocalDateTime verifyTime;
    }
}
