package com.shixiaoyu.xiangyueproject.util;

import cn.hutool.crypto.SecureUtil;
import com.shixiaoyu.xiangyueproject.entity.dto.AIDTO;

import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.AI_RECOMMEND_PREFIX;

public class CacheUtils {
    public static String getAiCacheKey(AIDTO aidto) {
        // 拼接核心参数字符串
        String coreParams = String.format("%s-%s-%d-%d-%d-%s",
                aidto.getProvince() == null ? "" : aidto.getProvince(),
                aidto.getCity() == null ? "" : aidto.getCity(),
                aidto.getTripDays() == null ? 0 : aidto.getTripDays(),
                aidto.getTripPeople() == null ? 0 : aidto.getTripPeople(),
                aidto.getTripPrefer() == null ? 0 : aidto.getTripPrefer(),
                aidto.getContent() == null ? "" : aidto.getContent()
        );
        // MD5加密成短Key
        return SecureUtil.md5(coreParams);
    }

}
