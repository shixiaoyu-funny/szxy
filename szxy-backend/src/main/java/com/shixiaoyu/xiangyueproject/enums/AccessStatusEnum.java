package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 农户准入申请状态（farmer_access.status）
 * 0 待审  1 已通过  2 已拒绝
 */
public enum AccessStatusEnum {
    PENDING(0, "待审"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    AccessStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 按数字 code 解析（查询参数 status=0/1/2） */
    public static AccessStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AccessStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("非法申请状态: " + code);
    }
}
