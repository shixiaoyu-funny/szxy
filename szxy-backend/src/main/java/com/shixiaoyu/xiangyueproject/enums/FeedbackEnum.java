package com.shixiaoyu.xiangyueproject.enums;

public enum FeedbackEnum {
    NO_FEEDBACK(0, "无反馈"),
    DISSATISFIED(1, "不满意"),
    AVERAGE(2, "一般"),
    SATISFIED(3, "满意");

    private final Integer score;
    private final String desc;

    FeedbackEnum(Integer score, String desc) {
        this.score = score;
        this.desc = desc;
    }

    public Integer getScore() {
        return score;
    }

    public String getDesc() {
        return desc;
    }
}
