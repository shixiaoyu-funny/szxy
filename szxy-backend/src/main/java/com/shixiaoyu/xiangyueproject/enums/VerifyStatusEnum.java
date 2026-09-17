package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 核销状态（order_item.verify_status，仅虚拟履约明细有意义）
 * 0未核销 1已核销
 */
public enum VerifyStatusEnum {
    UNVERIFIED(0, "未核销"),
    VERIFIED(1, "已核销");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    VerifyStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static VerifyStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (VerifyStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("非法核销状态: " + code);
    }
}
