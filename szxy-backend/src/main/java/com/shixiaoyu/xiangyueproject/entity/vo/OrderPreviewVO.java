package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.FulfillmentTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 确认订单预览
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "订单预览")
public class OrderPreviewVO {
    @Schema(description = "当前余额")
    private BigDecimal balance;

    @Schema(description = "应付总额")
    private BigDecimal totalAmount;

    @Schema(description = "余额缺口（total-balance，不足为正则需充值）")
    private BigDecimal shortage;

    @Schema(description = "整单履约类型")
    private FulfillmentTypeEnum fulfillmentType;

    @Schema(description = "是否需要收货地址（实物为 true）")
    private Boolean needAddress;

    @Schema(description = "选用的地址（若传了 addressId）")
    private UserAddressVO address;

    @Schema(description = "预览明细")
    private List<Item> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
        private Integer stock;
        private FulfillmentTypeEnum fulfillmentType;
    }
}
