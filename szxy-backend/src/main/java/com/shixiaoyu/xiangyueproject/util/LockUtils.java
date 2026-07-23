package com.shixiaoyu.xiangyueproject.util;

import cn.hutool.core.util.BooleanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.shixiaoyu.xiangyueproject.constants.RedisConstants.LOCK_TTL;

@Component
@RequiredArgsConstructor
public class LockUtils {
    private final StringRedisTemplate stringRedisTemplate;
    public boolean tryLock(String key){
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(b);
    }
    public void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
