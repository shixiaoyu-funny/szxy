package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.FulfillmentTypeEnum;
import com.shixiaoyu.xiangyueproject.enums.VerifyStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细（商品快照 + 虚拟核销码）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("order_item")
@Schema(title = "订单明细")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    @Schema(description = "订单明细ID")
    private Long id;

    @Schema(description = "所属订单 trade_order.id")
    private Long orderId;

    @Schema(description = "来源商品 shop_product.id")
    private Long productId;

    @Schema(description = "下单时关联景点快照（可空）")
    private Long scenicId;

    @Schema(description = "卖家 user.id")
    private Long sellerUserId;

    @Schema(description = "商品名称快照")
    private String itemName;

    @Schema(description = "商品封面快照")
    private String itemImage;

    @Schema(description = "下单单价快照（元）")
    private BigDecimal unitPrice;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "小计（元）")
    private BigDecimal subtotal;

    @Schema(description = "履约类型快照：1虚拟 2实物")
    private FulfillmentTypeEnum fulfillmentType;

    @Schema(description = "核销码（仅虚拟；支付成功后生成）")
    private String verifyCode;

    @Schema(description = "核销状态：0未核销 1已核销")
    private VerifyStatusEnum verifyStatus;

    @Schema(description = "核销时间")
    private LocalDateTime verifyTime;
}
