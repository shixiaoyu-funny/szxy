package com.shixiaoyu.xiangyueproject.enums;

public enum ScenicTypeEnum {
    NATURAL_LANDSCAPE(1, "自然景观"),
    HUMANISTIC_LANDSCAPE(2, "人文景观"),
    ENTERTAINMENT_EXPERIENCE(3, "娱乐体验"),
    FOLK_EXPERIENCE(4, "民宿体验"),
    OTHER(5, "其他");
    private final Integer code;
    private final String desc;
    ScenicTypeEnum(Integer code, String desc) {
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
