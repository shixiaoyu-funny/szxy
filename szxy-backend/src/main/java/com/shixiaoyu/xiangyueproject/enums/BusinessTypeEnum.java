package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 农户经营类型（farm_user.business_type）——仅表示"经营什么"，不再兼当角色
 * 1 民宿经营者  2 农产品销售者  3 文旅服务者
 */
public enum BusinessTypeEnum {
    HOMESTAY(1, "民宿经营者"),
    AGRICULTURE(2, "农产品销售者"),
    CULTURAL_TOURISM(3, "文旅服务者");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    BusinessTypeEnum(Integer code, String desc) {
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
