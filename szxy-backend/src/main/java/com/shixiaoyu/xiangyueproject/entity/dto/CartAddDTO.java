package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 加入购物车
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "加入购物车")
public class CartAddDTO {
    @Schema(description = "商品 shop_product.id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @Schema(description = "数量，默认 1")
    private Integer quantity;
}
