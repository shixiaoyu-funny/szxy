package com.shixiaoyu.xiangyueproject.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.entity.po.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Token 工具：UUID 无状态会话（Redis + UUID，对齐 robot-singleton）
 * 生成 token 并以 UserDTO JSON 存入 Redis，拦截器凭 access_token 头校验并续期
 */
@Component
@RequiredArgsConstructor
public class TokenUtil {

    private final StringRedisTemplate stringRedisTemplate;

    /** 生成 token 并持久化用户会话 */
    public String issue(User user) {
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        String token = IdUtil.simpleUUID();
        stringRedisTemplate.opsForValue().set(
                RedisConstants.LOGIN_TOKEN_PREFIX + token,
                JSONUtil.toJsonStr(userDTO),
                RedisConstants.TOKEN_EXPIRE_TIME,
                TimeUnit.MINUTES);
        return token;
    }
}
