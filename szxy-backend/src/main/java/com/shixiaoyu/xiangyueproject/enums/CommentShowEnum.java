package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 评论展示状态（user_comment.is_show）
 * 0 隐藏  1 展示
 */
public enum CommentShowEnum {
    HIDE(0, "隐藏"),
    SHOW(1, "展示");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    CommentShowEnum(Integer code, String desc) {
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
