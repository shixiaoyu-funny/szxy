package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 景点类型（village_scenic.type）
 * 1 自然景观  2 人文景观  3 娱乐体验  4 民俗体验
 */
public enum ScenicTypeEnum {
    NATURAL(1, "自然景观"),
    HUMANISTIC(2, "人文景观"),
    ENTERTAINMENT(3, "娱乐体验"),
    FOLK(4, "民俗体验");

    @EnumValue
    @JsonValue
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
