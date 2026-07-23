package com.shixiaoyu.xiangyueproject.enums;

public enum UserStatusEnum {
    NORMAL(1, "可用"),
    LOCKED(2, "禁用");
    private final Integer code;
    private final String desc;
    UserStatusEnum(Integer code, String desc) {
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
