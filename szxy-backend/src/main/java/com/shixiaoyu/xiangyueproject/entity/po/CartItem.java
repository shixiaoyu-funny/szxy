package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.FulfillmentTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 购物车明细
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("cart_item")
@Schema(title = "购物车明细")
public class CartItem {
    @TableId(type = IdType.AUTO)
    @Schema(description = "购物车项ID")
    private Long id;

    @Schema(description = "买家 user.id")
    private Long userId;

    @Schema(description = "商品 shop_product.id")
    private Long productId;

    @Schema(description = "履约类型冗余：1虚拟 2实物（结算时校验不可混单）")
    private FulfillmentTypeEnum fulfillmentType;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "是否勾选结算：1是 0否")
    private Integer selected;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "加入时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
