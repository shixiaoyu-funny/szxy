package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.ProductTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 可售商品简要 VO（景点详情挂商品列表）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "可售商品")
public class ShopProductVO {
    private Long id;
    private Long scenicId;
    private String name;
    private String intro;
    private String image;
    private BigDecimal price;
    private Integer stock;
    private ProductTypeEnum type;
    private Integer status;
}
