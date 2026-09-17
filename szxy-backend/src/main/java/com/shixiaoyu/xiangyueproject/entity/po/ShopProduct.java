package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.ProductTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可售商品（实体 / 门票核销 / 住宿核销）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("shop_product")
@Schema(title = "可售商品")
public class ShopProduct {
    @TableId(type = IdType.AUTO)
    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "发布/售卖方 user.id")
    private Long userId;

    @Schema(description = "所属农村 village_base.id")
    private Long villageId;

    @Schema(description = "关联景点 village_scenic.id（门票/住宿通常有值；实体可空）")
    private Long scenicId;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "商品介绍")
    private String intro;

    @Schema(description = "封面图URL")
    private String image;

    @Schema(description = "单价（元）")
    private BigDecimal price;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "商品类型：1实体 2门票核销 3住宿核销")
    private ProductTypeEnum type;

    @Schema(description = "上架状态：1上架 0下架")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
