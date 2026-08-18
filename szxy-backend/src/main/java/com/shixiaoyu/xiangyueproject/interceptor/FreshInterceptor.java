package com.shixiaoyu.xiangyueproject.interceptor;

import com.shixiaoyu.xiangyueproject.constants.RedisConstants;
import com.shixiaoyu.xiangyueproject.entity.dto.UserDTO;
import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import com.shixiaoyu.xiangyueproject.util.PVUVUtils;
import com.shixiaoyu.xiangyueproject.util.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 流量统计拦截器：刷新 token 有效期 + 记录 PV/UV（排除管理员）
 */
@Component
@Slf4j
public class FreshInterceptor implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private PVUVUtils pvuvUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("authorization");
        if (token != null && !token.isBlank()) {
            stringRedisTemplate.expire(RedisConstants.LOGIN_TOKEN_PREFIX + token,
                    RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
        }
        UserDTO user = UserHolder.getUser();
        if (user != null && user.getRole() != RoleEnum.ADMIN) {
            pvuvUtils.addUV(LocalDate.now(), token);
            pvuvUtils.incrPV(LocalDate.now());
        }
        return true;
    }
}
