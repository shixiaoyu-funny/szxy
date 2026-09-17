package com.shixiaoyu.xiangyueproject.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 钱包流水类型（wallet_record.type）
 * 1充值 2支付扣款 3退款入账
 */
public enum WalletRecordTypeEnum {
    RECHARGE(1, "充值"),
    PAY(2, "支付扣款"),
    REFUND(3, "退款入账");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String desc;

    WalletRecordTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static WalletRecordTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WalletRecordTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("非法流水类型: " + code);
    }
}
