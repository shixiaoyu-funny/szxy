package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下单 / 预览共用入参
 * <p>
 * 二选一：
 * 1. fromCart=true → 使用当前用户购物车中已勾选商品
 * 2. productId + quantity → 立即购买单品
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "下单/预览入参")
public class OrderSubmitDTO {
    @Schema(description = "是否从购物车勾选项结算")
    private Boolean fromCart;

    @Schema(description = "立即购买：商品ID")
    private Long productId;

    @Schema(description = "立即购买：数量，默认1")
    private Integer quantity;

    @Schema(description = "收货地址ID（实物必填，虚拟可空）")
    private Long addressId;

    @Schema(description = "买家备注")
    private String remark;
}
