package com.shixiaoyu.xiangyueproject.enums;

public enum UserTypeEnum {
    ADMIN(1, "管理端"),
    USER(2, "用户端"),
    FARMER(3, "农户端");
    private final Integer code;
    private final String desc;
    UserTypeEnum(Integer code, String desc) {
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
