package com.shixiaoyu.xiangyueproject.enums;

public enum ReviewStatusEnum {
    UNDER_REVIEW(0, "待审核"),
    REVIEW_PASSED(1, "审核通过"),
    REVIEW_FAILED(2, "审核未通过");
    private final Integer code;
    private final String message;
    ReviewStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    public Integer getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
}
