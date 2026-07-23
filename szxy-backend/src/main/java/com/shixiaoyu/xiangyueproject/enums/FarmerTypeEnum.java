package com.shixiaoyu.xiangyueproject.enums;

public enum FarmerTypeEnum {
    FARMER(1, "农户"),
    VILLAGE_MANAGER(2, "村长");
    private final Integer code;
    private final String desc;
    FarmerTypeEnum(Integer code, String desc) {
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
