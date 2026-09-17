package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 订单状态（trade_order.status）
 * 1待支付 2已取消 3待使用 4待发货 5待收货 6待签收 7已签收 8已使用 9退款中 10已退款
 */
public enum OrderStatusEnum {
    UNPAID(1, "待支付"),
    CANCELLED(2, "已取消"),
    TO_USE(3, "待使用"),
    TO_SHIP(4, "待发货"),
    TO_RECEIVE(5, "待收货"),
    TO_SIGN(6, "待签收"),
    SIGNED(7, "已签收"),
    USED(8, "已使用"),
    REFUNDING(9, "退款中"),
    REFUNDED(10, "已退款");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("非法订单状态: " + code);
    }
}
