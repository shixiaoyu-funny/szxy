package com.shixiaoyu.xiangyueproject.enums;

public enum VillageTypeEnum {
    ANCIENT_VILLAGE(1, "古村古镇型"),
    FOLK_CUSTOM(2, "民俗风情型"),
    RED_TOURISM(3, "红色旅游型"),
    ECOLOGICAL(4, "自然生态型"),
    LEISURE_RESORT(5, "休闲度假型"),
    ART_CULTURE(6, "艺术文创型"),
    STUDY_EDUCATION(7, "研学教育型"),
    INDUSTRY_INTEGRATION(8, "产业融合型"),
    DIGITAL_SMART(9, "数字智慧型");

    private final int code;
    private final String desc;

    VillageTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public int getCode() {
        return code;
    }
    public String getDesc() {
        return desc;
    }
}
