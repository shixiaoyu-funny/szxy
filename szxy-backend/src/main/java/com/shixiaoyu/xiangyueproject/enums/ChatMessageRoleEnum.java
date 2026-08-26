package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * AI 会话消息角色（chat_message.role）
 * 1 用户  2 AI（禾小智）
 */
public enum ChatMessageRoleEnum {
    USER(1, "用户"),
    ASSISTANT(2, "AI");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    ChatMessageRoleEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ChatMessageRoleEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ChatMessageRoleEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("非法消息角色: " + code);
    }
}
