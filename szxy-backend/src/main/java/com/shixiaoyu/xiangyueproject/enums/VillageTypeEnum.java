package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 村落特色类型（village_base.type，方案 B：C 端可感知的核心卖点）
 * 1 古建聚落型  2 非遗民俗型  3 山水生态型  4 农业观光型  5 近郊休闲型
 * 6 康养度假型  7 红色研学型  8 滨水渔乡型  9 民族村寨型  10 综合文旅型
 */
public enum VillageTypeEnum {
    ANCIENT_ARCHITECTURE(1, "古建聚落型"),
    INTANGIBLE_HERITAGE(2, "非遗民俗型"),
    LANDSCAPE_ECOLOGY(3, "山水生态型"),
    AGRICULTURAL_TOURISM(4, "农业观光型"),
    SUBURBAN_LEISURE(5, "近郊休闲型"),
    HEALTH_RESORT(6, "康养度假型"),
    RED_STUDY(7, "红色研学型"),
    WATERSIDE_FISHING(8, "滨水渔乡型"),
    ETHNIC_VILLAGE(9, "民族村寨型"),
    COMPREHENSIVE_TOURISM(10, "综合文旅型");

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
