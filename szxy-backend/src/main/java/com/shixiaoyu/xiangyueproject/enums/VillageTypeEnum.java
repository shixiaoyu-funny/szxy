package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 村落特色类型（village_base.type）
 * 1 古村落  2 生态村  3 民俗村  4 文旅村
 */
public enum VillageTypeEnum {
    ANCIENT(1, "古村落"),
    ECOLOGICAL(2, "生态村"),
    FOLK(3, "民俗村"),
    CULTURAL_TOURISM(4, "文旅村");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    VillageTypeEnum(Integer code, String desc) {
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
