package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 订单/购物车履约方式（由商品 ProductTypeEnum 推导）
 * 1 虚拟核销  2 实物物流
 */
public enum FulfillmentTypeEnum {
    VIRTUAL(1, "虚拟履约"),
    PHYSICAL(2, "实物履约");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    FulfillmentTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static FulfillmentTypeEnum fromProductType(ProductTypeEnum productType) {
        if (productType == null) {
            return null;
        }
        return productType.isVirtual() ? VIRTUAL : PHYSICAL;
    }
}
