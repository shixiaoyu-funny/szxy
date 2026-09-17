package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 可售商品类型（shop_product.type）
 * 1 实体商品  2 门票核销  3 住宿核销
 * <p>订单履约：1 → 实物物流；2/3 → 虚拟核销
 */
public enum ProductTypeEnum {
    PHYSICAL(1, "实体商品"),
    TICKET(2, "门票核销"),
    STAY(3, "住宿核销");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    ProductTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 是否走虚拟核销履约 */
    public boolean isVirtual() {
        return this == TICKET || this == STAY;
    }
}
