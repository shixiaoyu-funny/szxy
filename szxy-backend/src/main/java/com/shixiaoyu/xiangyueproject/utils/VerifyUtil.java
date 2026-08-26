package com.shixiaoyu.xiangyueproject.utils;

import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerifyUtil {
    private final FlowUtil flowUtil;
    public boolean verifyPhoneLimit(String ip) {
        String key = RedisConstants.VERIFY_PHONE_LIMIT_PREFIX + ip;
        return flowUtil.limitOnceCheck(key, 60);
    }
    public boolean verifyEmailLimit(String ip) {
        String key = RedisConstants.VERIFY_EMAIL_LIMIT_PREFIX + ip;
        return flowUtil.limitOnceCheck(key, 60);
    }
}
