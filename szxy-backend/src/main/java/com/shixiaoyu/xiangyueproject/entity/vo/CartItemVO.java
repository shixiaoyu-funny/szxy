package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.FulfillmentTypeEnum;
import com.shixiaoyu.xiangyueproject.enums.ProductTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车列表项（含商品展示字段）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "购物车项")
public class CartItemVO {
    @Schema(description = "购物车项ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "履约类型：1虚拟 2实物")
    private FulfillmentTypeEnum fulfillmentType;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "是否勾选：1是 0否")
    private Integer selected;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品封面")
    private String productImage;

    @Schema(description = "商品单价（元）")
    private BigDecimal price;

    @Schema(description = "商品类型：1实体 2门票核销 3住宿核销")
    private ProductTypeEnum productType;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "上架状态：1上架 0下架")
    private Integer productStatus;

    @Schema(description = "卖家用户ID（农户/村长）")
    private Long sellerUserId;
}
