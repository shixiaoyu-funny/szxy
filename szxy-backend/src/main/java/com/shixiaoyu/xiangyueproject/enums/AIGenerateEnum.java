package com.shixiaoyu.xiangyueproject.enums;

public enum AIGenerateEnum {
    AI_RECOMMEND(1, "AI推荐"),
    AI_INTELLIGENCE(2, "AI智能问答"),
    AI_MULTI(3, "AI多模态识别");

    private final Integer code;
    private final String desc;
    AIGenerateEnum(Integer code, String desc) {
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
