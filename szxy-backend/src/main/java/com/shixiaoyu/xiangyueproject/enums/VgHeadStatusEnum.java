package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 村长准入申请状态（vghead_access.status）
 * 0 待审  1 村长已审  2 管理员已审  3 已拒绝
 */
public enum VgHeadStatusEnum {
    PENDING(0, "待审"),
    CHIEF_APPROVED(1, "村长已审批"),
    ADMIN_APPROVED(2, "管理员已审批"),
    REJECTED(3, "已拒绝");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    VgHeadStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static VgHeadStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (VgHeadStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("非法村长申请状态: " + code);
    }
}
