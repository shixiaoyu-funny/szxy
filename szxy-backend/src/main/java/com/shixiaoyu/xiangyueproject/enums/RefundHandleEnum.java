package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 农户处理退款申请（接口入参，不落库）
 * 1同意 2驳回
 */
public enum RefundHandleEnum {
    APPROVE(1, "同意退款"),
    REJECT(2, "驳回退款");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    RefundHandleEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
